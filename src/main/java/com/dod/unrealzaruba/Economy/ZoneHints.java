package com.dod.unrealzaruba.Economy;

import java.util.Map;
import java.util.WeakHashMap;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundZoneHintsPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells each player whether the shop and team storage work where they stand, so that their hints under the money light up in the
 * team's respawn zones. Checked every few ticks and sent only when it changes; a new player entity (a rejoin) gets it again.
 */
public final class ZoneHints {
    private static final int EVERY_TICKS = 5;
    private static final Map<ServerPlayer, Integer> SENT = new WeakHashMap<>();
    private static int ticks;

    private ZoneHints() {}

    public static void tick(MinecraftServer server) {
        if (++ticks % EVERY_TICKS != 0) return;
        boolean match = GamemodeManager.instance.GetActiveGamemode() != null;
        EconomyConfig config = EconomyData.current();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean shown = match && TeamZones.teamOf(player) != null && player.isAlive() && !player.isSpectator();
            boolean inZone = shown && TeamZones.isInOwnZone(player);
            boolean shopEnabled = shown && config.buyMenuEnabled;
            boolean shopOpen = shopEnabled && (inZone || !config.buyOnlyInRespawnZones);
            int state = (shown ? 1 : 0) | (shopEnabled ? 2 : 0) | (shopOpen ? 4 : 0) | (inZone ? 8 : 0);
            Integer last = SENT.put(player, state);
            if (last == null || last != state) {
                PacketDistributor.sendToPlayer(player, new ClientboundZoneHintsPacket(shown, shopEnabled, shopOpen, inZone));
            }
        }
    }
}
