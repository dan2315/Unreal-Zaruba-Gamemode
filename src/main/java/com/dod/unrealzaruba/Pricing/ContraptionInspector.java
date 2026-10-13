package com.dod.unrealzaruba.Pricing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundContraptionReportPacket;
import com.dod.unrealzaruba.SableIntegration.SableShips;
import com.dod.unrealzaruba.UnrealZaruba;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Backs the contraption scanner item. Using it on a Sable contraption scans its blocks (and what is stored inside them)
 * on the server, prices and checks the result with the backend rules ({@link RulesService}), and sends it to the
 * player, who keeps seeing it (also without holding the scanner) until they Shift-click with it. The same
 * contraption is scanned again every second while the player is near, so edits show up at once. Everything is computed on the server because clients do not know what is inside containers.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID)
public final class ContraptionInspector {
    private static final int REFRESH_TICKS = 20;
    /** Farther than this the contraption is not rescanned (it may not even be loaded); the last report stays on screen. */
    private static final double RESCAN_DISTANCE = 160;
    /** A scan walks every position of the contraption box; refuse absurd boxes instead of stalling the server. */
    private static final long MAX_VOLUME = 4_000_000L;
    private static final int REPORT_LINES = 6;

    /** Player to the sub-level they are scanning. */
    private static final Map<UUID, UUID> TRACKED = new HashMap<>();

    private ContraptionInspector() {}

    /** The scanner was used on a block; {@code pos} is where the player clicked (plot coordinates on a contraption). */
    public static void inspect(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        SubLevel subLevel = SableShips.getContaining(level, pos);
        if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
            player.displayClientMessage(Component.translatable("contraption.unrealzaruba.not_contraption"), true);
            return;
        }
        TRACKED.put(player.getUUID(), serverSubLevel.getUniqueId());
        send(player, serverSubLevel);
    }

    /** Shift-click with the scanner: stop following the contraption and clear the panel. */
    public static void stop(ServerPlayer player) {
        if (TRACKED.remove(player.getUUID()) != null) {
            PacketDistributor.sendToPlayer(player, new ClientboundContraptionReportPacket(null));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TRACKED.isEmpty() || event.getServer().getTickCount() % REFRESH_TICKS != 0) return;
        Iterator<Map.Entry<UUID, UUID>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> tracked = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(tracked.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            ServerSubLevel subLevel = SableShips.get(player.serverLevel(), tracked.getValue());
            if (subLevel == null) {
                // The contraption is gone (removed, or in another dimension): nothing left to follow
                iterator.remove();
                PacketDistributor.sendToPlayer(player, new ClientboundContraptionReportPacket(null));
                continue;
            }
            var position = subLevel.logicalPose().position();
            if (player.position().distanceToSqr(position.x(), position.y(), position.z()) <= RESCAN_DISTANCE * RESCAN_DISTANCE) {
                send(player, subLevel);
            }
        }
    }

    private static void send(ServerPlayer player, ServerSubLevel subLevel) {
        PacketDistributor.sendToPlayer(player, new ClientboundContraptionReportPacket(report(player.serverLevel(), subLevel)));
    }

    /** Scans the contraption as it is now and checks it against the current backend rules. */
    public static ContraptionReport report(ServerLevel level, SubLevel subLevel) {
        String name = subLevel.getName() == null ? "" : subLevel.getName();
        BoundingBox3ic box = subLevel.getPlot().getBoundingBox();
        long volume = (long) (box.maxX() - box.minX() + 1) * (box.maxY() - box.minY() + 1) * (box.maxZ() - box.minZ() + 1);
        RulesService.Rules rules = RulesService.get();
        if (volume > MAX_VOLUME) {
            return new ContraptionReport(name, null, rules == null ? null : rules.maxSize(), 0, 0, 0, 0, List.of(), 0,
                    List.of("Конструкция слишком велика для сканирования"), false, false);
        }

        Composition composition = CompositionScanner.scanLive(level,
                new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
        if (rules == null) {
            return new ContraptionReport(name, composition.size, null, 0, 0, 0, 0, List.of(), 0, List.of(), false, true);
        }

        Pricer.Breakdown price = Pricer.price(composition, rules.catalog());
        Validator.Check check = Validator.check(composition, rules.catalog(), rules.maxSize());
        List<ContraptionReport.Line> lines = new ArrayList<>();
        for (Pricer.Line line : price.lines()) {
            if (lines.size() >= REPORT_LINES) break;
            lines.add(new ContraptionReport.Line(kindCode(line.kind()), line.id(), line.count(), line.subtotal().doubleValue()));
        }
        return new ContraptionReport(name, composition.size, rules.maxSize(), price.total().doubleValue(),
                price.blocks().doubleValue(), price.items().doubleValue(), price.fluids().doubleValue(), lines,
                price.unpriced().size(), check.problems(), check.isAllowed(), false);
    }

    private static int kindCode(String kind) {
        return switch (kind) {
            case Pricer.ITEM -> 1;
            case Pricer.FLUID -> 2;
            default -> 0;
        };
    }
}
