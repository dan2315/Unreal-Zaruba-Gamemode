package com.dod.unrealzaruba.Vehicles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.Economy.EconomyData;
import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Economy.Wallets;
import com.dod.unrealzaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlockEntity;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenPurchaseSessionPacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundSchematicSpawnResultPacket;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Buying a vehicle together. The first player to pay starts a purchase at a vehicle purchase block: the block is then
 * locked to that blueprint and anyone near it can add money. The money is held until the price is covered, and the
 * vehicle appears. The first contribution of each new player gives the purchase another {@link #WINDOW_TICKS} (more
 * from the same player does not); when no new player joins in time, everybody gets their money back.
 */
public final class PurchaseSessions {
    /** 15 seconds. */
    public static final int WINDOW_TICKS = 300;
    private static final double BROADCAST_RANGE = 128;
    private static final double CONTRIBUTE_RANGE_SQR = 8 * 8;

    /** One purchase in progress at a block. */
    public static final class Session {
        final BlockPos pos;
        final UUID initiator;
        final String initiatorName;
        final long schematicId;
        final String name;
        final byte[] data;
        final int price;
        final double centerX;
        final double centerZ;
        final double minimumBlockCenterY;
        final Map<UUID, Integer> contributions = new LinkedHashMap<>();
        final Map<UUID, String> names = new HashMap<>();
        int paid;
        long deadline;

        Session(BlockPos pos, ServerPlayer initiator, long schematicId, String name, byte[] data, int price,
                double centerX, double centerZ, double minimumBlockCenterY) {
            this.pos = pos;
            this.initiator = initiator.getUUID();
            this.initiatorName = initiator.getGameProfile().getName();
            this.schematicId = schematicId;
            this.name = name;
            this.data = data;
            this.price = price;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.minimumBlockCenterY = minimumBlockCenterY;
        }

        public int remaining() {
            return Math.max(0, price - paid);
        }
    }

    private static final Map<BlockPos, Session> SESSIONS = new HashMap<>();
    private static long now;

    private PurchaseSessions() {}

    public static Session at(BlockPos pos) {
        return SESSIONS.get(pos);
    }

    /** The purchase this player started, if one is still going: it holds them to it. */
    public static Session startedBy(UUID player) {
        for (Session session : SESSIONS.values()) {
            if (session.initiator.equals(player)) return session;
        }
        return null;
    }

    /** The smallest contribution: a hundredth of the price, or what is left when that is more. */
    public static int minimumContribution(int price, int remaining) {
        return Math.min(Math.max(1, remaining), Math.max(1, price / 100));
    }

    /**
     * Starts a purchase with the first contribution. Returns the reason in Russian when it can't, null when it started.
     * The caller has checked the blueprint, the price and the block already.
     */
    public static Component start(ServerPlayer player, VehiclePurchaseBlockEntity block, long schematicId, String name,
                                  byte[] data, int price, double centerX, double centerZ, double minimumBlockCenterY, int amount) {
        BlockPos pos = block.getBlockPos();
        if (SESSIONS.containsKey(pos)) {
            return Component.translatable("schematics.unrealzaruba.purchase.busy");
        }
        if (startedBy(player.getUUID()) != null) {
            return Component.translatable("schematics.unrealzaruba.purchase.already_buying");
        }
        if (amount < minimumContribution(price, price) || amount > price) {
            return Component.translatable("schematics.unrealzaruba.purchase.bad_amount");
        }
        if (!Wallets.trySpend(player, amount)) {
            return Component.translatable("schematics.unrealzaruba.spawn.no_money", Money.format(amount));
        }

        Session session = new Session(pos, player, schematicId, name, data, price, centerX, centerZ, minimumBlockCenterY);
        session.contributions.put(player.getUUID(), amount);
        session.names.put(player.getUUID(), player.getGameProfile().getName());
        session.paid = amount;
        session.deadline = now + WINDOW_TICKS;
        SESSIONS.put(pos, session);
        UnrealZaruba.LOGGER.info("[Purchase] {} started buying {} ({}) at {} for {}, paid {}", session.initiatorName, name, schematicId, pos, price, amount);

        if (session.paid >= session.price) {
            finish(session, player, (ServerLevel) player.level(), block);
        } else {
            broadcast(session, player.serverLevel(), true, false);
            PacketDistributor.sendToPlayer(player, new ClientboundOpenPurchaseSessionPacket(pos));
        }
        return null;
    }

    /** Adds money to the purchase at {@code pos}. Failures go back to the player as an action bar message. */
    public static void contribute(ServerPlayer player, BlockPos pos, int amount) {
        Session session = SESSIONS.get(pos);
        ServerLevel level = player.serverLevel();
        if (session == null) {
            deny(player, Component.translatable("schematics.unrealzaruba.purchase.ended"));
            return;
        }
        if (!player.isAlive() || player.distanceToSqr(pos.getCenter()) > CONTRIBUTE_RANGE_SQR) {
            deny(player, Component.translatable("schematics.unrealzaruba.spawn.too_far"));
            return;
        }
        int remaining = session.remaining();
        if (amount < minimumContribution(session.price, remaining) || amount > remaining) {
            deny(player, Component.translatable("schematics.unrealzaruba.purchase.bad_amount"));
            return;
        }
        if (!Wallets.trySpend(player, amount)) {
            deny(player, Component.translatable("schematics.unrealzaruba.spawn.no_money", Money.format(amount)));
            return;
        }
        boolean newcomer = !session.contributions.containsKey(player.getUUID());
        session.contributions.merge(player.getUUID(), amount, Integer::sum);
        session.names.put(player.getUUID(), player.getGameProfile().getName());
        session.paid += amount;
        if (newcomer) session.deadline = now + WINDOW_TICKS; // only a new player buys more time

        if (session.paid >= session.price && level.getBlockEntity(pos) instanceof VehiclePurchaseBlockEntity block) {
            finish(session, player, level, block);
        } else {
            broadcast(session, level, true, false);
        }
    }

    /** Called every server tick: purchases nobody added to in time are cancelled and refunded. */
    public static void tick(MinecraftServer server) {
        now++;
        if (SESSIONS.isEmpty()) return;
        for (Iterator<Session> it = SESSIONS.values().iterator(); it.hasNext(); ) {
            Session session = it.next();
            if (now < session.deadline) continue;
            it.remove();
            ServerLevel level = com.dod.unrealzaruba.WorldManager.WorldManager.gameLevel;
            refund(server, session);
            if (level != null) broadcast(session, level, false, false);
            UnrealZaruba.LOGGER.info("[Purchase] {} at {} ran out of time with {} of {}, refunded", session.name, session.pos, session.paid, session.price);
        }
    }

    /** New match or server stop: nothing carries over. */
    public static void reset() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // The ghosts and plates of purchases that are over are taken down on the clients too
            for (Session session : SESSIONS.values()) {
                var ended = new ClientboundPurchaseSessionPacket(session.pos, false, false, session.schematicId, session.name,
                        session.price, session.paid, 0, session.centerX, session.centerZ, session.minimumBlockCenterY,
                        session.initiatorName, new LinkedHashMap<>());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(player, ended);
            }
        }
        SESSIONS.clear();
    }

    private static void finish(Session session, ServerPlayer completer, ServerLevel level, VehiclePurchaseBlockEntity block) {
        SESSIONS.remove(session.pos);
        try {
            java.util.Set<UUID> before = TrackedVehicles.snapshot(level);
            UUID body = Blueprints.place(completer, level, session.name, session.data, session.centerX, session.centerZ, session.minimumBlockCenterY);
            TrackedVehicles.claim(before, session.name, teamOf(session.initiator, completer), body);
        } catch (IOException | RuntimeException e) {
            UnrealZaruba.LOGGER.warn("[Purchase] {} could not be placed at {}: {}", session.name, session.pos, e.getMessage());
            refund(level.getServer(), session);
            broadcast(session, level, false, false);
            PacketDistributor.sendToPlayer(completer, new ClientboundSchematicSpawnResultPacket(false,
                    Component.translatable("schematics.unrealzaruba.spawn.failed", String.valueOf(e.getMessage()))));
            return;
        }
        block.startCooldown(EconomyData.current().schematicSpawns.cooldownSeconds * 20);
        broadcast(session, level, false, true);
        var boughtBy = teamOf(session.initiator, completer);
        com.dod.unrealzaruba.Feed.Feed.event(level.getServer(), com.dod.unrealzaruba.Feed.FeedKind.VEHICLE, com.dod.unrealzaruba.Feed.Feed.colorOf(boughtBy), "feed.vehicle_bought",
                session.initiatorName, session.name, com.dod.unrealzaruba.Economy.Money.format(session.price));
        com.dod.unrealzaruba.SoundHandler.Voice.playSplit(level.getServer(), boughtBy, com.dod.unrealzaruba.SoundHandler.VoiceLine.VEHICLE_BOUGHT_ALLY, com.dod.unrealzaruba.SoundHandler.VoiceLine.VEHICLE_BOUGHT_ENEMY);
        Component done = Component.translatable("schematics.unrealzaruba.purchase.done", session.name);
        for (UUID id : session.contributions.keySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) PacketDistributor.sendToPlayer(player, new ClientboundSchematicSpawnResultPacket(true, done));
        }
        UnrealZaruba.LOGGER.info("[Purchase] {} bought at {} by {} players", session.name, session.pos, session.contributions.size());
    }

    /** The team of whoever started the purchase, or of the one who finished it if the starter is gone. */
    private static com.dod.unrealzaruba.Commands.Arguments.TeamColor teamOf(UUID initiator, ServerPlayer completer) {
        for (UUID id : new UUID[]{initiator, completer.getUUID()}) {
            if (com.dod.unrealzaruba.Player.PlayerContext.Get(id) instanceof com.dod.unrealzaruba.Player.TeamPlayerContext context
                    && context.Team() != null) {
                return context.Team().Color();
            }
        }
        return null;
    }

    private static void refund(MinecraftServer server, Session session) {
        Component message = Component.translatable("schematics.unrealzaruba.purchase.refunded", session.name);
        for (var entry : session.contributions.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                Wallets.add(player, entry.getValue());
                player.displayClientMessage(message, true);
            }
            // A player who left keeps nothing here: wallets belong to the match, not to the connection
        }
    }

    private static void deny(ServerPlayer player, Component reason) {
        player.displayClientMessage(reason, true);
    }

    /** Tells the players near the block (and everybody who paid) how the purchase stands. */
    private static void broadcast(Session session, ServerLevel level, boolean active, boolean completed) {
        Map<String, Integer> contributors = new LinkedHashMap<>();
        for (var entry : session.contributions.entrySet()) {
            contributors.put(session.names.getOrDefault(entry.getKey(), "?"), entry.getValue());
        }
        int ticksLeft = active ? (int) Math.max(0, session.deadline - now) : 0;
        var packet = new ClientboundPurchaseSessionPacket(session.pos, active, completed, session.schematicId, session.name,
                session.price, session.paid, ticksLeft, session.centerX, session.centerZ, session.minimumBlockCenterY,
                session.initiatorName, contributors);
        List<ServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            boolean near = player.level() == level && player.distanceToSqr(session.pos.getCenter()) <= BROADCAST_RANGE * BROADCAST_RANGE;
            if (near || session.contributions.containsKey(player.getUUID())) recipients.add(player);
        }
        for (ServerPlayer player : recipients) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    /** A player who connects while purchases are going gets the state of each (the ghost and the label over the block). */
    public static void syncTo(ServerPlayer player) {
        for (Session session : SESSIONS.values()) broadcastTo(player, session);
    }

    /** A player at the block while a purchase is going: their screen shows it instead of the vehicle list. */
    public static void sendCurrent(ServerPlayer player, Session session) {
        broadcastTo(player, session);
        PacketDistributor.sendToPlayer(player, new ClientboundOpenPurchaseSessionPacket(session.pos));
    }

    private static void broadcastTo(ServerPlayer player, Session session) {
        Map<String, Integer> contributors = new LinkedHashMap<>();
        for (var entry : session.contributions.entrySet()) {
            contributors.put(session.names.getOrDefault(entry.getKey(), "?"), entry.getValue());
        }
        PacketDistributor.sendToPlayer(player, new ClientboundPurchaseSessionPacket(session.pos, true, false, session.schematicId,
                session.name, session.price, session.paid, (int) Math.max(0, session.deadline - now), session.centerX,
                session.centerZ, session.minimumBlockCenterY, session.initiatorName, contributors));
    }
}
