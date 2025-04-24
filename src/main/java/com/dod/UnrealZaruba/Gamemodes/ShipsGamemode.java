package com.dod.UnrealZaruba.Gamemodes;

import java.util.Map;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassEquipper;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.AbstractGamePhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.AbstractGamePhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.GamePhase;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.TimePassedCondition;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.TimedGamePhase;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.AllPlayersReadyCondition;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.CombinedOrCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.Condition;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.TeamsHaveEnoughPlayersCondition;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Config.MainConfig;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnDataHandler;


public class ShipsGamemode extends TeamGamemode {
    public static final String GAMEMODE_NAME = "ships";
    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;

    public ShipsGamemode(GameStatisticsService gamestatistics, IGameTimer gameTimer) 
    {
        super();
        this.gameStatistics = gamestatistics;
        this.gameTimer = gameTimer;
        gameTimer.setup();
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
                new TimePassedCondition(60)
        )))
        .AddPhase(new ConditionalPhase(
            PhaseId.PREPARATION,
            this::StartPreparation,
            this::CompletePreparation,
            new TimePassedCondition(60)
        ))
        .AddPhase(new ConditionalPhase(
            PhaseId.GAME,
            this::StartGamePhase,
            this::CompleteGamePhase,
            new TimePassedCondition(30 * 60)
        ));

        objectivesHandler.OnObjectivesCompleted(this::CompleteGamePhase);
    }

    private void StartLobby() {
        UnrealZaruba.LOGGER.info("Starting lobby");
        // disable pvp
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION);
        server.setDifficulty(Difficulty.PEACEFUL, true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.setGameMode(GameType.ADVENTURE);
        }
    }

    private void CompleteLobby() {
        UnrealZaruba.LOGGER.info("Completing lobby");
        WorldManager.TeleportAllPlayersTo(server, WorldManager.GAME_DIMENSION);
    }

    private void StartTeamSelection() {
        UnrealZaruba.LOGGER.info("Starting team selection");
    }

    private void CompleteTeamSelection() {
        UnrealZaruba.LOGGER.info("Completing team selection");
    }

    private void StartPreparation() {
        UnrealZaruba.LOGGER.info("Starting preparation");
    }

    private void CompletePreparation() {
        UnrealZaruba.LOGGER.info("Completing preparation");
    }

    private void StartGamePhase() {
        UnrealZaruba.LOGGER.info("Starting game");
        objectivesHandler.load();
        objectivesHandler.addRecipients(TeamManager.GetTeams());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.setGameMode(GameType.SURVIVAL);
        }

        GamemodeDataManager
        .getDataHandler(this.getClass(), VehicleSpawnDataHandler.class)
        .triggerVehicleSpawns(server);

        
    }

    private void CompleteGamePhase() {
        UnrealZaruba.LOGGER.info("Completing game");
    }
}
