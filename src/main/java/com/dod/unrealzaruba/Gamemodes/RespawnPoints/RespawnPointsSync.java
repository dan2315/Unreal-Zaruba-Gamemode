package com.dod.unrealzaruba.Gamemodes.RespawnPoints;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.TeamGamemode;
import com.dod.unrealzaruba.ModBlocks.Tent.Tent;
import com.dod.unrealzaruba.NetworkPackets.RespawnPoints.ClientboundRespawnPointsPacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.Vehicles.Vehicle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Keeps clients' respawn point lists (shown on the map) in step with the server. */
public final class RespawnPointsSync {
    private RespawnPointsSync() {}

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundRespawnPointsPacket(viewFor(player)));
    }

    public static void sendToTeam(TeamContext team) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || team == null) return;
        for (UUID member : List.copyOf(team.Members())) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                sendTo(player);
            }
        }
    }

    public static RespawnPointsView viewFor(ServerPlayer player) {
        if (!(PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext context) || context.Team() == null) {
            return RespawnPointsView.EMPTY;
        }
        List<IRespawnPoint> points = context.Team().RespawnPoints();
        List<RespawnPointsView.Entry> entries = new ArrayList<>();
        for (IRespawnPoint point : points) {
            entries.add(new RespawnPointsView.Entry(point.getRuntimeId(), point.getDisplayName(), point.getSpawnPosition(),
                    kindOf(point), point instanceof CapturePointRespawnPoint cp ? cp.getObjective().GetRuntimeId() : null));
        }

        IRespawnPoint selected = context.RespawnPoint();
        if (selected != null && !points.contains(selected)) {
            selected = null;
        }
        IRespawnPoint effective = selected != null ? selected
                : points.stream().min(Comparator.comparingInt(IRespawnPoint::getPriority)).orElse(null);
        return new RespawnPointsView(entries,
                selected == null ? null : selected.getRuntimeId(),
                effective == null ? null : effective.getRuntimeId(),
                enemyBases(context.Team()));
    }

    /** The main bases of the other teams: where the enemy starts is public knowledge, their tents and vehicles are not. */
    private static List<RespawnPointsView.EnemyBase> enemyBases(TeamContext own) {
        List<RespawnPointsView.EnemyBase> bases = new ArrayList<>();
        if (GamemodeManager.instance != null && GamemodeManager.instance.GetActiveGamemode() instanceof TeamGamemode gamemode
                && gamemode.GetTeamManager() != null) {
            for (TeamContext team : gamemode.GetTeamManager().GetTeams().values()) {
                if (team == own || team.RespawnPoints().isEmpty()) continue;
                bases.add(new RespawnPointsView.EnemyBase(team.Color(), team.MainSpawn()));
            }
        }
        return bases;
    }

    private static RespawnPointsView.Kind kindOf(IRespawnPoint point) {
        if (point instanceof CapturePointRespawnPoint) return RespawnPointsView.Kind.CAPTURE_POINT;
        if (point instanceof Tent) return RespawnPointsView.Kind.TENT;
        if (point instanceof Vehicle) return RespawnPointsView.Kind.VEHICLE;
        return RespawnPointsView.Kind.BASE;
    }
}
