package com.dod.unrealzaruba.Vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.TeamGamemode;
import com.dod.unrealzaruba.NetworkPackets.Vehicles.ClientboundTeamVehiclesPacket;
import com.dod.unrealzaruba.SableIntegration.SableShips;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The vehicles teams bought, followed while they exist: each team's players are told every half second where their
 * team's vehicles are and what they are called (for the labels over them and the waypoints on the map).
 */
public final class TrackedVehicles {
    private static final int SEND_EVERY_TICKS = 4;
    private static final int CLAIM_TICKS = 20 * 20;

    private record Tracked(UUID id, String name, TeamColor team) {}

    /** A vehicle just placed whose body is not known yet: it is the new sub-level that appears next. */
    private static final class Claim {
        final Set<UUID> before;
        final String name;
        final TeamColor team;
        final long expires;

        Claim(Set<UUID> before, String name, TeamColor team, long expires) {
            this.before = before;
            this.name = name;
            this.team = team;
            this.expires = expires;
        }
    }

    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();
    private static final List<Claim> CLAIMS = new ArrayList<>();
    private static final Set<TeamColor> SENT_SOMETHING = new HashSet<>();
    private static long now;

    private TrackedVehicles() {}

    /** The ids of the sub-levels that exist now: take it before placing a vehicle, give it to {@link #claim}. */
    public static Set<UUID> snapshot(ServerLevel level) {
        Set<UUID> ids = new HashSet<>();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null) {
            for (ServerSubLevel subLevel : container.getAllSubLevels()) ids.add(subLevel.getUniqueId());
        }
        return ids;
    }

    /**
     * Starts following a vehicle that was just placed. {@code rootId} is its body when the placement says which one it
     * is; otherwise it is the biggest sub-level that shows up that was not there in {@code before}.
     */
    public static void claim(Set<UUID> before, String name, TeamColor team, UUID rootId) {
        if (team == null) return;
        if (rootId != null) {
            TRACKED.put(rootId, new Tracked(rootId, name, team));
            return;
        }
        CLAIMS.add(new Claim(before, name, team, now + CLAIM_TICKS));
    }

    public static void tick(MinecraftServer server) {
        now++;
        ServerLevel level = WorldManager.gameLevel;
        if (level == null) return;
        if (now % 5 == 0 && !CLAIMS.isEmpty()) processClaims(level);
        if (now % SEND_EVERY_TICKS != 0) return;
        send(server, level);
    }

    public static void reset() {
        // Clients keep the last list they were given, so they are told it is empty now
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && !TRACKED.isEmpty() || !SENT_SOMETHING.isEmpty()) {
            if (server != null) {
                var empty = new ClientboundTeamVehiclesPacket(List.of());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(player, empty);
            }
        }
        TRACKED.clear();
        CLAIMS.clear();
        SENT_SOMETHING.clear();
    }

    private static void processClaims(ServerLevel level) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        for (Iterator<Claim> it = CLAIMS.iterator(); it.hasNext(); ) {
            Claim claim = it.next();
            if (now > claim.expires) {
                UnrealZaruba.LOGGER.info("[Vehicles] {} never showed up as a sub-level, it is not tracked", claim.name);
                it.remove();
                continue;
            }
            ServerSubLevel biggest = null;
            double biggestVolume = -1;
            for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                UUID id = subLevel.getUniqueId();
                if (subLevel.isRemoved() || claim.before.contains(id) || TRACKED.containsKey(id)) continue;
                BoundingBox3dc box = subLevel.boundingBox();
                double volume = (box.maxX() - box.minX()) * (box.maxY() - box.minY()) * (box.maxZ() - box.minZ());
                if (volume > biggestVolume) {
                    biggestVolume = volume;
                    biggest = subLevel;
                }
            }
            if (biggest != null) {
                TRACKED.put(biggest.getUniqueId(), new Tracked(biggest.getUniqueId(), claim.name, claim.team));
                it.remove();
            }
        }
    }

    private static void send(MinecraftServer server, ServerLevel level) {
        Map<TeamColor, List<ClientboundTeamVehiclesPacket.Entry>> byTeam = new HashMap<>();
        for (Iterator<Tracked> it = TRACKED.values().iterator(); it.hasNext(); ) {
            Tracked tracked = it.next();
            ServerSubLevel subLevel = SableShips.get(level, tracked.id());
            if (subLevel == null) {
                it.remove(); // destroyed or removed
                continue;
            }
            BoundingBox3dc box = subLevel.boundingBox();
            byTeam.computeIfAbsent(tracked.team(), team -> new ArrayList<>()).add(new ClientboundTeamVehiclesPacket.Entry(
                    tracked.id(), tracked.name(), tracked.team(), (box.minX() + box.maxX()) / 2, box.maxY(),
                    (box.minZ() + box.maxZ()) / 2, (box.minY() + box.maxY()) / 2));
        }

        if (!(GamemodeManager.instance != null && GamemodeManager.instance.GetActiveGamemode() instanceof TeamGamemode gamemode)
                || gamemode.GetTeamManager() == null) {
            return;
        }
        for (TeamContext team : gamemode.GetTeamManager().GetTeams().values()) {
            List<ClientboundTeamVehiclesPacket.Entry> entries = byTeam.getOrDefault(team.Color(), List.of());
            boolean hadBefore = SENT_SOMETHING.contains(team.Color());
            if (entries.isEmpty() && !hadBefore) continue; // nothing to say, and nothing to take back
            if (entries.isEmpty()) SENT_SOMETHING.remove(team.Color()); else SENT_SOMETHING.add(team.Color());
            var packet = new ClientboundTeamVehiclesPacket(entries);
            for (UUID member : List.copyOf(team.Members())) {
                ServerPlayer player = server.getPlayerList().getPlayer(member);
                if (player != null) PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }
}
