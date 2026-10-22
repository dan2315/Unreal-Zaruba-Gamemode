package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
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
    
    /** Whether the player is in a team of this match (a player who comes back keeps what they carry). */
    public boolean isInTeam(ServerPlayer player) {
        return TeamManager != null && TeamManager.IsInTeam(player);
    }

    /** Only players of a team die for real; the rest are healed (they are still choosing). */
    @Override
    protected boolean IsRealDeath(ServerPlayer player) {
        return super.IsRealDeath(player) && TeamManager != null && TeamManager.IsInTeam(player);
    }

    @Override
    protected net.minecraft.core.BlockPos DeathRespawnPosition(ServerPlayer player) {
        var team = TeamManager == null ? null : TeamManager.GetPlayersTeam(player);
        return team == null ? null : team.MainSpawn();
    }

    /**
     * A player has chosen a team by stepping on its block. While the match is on that takes them straight into the battle:
     * to their base, with their class, as a normal player.
     */
    public void HandleTeamChosen(ServerPlayer player) {
        if (GetCurrentPhaseId() != PhaseId.BATTLE || TeamManager == null || !TeamManager.IsInTeam(player)) return;
        TeamManager.teleportToChosenOrPriority(player);
        com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper.equipPlayerWithSelectedClass(player);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setInvulnerable(false);
        NBT.addEntityTag(player, "isPlayerDead", 0);
        RespawnPointsSync.sendTo(player);
    }

    @Override
    public boolean HandleDeathRespawn(ServerPlayer player) {
        if (!super.HandleDeathRespawn(player)) return false;
        // The camera flies over the body for the waiting time (the base class moves it); the map opens when it is over
        RespawnPointsSync.sendTo(player);
        var spot = deathSpotOf(player.getUUID());
        PacketDistributor.sendToPlayer(player, spot == null ? new ClientboundDeathStatePacket(true, RESPAWN_DURATION_SECONDS)
                : new ClientboundDeathStatePacket(true, RESPAWN_DURATION_SECONDS, spot.x, spot.y, spot.z, deathYawOf(player.getUUID())));
        return true;
    }

    /** The waiting is over: the player picks a respawn point on the map, and returns only when they confirm one. */
    @Override
    protected void OnRespawnReady(ServerPlayer player) {
        TeamManager.teleportToSpawnByPriority(player);
        RespawnPointsSync.sendTo(player);
        PacketDistributor.sendToPlayer(player, new ClientboundDeathStatePacket(true, 0));
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
