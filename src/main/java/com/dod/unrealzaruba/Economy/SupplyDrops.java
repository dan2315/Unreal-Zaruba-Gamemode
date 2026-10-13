package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyDropPacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A supply crate appears at one of the map's spots every few minutes of battle. A player opens it by staying next to
 * it for a few seconds; then everyone on their team is paid and the crate's rolled loot can be taken. The crate can't
 * be broken or blown up, and disappears after a while.
 */
public final class SupplyDrops {
    private static final double OPEN_RANGE_SQR = 3.5 * 3.5;
    private static final RandomSource RANDOM = RandomSource.create();

    private static BlockPos activePos;
    private static boolean opened;
    private static int expiresAtTick;
    private static int nextDropTick = -1;
    private static int lastLocationIndex = -1;
    private static UUID opener;
    private static int openTicks;

    private SupplyDrops() {}

    public static void reset() {
        removeCrate();
        SupplyParadrop.clear();
        SupplyCases.reset();
        nextDropTick = -1;
        lastLocationIndex = -1;
    }

    static void tick(MinecraftServer server, EconomyConfig.SupplyDrops config, int battleTicks) {
        if (config == null || !config.enabled || config.locations == null || config.locations.isEmpty()) return;
        if (nextDropTick < 0) {
            nextDropTick = config.firstDropSeconds * 20;
        }
        if (activePos == null && battleTicks >= nextDropTick) {
            spawn(server, config);
            nextDropTick = battleTicks + Math.max(1, config.intervalSeconds) * 20;
            expiresAtTick = battleTicks + Math.max(1, config.lifetimeSeconds) * 20;
        }
        if (activePos == null) return;

        if (battleTicks >= expiresAtTick) {
            if (!opened) SideIncome.broadcast(server, Component.translatable("supplydrop.unrealzaruba.expired"));
            removeCrate();
            return;
        }
        tickOpening(server, config);
    }

    /** Drops a crate now (also used by /supplydrop now). Returns false if the map has no spots. */
    public static boolean spawn(MinecraftServer server, EconomyConfig.SupplyDrops config) {
        ServerLevel level = WorldManager.gameLevel;
        if (level == null || config.locations == null || config.locations.isEmpty()) return false;
        removeCrate();

        int index = RANDOM.nextInt(config.locations.size());
        if (config.locations.size() > 1 && index == lastLocationIndex) {
            index = (index + 1 + RANDOM.nextInt(config.locations.size() - 1)) % config.locations.size();
        }
        lastLocationIndex = index;
        BlockPos pos = config.locations.get(index);

        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            List<ItemStack> loot = rollLoot(server, config);
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < chest.getContainerSize(); i++) slots.add(i);
            java.util.Collections.shuffle(slots, new java.util.Random(RANDOM.nextLong()));
            for (int i = 0; i < loot.size() && i < slots.size(); i++) {
                chest.setItem(slots.get(i), loot.get(i));
            }
        }
        activePos = pos;
        opened = false;
        opener = null;
        openTicks = 0;
        syncAll(server);
        SideIncome.broadcast(server, Component.translatable("supplydrop.unrealzaruba.spawned",
                pos.getX(), pos.getZ(), Money.format(config.teamReward)));
        return true;
    }

    private static List<ItemStack> rollLoot(MinecraftServer server, EconomyConfig.SupplyDrops config) {
        return rollLoot(server, config.loot, config.lootRolls);
    }

    /** {@code rolls} weighted picks from {@code loot}; entries that don't parse are skipped. */
    static List<ItemStack> rollLoot(MinecraftServer server, List<EconomyConfig.Loot> table, int rolls) {
        List<ItemStack> stacks = new ArrayList<>();
        List<EconomyConfig.Loot> loot = table == null ? List.of() : table;
        int totalWeight = loot.stream().mapToInt(entry -> Math.max(0, entry.weight)).sum();
        if (totalWeight <= 0) return stacks;
        for (int roll = 0; roll < rolls; roll++) {
            int pick = RANDOM.nextInt(totalWeight);
            for (EconomyConfig.Loot entry : loot) {
                pick -= Math.max(0, entry.weight);
                if (pick < 0) {
                    ItemStack stack = ItemSpecs.parse(server.registryAccess(), entry.item, entry.count);
                    if (!stack.isEmpty()) stacks.add(stack);
                    break;
                }
            }
        }
        return stacks;
    }

    private static void tickOpening(MinecraftServer server, EconomyConfig.SupplyDrops config) {
        if (opened || opener == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(opener);
        if (!canOpen(player)) {
            if (player != null) player.displayClientMessage(Component.translatable("supplydrop.unrealzaruba.interrupted"), true);
            opener = null;
            openTicks = 0;
            return;
        }
        int required = Math.max(1, config.openSeconds) * 20;
        openTicks++;
        if (openTicks % 10 == 0 && openTicks < required) {
            int percent = openTicks * 100 / required;
            player.displayClientMessage(Component.translatable("supplydrop.unrealzaruba.opening", percent + "%"), true);
        }
        if (openTicks >= required) {
            open(server, player, config);
        }
    }

    private static void open(MinecraftServer server, ServerPlayer player, EconomyConfig.SupplyDrops config) {
        opened = true;
        opener = null;
        TeamContext team = teamOf(player);
        if (team != null) {
            for (ServerPlayer member : SideIncome.onlineMembers(server, team)) {
                Wallets.add(member, config.teamReward);
            }
            SideIncome.broadcast(server, Component.translatable("supplydrop.unrealzaruba.opened",
                    player.getDisplayName(), Money.format(config.teamReward), team.Color().getDisplayName()));
        }
        syncAll(server);
    }

    private static boolean canOpen(ServerPlayer player) {
        return player != null && activePos != null && player.isAlive()
                && player.level() == WorldManager.gameLevel
                && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR
                && teamOf(player) != null
                && player.distanceToSqr(activePos.getCenter()) <= OPEN_RANGE_SQR;
    }

    private static TeamContext teamOf(ServerPlayer player) {
        return PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext context ? context.Team() : null;
    }

    private static void removeCrate() {
        ServerLevel level = WorldManager.gameLevel;
        if (activePos != null && level != null) {
            if (level.getBlockEntity(activePos) instanceof ChestBlockEntity chest) {
                chest.clearContent(); // nothing spills out
            }
            level.setBlockAndUpdate(activePos, Blocks.AIR.defaultBlockState());
        }
        boolean hadCrate = activePos != null;
        activePos = null;
        opened = false;
        opener = null;
        openTicks = 0;
        if (hadCrate && WorldManager.server != null) syncAll(WorldManager.server);
    }

    // ---- events ----

    /** Right-clicking a closed crate starts opening it instead of showing its contents. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (activePos == null || opened || event.getLevel().isClientSide() || !event.getPos().equals(activePos)) return;
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || !canOpen(player)) return;
        if (opener != null && !opener.equals(player.getUUID())) {
            ServerPlayer current = player.server.getPlayerList().getPlayer(opener);
            if (canOpen(current)) {
                player.displayClientMessage(Component.translatable("supplydrop.unrealzaruba.busy"), true);
                return;
            }
        }
        if (!player.getUUID().equals(opener)) {
            opener = player.getUUID();
            openTicks = 0;
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (activePos != null && event.getPos().equals(activePos) && event.getLevel() == WorldManager.gameLevel) {
            event.setCanceled(true);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (activePos != null && event.getLevel() == WorldManager.gameLevel) {
            event.getAffectedBlocks().remove(activePos);
        }
    }

    // ---- clients (map waypoint) ----

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet());
    }

    private static void syncAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(packet());
    }

    private static ClientboundSupplyDropPacket packet() {
        // Only unopened crates are worth showing on the map
        return new ClientboundSupplyDropPacket(activePos != null && !opened ? activePos : null);
    }
}
