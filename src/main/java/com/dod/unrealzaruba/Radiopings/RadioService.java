package com.dod.unrealzaruba.Radiopings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundRadioPacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side of radio commands: checks a phrase and reads it to the sender's team. */
public final class RadioService {
    private static final long COOLDOWN_MS = 3000;

    private static final Map<UUID, Long> lastSent = new HashMap<>();

    private RadioService() {}

    /** Drop the cooldown of a player who left, so the maps stay bounded by the online players. */
    public static void forget(UUID id) {
        lastSent.remove(id);
    }

    /** Debug only: forget everything (called when the singleplayer bypass is switched off). */
    static void clear() {
        lastSent.clear();
    }

    public static void onRadio(ServerPlayer sender, byte phraseId) {
        RadioPhrase phrase = RadioPhrase.byId(phraseId);
        if (phrase == null) return;
        TeamContext team = PlayerContext.Get(sender.getUUID()) instanceof TeamPlayerContext context ? context.Team() : null;
        boolean debug = team == null && RadioDebug.DEBUG_FORCE;
        if (team == null && !debug) return;
        // The dead keep talking: radio is heard by the whole team anywhere.
        if (!debug && !matchRunning()) return;
        long now = System.currentTimeMillis();
        if (!cooldownOk(lastSent, sender.getUUID(), now, COOLDOWN_MS)) return;
        lastSent.put(sender.getUUID(), now);

        int color = team != null ? 0xFF000000 | team.GetIntColor() : 0xFF00FFA3;
        MinecraftServer server = sender.server;
        List<UUID> recipients = team != null ? List.copyOf(team.Members()) : List.of(sender.getUUID());
        for (UUID id : recipients) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            if (team != null && !player.level().dimension().equals(WorldManager.GAME_DIMENSION)) continue;
            PacketDistributor.sendToPlayer(player,
                    new ClientboundRadioPacket(sender.getUUID(), sender.getGameProfile().getName(), phraseId, color));
        }
    }

    /** Radio and pings only make sense once teams are out planning or fighting, not in the lobby. */
    public static boolean matchRunning() {
        if (GamemodeManager.instance == null || GamemodeManager.instance.GetActiveGamemode() == null) return false;
        PhaseId phase = GamemodeManager.instance.GetActiveGamemode().GetCurrentPhaseId();
        return phase == PhaseId.STRATEGY_TIME || phase == PhaseId.BATTLE || phase == PhaseId.GAME;
    }

    /** Pure cooldown check, so it can be unit-tested. */
    static boolean cooldownOk(Map<UUID, Long> last, UUID id, long nowMs, long cooldownMs) {
        return nowMs - last.getOrDefault(id, 0L) >= cooldownMs;
    }
}
