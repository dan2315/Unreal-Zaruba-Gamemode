package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.unrealzaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.Gamemodes.StartCondition.TimePassedCondition;
import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.unrealzaruba.Gamemodes.StartCondition.CombinedOrCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import net.minecraft.world.Difficulty;
import net.minecraft.server.MinecraftServer;
import com.dod.unrealzaruba.utils.NBT;
import com.dod.unrealzaruba.utils.Timers.TimerManager;

import net.minecraft.world.entity.player.Player;
import com.dod.unrealzaruba.Gamemodes.StartCondition.TeamsHaveEnoughPlayersCondition;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.unrealzaruba.Vehicles.VehicleManager;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Config.MainConfig;

import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;

public class ShipsGamemode extends TeamGamemode {
    public static final String GAMEMODE_NAME = "ships";
    private static final HashMap<TeamColor, BlockPos> TEAM_PREPARATION_POINTS = new HashMap<>();
    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;
    private VehicleManager vehicleManager;

    public ShipsGamemode(VehicleManager vehicleManager, GameStatisticsService gamestatistics, IGameTimer gameTimer) 
    {
        super();
        this.gameStatistics = gamestatistics;
        this.gameTimer = gameTimer;
        this.vehicleManager = vehicleManager;

        TEAM_PREPARATION_POINTS.put(TeamColor.RED, new BlockPos(-147, 54, -21));
        TEAM_PREPARATION_POINTS.put(TeamColor.BLUE, new BlockPos(-147, 54, -121));
        TEAM_PREPARATION_POINTS.put(TeamColor.YELLOW, new BlockPos(-147, 54, 79));
        
        Initialize();
    }

    @Override
    protected void Initialize() {
        super.Initialize();

        AddPhase(new ConditionalPhase(
            PhaseId.TEAM_SELECTION, 
            this::StartTeamSelection,
            this::CompleteTeamSelection,
            new CombinedOrCondition(
                new TeamsHaveEnoughPlayersCondition(TeamManager, 5, 10),
                new TimePassedCondition(10)
        )))
        .AddPhase(new ConditionalPhase(
            PhaseId.PREPARATION,
            this::StartPreparation,
            this::CompletePreparation,
            new TimePassedCondition(10)
        ))
        .AddPhase(new ConditionalPhase(
            PhaseId.GAME,
            this::StartGamePhase,
            this::CompleteGamePhase,
            new TimePassedCondition(30 * 60)
        ));

        objectivesHandler.OnObjectivesCompleted(this::CompleteGamePhase);
        ProceedToPhaseForced(PhaseId.TEAM_SELECTION);
    }

    private void StartTeamSelection() {
        UnrealZaruba.LOGGER.info("Starting lobby");
        // disable pvp
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION, new BlockPos(-95, 55, -21));
        server.setDifficulty(Difficulty.PEACEFUL, true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.setGameMode(GameType.ADVENTURE);
        }
    }

    private void CompleteTeamSelection() {
        UnrealZaruba.LOGGER.info("Completing team selection");
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            if (!TeamManager.IsInTeam(player)) {
                var team = TeamManager.GetWithMinimumMembers();
                if (team != null) {
                    UnrealZaruba.LOGGER.info("[UnrealZaruba] Player not in team {} joined team {}", player.getName().getString(), team.Color());
                    TeamManager.AssignToTeam(team.Color(), player);
                }
            }

        }
    }

    private void StartPreparation() {
        UnrealZaruba.LOGGER.info("Starting preparation");
        for (Map.Entry<TeamColor, BlockPos> entry : TEAM_PREPARATION_POINTS.entrySet()) {
            TeamColor teamColor = entry.getKey();
            BlockPos blockPos = entry.getValue();
            TeamManager.Get(teamColor).TeleportAll(blockPos);
        }
        
        GamemodeDataManager
        .getHandler(this.getClass(), VehicleSpawnData.class)
        .triggerVehicleSpawns(server);
    }

    private void CompletePreparation() {
        UnrealZaruba.LOGGER.info("Completing preparation");
        var players = server.getPlayerList().getPlayers();
        CharacterClassEquipper.equipTeamWithSelectedClasses(players);
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
        objectivesHandler.load();
        objectivesHandler.addRecipients(TeamManager.GetTeams());
    }

    private void StartGamePhase() {
        UnrealZaruba.LOGGER.info("Starting game");
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            TeamManager.teleportToSpawnByPriority(player);
        }
        objectivesHandler.load();
        objectivesHandler.addRecipients(TeamManager.GetTeams());
        

        vehicleManager.getVehicles().forEach(vehicle -> {
            TeamManager.Get(vehicle.getOwner()).AddRespawnPoint(vehicle);
        });
    }

    private void CompleteGamePhase() {
        UnrealZaruba.LOGGER.info("Completing game");
        TimerManager.createRealTimeTimer(3 * 1000 /*10s*/, () -> CompleteGameDelayed(server), null);
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION);
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
        }
        
        objectivesHandler.reset();
        TeamManager.reset();
        GetCurrentPhase().Clear();
        GamemodeManager.instance.StartVoting();
    }

    public void HandleConnectedPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();

        var isInTeam = TeamManager.IsInTeam(serverPlayer);
        var isDead = NBT.readEntityTag(serverPlayer, "isPlayerDead");

        if (server == null)
            return;

        if (GetCurrentPhaseId() == PhaseId.TEAM_SELECTION) {
            TeleportToLobby(serverPlayer, server);
            return;
        }
        if (GetCurrentPhaseId() == PhaseId.GAME && !isInTeam) {
            MakePlayerSpectator(serverPlayer, server);
            return;
        }
        if (GetCurrentPhaseId() == PhaseId.GAME && isDead == 1) {
            TeamManager.teleportToSpawnByPriority(serverPlayer);
            serverPlayer.setGameMode(GameType.ADVENTURE);
        }
    }

    private void TeleportToLobby(ServerPlayer serverPlayer, MinecraftServer server) {
        WorldManager.teleportPlayerToDimension(serverPlayer, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
    }

    private void MakePlayerSpectator(ServerPlayer serverPlayer, MinecraftServer server) {
        serverPlayer.setGameMode(GameType.SPECTATOR);
        var spawn = server.overworld().getSharedSpawnPos();
        serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
    }
}