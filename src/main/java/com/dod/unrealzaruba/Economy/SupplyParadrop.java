package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;
import com.dod.unrealzaruba.ModBlocks.Parachute.ParachutePackBlockEntity;
import com.dod.unrealzaruba.SableIntegration.SableShips;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.utils.Timers.TimerManager;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A supply drop that comes down by parachute: a small wooden stand with a barrel of loot in the middle and a
 * {@link ModBlocks#PARACHUTE_PACK_BLOCK} on its roof, built in the sky above a spot and assembled into a Sable physics body. The pack is deployed from the
 * start, so the body falls at the configured terminal speed; the canopy retracts once it has landed.
 */
public final class SupplyParadrop {
    private static final int WIDTH = 3;
    /** Floor, posts and roof; the pack stands on top of that. */
    private static final int HEIGHT = 4;
    /** Lets the placed blocks settle (and the barrel's block entity exist) before they are assembled. */
    private static final int ASSEMBLE_DELAY_TICKS = 4;
    private static final int CEILING_MARGIN = 8;

    private static final List<UUID> ACTIVE = new ArrayList<>();
    /** Chunks kept loaded around the drops: a body in an unloaded chunk is stored away and stops falling. */
    private static final List<net.minecraft.world.level.ChunkPos> FORCED = new ArrayList<>();
    private static final int FORCE_RADIUS = 2;

    private SupplyParadrop() {}

    /** Starts a drop above {@code target} (a block on the ground). Returns false if it can't be built there. */
    public static boolean spawn(MinecraftServer server, EconomyConfig.SupplyDrops config, BlockPos target) {
        ServerLevel level = WorldManager.gameLevel;
        if (level == null) return false;
        EconomyConfig.Paradrop settings = config.paradrop;

        int top = level.getMaxBuildHeight() - CEILING_MARGIN - HEIGHT - 1;
        int y = Math.min(target.getY() + Math.max(10, settings.dropHeight), top);
        BlockPos origin = new BlockPos(target.getX() - 1, y, target.getZ() - 1);

        // The stand: floor, four corner posts and the barrel in the middle, roof, and the pack on the roof's centre
        BlockState floor = blockState(settings.floorBlock, Blocks.SPRUCE_PLANKS);
        BlockState roof = blockState(settings.roofBlock, Blocks.SPRUCE_PLANKS);
        BlockState post = blockState(settings.postBlock, Blocks.SPRUCE_FENCE);
        BlockState barrel = blockState(settings.barrelBlock, Blocks.BARREL);
        if (barrel.hasProperty(BlockStateProperties.FACING)) barrel = barrel.setValue(BlockStateProperties.FACING, Direction.UP);

        Map<BlockPos, BlockState> parts = new LinkedHashMap<>();
        for (int dx = 0; dx < WIDTH; dx++) {
            for (int dz = 0; dz < WIDTH; dz++) {
                parts.put(origin.offset(dx, 0, dz), floor);
                parts.put(origin.offset(dx, 2, dz), roof);
            }
        }
        for (int[] corner : new int[][]{{0, 0}, {2, 0}, {0, 2}, {2, 2}}) {
            parts.put(origin.offset(corner[0], 1, corner[1]), post);
        }
        BlockPos barrelPos = origin.offset(1, 1, 1);
        parts.put(barrelPos, barrel);
        BlockPos packPos = origin.offset(1, 3, 1);
        parts.put(packPos, ModBlocks.PARACHUTE_PACK_BLOCK.get().defaultBlockState());

        for (BlockPos pos : parts.keySet()) {
            if (!level.getBlockState(pos).isAir()) {
                UnrealZaruba.LOGGER.warn("[SupplyParadrop] Sky above {} is not free at {}", target, pos);
                return false;
            }
        }
        parts.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_ALL));

        fillBarrel(server, level, barrelPos, settings);
        if (level.getBlockEntity(packPos) instanceof ParachutePackBlockEntity pack) {
            pack.setTerminalSpeed(settings.terminalSpeed);
            pack.setDeployed(true); // travels with the block when the body is assembled
        }

        List<BlockPos> blocks = new ArrayList<>(parts.keySet());
        forceChunks(level, target);
        later(ASSEMBLE_DELAY_TICKS, () -> assemble(level, blocks, target));
        return true;
    }

    /** The supply case in the middle slot, plus the configured random loot in random free slots. */
    private static void fillBarrel(MinecraftServer server, ServerLevel level, BlockPos pos, EconomyConfig.Paradrop settings) {
        if (!(level.getBlockEntity(pos) instanceof Container barrel)) return;
        int size = barrel.getContainerSize();
        ItemStack cash = ItemSpecs.parse(server.registryAccess(), settings.cashItem, 1);
        int cashSlot = size / 2;
        if (!cash.isEmpty()) barrel.setItem(cashSlot, cash);

        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < size; i++) if (i != cashSlot || cash.isEmpty()) free.add(i);
        java.util.Collections.shuffle(free);
        List<ItemStack> loot = SupplyDrops.rollLoot(server, settings.loot, settings.lootRolls);
        for (int i = 0; i < loot.size() && i < free.size(); i++) barrel.setItem(free.get(i), loot.get(i));
        barrel.setChanged();
        UnrealZaruba.LOGGER.info("[SupplyParadrop] Barrel at {} holds {}{} random item stacks", pos,
                cash.isEmpty() ? "" : "the supply case and ", Math.min(loot.size(), free.size()));
    }

    private static void assemble(ServerLevel level, List<BlockPos> blocks, BlockPos target) {
        if (level != WorldManager.gameLevel) return; // the map was reloaded meanwhile
        ServerSubLevel subLevel = SableShips.assemble(level, blocks);
        if (subLevel == null) {
            UnrealZaruba.LOGGER.error("[SupplyParadrop] Could not assemble the drop above {}", target);
            for (BlockPos pos : blocks) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        subLevel.setName("supply_drop");
        ACTIVE.add(subLevel.getUniqueId());
        UnrealZaruba.LOGGER.info("[SupplyParadrop] Drop {} assembled above {}: {} blocks, mass {}", subLevel.getUniqueId(), target,
                blocks.size(), subLevel.getMassTracker().getMass());
        trace(level, subLevel.getUniqueId(), 0);
        SideIncome.broadcast(level.getServer(), Component.translatable("supplydrop.unrealzaruba.incoming",
                target.getX(), target.getZ()));
    }

    /** Runs after a number of server ticks (server.tell with a TickTask would run it right away). */
    private static void later(int ticks, Runnable task) {
        TimerManager.createTickTimer(ticks, task::run, null);
    }

    /** Logs the drop's height and speed once a second for its first half minute, to see it settle at the terminal speed. */
    private static void trace(ServerLevel level, UUID id, int seconds) {
        if (seconds > 30) return;
        if (level != WorldManager.gameLevel) {
            UnrealZaruba.LOGGER.info("[SupplyParadrop] t={}s: the game level was replaced", seconds);
            return;
        }
        ServerSubLevel subLevel = SableShips.get(level, id);
        if (subLevel == null) {
            UnrealZaruba.LOGGER.info("[SupplyParadrop] t={}s: the drop is gone", seconds);
            return;
        }
        UnrealZaruba.LOGGER.info("[SupplyParadrop] t={}s y={} speed={}", seconds,
                String.format(java.util.Locale.ROOT, "%.1f", subLevel.logicalPose().position().y()),
                String.format(java.util.Locale.ROOT, "%.2f", subLevel.latestLinearVelocity.length()));
        later(20, () -> trace(level, id, seconds + 1));
    }

    /** Removes every drop that is still around (end of a match, map change). */
    public static void clear() {
        ServerLevel level = WorldManager.gameLevel;
        if (level != null) {
            for (UUID id : ACTIVE) SableShips.remove(level, id);
            for (net.minecraft.world.level.ChunkPos chunk : FORCED) level.setChunkForced(chunk.x, chunk.z, false);
        }
        ACTIVE.clear();
        FORCED.clear();
    }

    private static void forceChunks(ServerLevel level, BlockPos target) {
        net.minecraft.world.level.ChunkPos center = new net.minecraft.world.level.ChunkPos(target);
        for (int dx = -FORCE_RADIUS; dx <= FORCE_RADIUS; dx++) {
            for (int dz = -FORCE_RADIUS; dz <= FORCE_RADIUS; dz++) {
                net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(center.x + dx, center.z + dz);
                if (!FORCED.contains(chunk)) {
                    level.setChunkForced(chunk.x, chunk.z, true);
                    FORCED.add(chunk);
                }
            }
        }
    }

    private static BlockState blockState(String id, Block fallback) {
        ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
        return (location == null ? Optional.<Block>empty() : BuiltInRegistries.BLOCK.getOptional(location))
                .orElse(fallback).defaultBlockState();
    }
}
