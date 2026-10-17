package com.dod.unrealzaruba.Vehicles;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.Economy.EconomyConfig;
import com.dod.unrealzaruba.Economy.EconomyData;
import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Economy.Wallets;
import com.dod.unrealzaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlockEntity;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenSchematicSpawnerPacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundSchematicSpawnResultPacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.SpawnSchematicPiecePacket;
import com.dod.unrealzaruba.Pricing.BlueprintScanner;
import com.dod.unrealzaruba.Pricing.Pricer;
import com.dod.unrealzaruba.Pricing.RulesService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Vehicle purchase blocks spawn shared schematics (toolgun blueprints). Using the block opens the schematics browser
 * on the client; the client downloads the chosen blueprint from the backend and sends it here in pieces. The server
 * checks the player, the block, money and the blueprint's size, then places it behind the block.
 */
public final class SchematicSpawner {
    private static final double MAX_DISTANCE_SQR = 8 * 8;
    private static final int MAX_BYTES = 1024 * 1024;

    private record Pending(BlockPos pos, long schematicId, String name, byte[] data, int[] received, int contribution) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private SchematicSpawner() {}

    public static void open(ServerPlayer player, VehiclePurchaseBlockEntity block) {
        EconomyConfig economy = EconomyData.current();
        if (!economy.schematicSpawns.enabled) {
            player.displayClientMessage(Component.translatable("schematics.unrealzaruba.spawn.disabled"), true);
            return;
        }
        // A purchase is going on here: its screen, not the vehicle list
        PurchaseSessions.Session going = PurchaseSessions.at(block.getBlockPos());
        if (going != null) {
            PurchaseSessions.sendCurrent(player, going);
            return;
        }
        RulesService.get(); // warms up the price list, so the first spawn does not have to wait for it
        PacketDistributor.sendToPlayer(player, new ClientboundOpenSchematicSpawnerPacket(block.getBlockPos(),
                economy.defaultVehiclePrice, (block.getCooldownTicks() + 19) / 20));
    }

    public static void onPiece(ServerPlayer player, SpawnSchematicPiecePacket piece) {
        UUID id = player.getUUID();
        if (piece.offset == 0) {
            if (piece.totalSize <= 0 || piece.totalSize > MAX_BYTES) {
                fail(player, Component.translatable("schematics.unrealzaruba.error.too_big"));
                return;
            }
            PENDING.put(id, new Pending(piece.pos, piece.schematicId, piece.name, new byte[piece.totalSize], new int[1], piece.contribution));
        }
        Pending pending = PENDING.get(id);
        if (pending == null || !pending.pos.equals(piece.pos) || pending.schematicId != piece.schematicId
                || piece.offset != pending.received[0] || piece.offset + piece.data.length > pending.data.length) {
            PENDING.remove(id);
            return; // out of order or from an older request
        }
        System.arraycopy(piece.data, 0, pending.data, piece.offset, piece.data.length);
        pending.received[0] += piece.data.length;
        if (pending.received[0] == pending.data.length) {
            PENDING.remove(id);
            spawn(player, pending.pos, pending.schematicId, pending.name, pending.data, pending.contribution);
        }
    }

    private static void spawn(ServerPlayer player, BlockPos pos, long schematicId, String name, byte[] data, int contribution) {
        EconomyConfig economy = EconomyData.current();
        EconomyConfig.SchematicSpawns limits = economy.schematicSpawns;
        ServerLevel level = player.serverLevel();
        if (!limits.enabled) {
            fail(player, Component.translatable("schematics.unrealzaruba.spawn.disabled"));
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof VehiclePurchaseBlockEntity block)
                || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) {
            fail(player, Component.translatable("schematics.unrealzaruba.spawn.too_far"));
            return;
        }
        if (!player.isAlive() || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            fail(player, Component.translatable("schematics.unrealzaruba.spawn.dead"));
            return;
        }
        if (block.getCooldownTicks() > 0) {
            fail(player, Component.translatable("schematics.unrealzaruba.spawn.cooldown", (block.getCooldownTicks() + 19) / 20));
            return;
        }
        Blueprints.Info info = Blueprints.read(name, data, level);
        if (info == null) {
            fail(player, Component.translatable("schematics.unrealzaruba.error.not_structure"));
            return;
        }
        // The price is what the blueprint is worth by the backend price list, worked out from the file itself (the same
        // reading the backend does, so it is the number shown in the browser), not a flat fee
        RulesService.Rules rules = RulesService.get();
        if (rules == null) {
            fail(player, Component.translatable("schematics.unrealzaruba.spawn.no_rules"));
            return;
        }
        int price;
        try {
            price = Money.charge(Pricer.price(BlueprintScanner.scan(data), rules.catalog()).total());
        } catch (IOException e) {
            fail(player, Component.translatable("schematics.unrealzaruba.error.not_structure"));
            return;
        }
        if (info.blocks() > limits.maxBlocks || info.maxSide() > limits.maxSize) {
            fail(player, Component.translatable("schematics.unrealzaruba.spawn.too_large",
                    info.blocks(), limits.maxBlocks, info.maxSide(), limits.maxSize));
            return;
        }

        // Behind the block (it faces the players using it), far enough that the vehicle clears it
        Direction facing = block.getBlockState().getValue(BlockStateProperties.FACING);
        Direction away = facing.getAxis().isHorizontal() ? facing.getOpposite() : Direction.NORTH;
        double distance = 0.5 + limits.gap + Math.max(info.width(), info.depth()) / 2.0;
        double centerX = pos.getX() + 0.5 + away.getStepX() * distance;
        double centerZ = pos.getZ() + 0.5 + away.getStepZ() * distance;
        double minimumBlockCenterY = pos.getY() + 1.5; // a block above the floor, so uneven ground doesn't clip it

        // The first payment starts a purchase others can add to; paying everything buys the vehicle right away
        int amount = Math.max(1, Math.min(contribution, price));
        Component refusal = PurchaseSessions.start(player, block, schematicId, name, data, price, centerX, centerZ,
                minimumBlockCenterY, amount);
        if (refusal != null) {
            fail(player, refusal);
            return;
        }
        UnrealZaruba.LOGGER.info("[SchematicSpawner] {} paid {} of {} for schematic {} ({}) at {}",
                player.getGameProfile().getName(), amount, price, schematicId, name, pos);
        if (amount < price) {
            PacketDistributor.sendToPlayer(player, new ClientboundSchematicSpawnResultPacket(true,
                    Component.translatable("schematics.unrealzaruba.purchase.started", name, Money.format(amount), Money.format(price))));
        }
    }

    private static void fail(ServerPlayer player, Component reason) {
        PacketDistributor.sendToPlayer(player, new ClientboundSchematicSpawnResultPacket(false, reason));
    }

    public static void forget(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }
}
