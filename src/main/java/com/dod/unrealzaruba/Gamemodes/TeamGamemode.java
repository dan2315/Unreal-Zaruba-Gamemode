package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.GameText.StartGameText;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.utils.NBT;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsSync;
import com.dod.unrealzaruba.NetworkPackets.RespawnPoints.ClientboundDeathStatePacket;
import net.neoforged.neoforge.network.PacketDistributor;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;

public class TeamGamemode extends BaseGamemode {
    protected TeamManager TeamManager;
    public HashMap<TeamColor, StartGameText> startGameTexts = new HashMap<>();
    
    // Shortest map loading phase: clients use it to install the map's pre-rendered world map in the lobby
    protected static final int MIN_MAP_LOADING_SECONDS = 3;

    public TeamManager GetTeamManager() { return TeamManager; }
    public void SetTeamManager(TeamManager teamManager) { TeamManager = teamManager; }
    
    public TeamGamemode() {
        super();
        TeamManager = new TeamManager();
    }

    @Override
    protected void Initialize() {
    }
    @Override
    public void HandleConnectedPlayer(Player player) {}

    @Override
    public void CheckObjectives() {}

    @Override
    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException { return 0; }



    @Override
    public void HandleRespawn(ServerPlayer player) {
        super.HandleRespawn(player);
        TeamPlayerContext playerContext = (TeamPlayerContext)PlayerContext.Get(player.getUUID());
        if (playerContext.RespawnPointSelected()) {
            TeamManager.teleportToSelectedPoint(player);
        } else {
            TeamManager.teleportToSpawnByPriority(player);
        }
        PacketDistributor.sendToPlayer(player, new ClientboundDeathStatePacket(false, 0));
    }
    
    @Override
    public void HandleDeath(ServerPlayer player, LivingDeathEvent event) {
        super.HandleDeath(player, event);
        if (PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext teamPlayerContext) {
            // Deaths before the battle are cancelled in BaseGamemode and don't count
            if (teamPlayerContext.Team() == null || NBT.readEntityTag(player, "isPlayerDead") != 1) return;
            // The client opens the map to pick a respawn point
            RespawnPointsSync.sendTo(player);
            PacketDistributor.sendToPlayer(player, new ClientboundDeathStatePacket(true, RESPAWN_DURATION_SECONDS));
        }
    }

    @Override
    public java.util.Collection<net.minecraft.core.BlockPos> GetPreloadPositions() {
        java.util.List<net.minecraft.core.BlockPos> positions = new java.util.ArrayList<>();
        if (TeamManager != null) {
            TeamManager.GetTeams().values().forEach(team -> positions.add(team.MainSpawn()));
        }
        return positions;
    }

    @Override
    public void Cleanup() {
        TeamManager.Cleanup();
    }

    @Override
    public void onPlayerTick(PlayerTickEvent.Post event) {
        super.onPlayerTick(event);
    }
}
