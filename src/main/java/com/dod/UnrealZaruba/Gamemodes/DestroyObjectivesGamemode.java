package com.dod.UnrealZaruba.Gamemodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Utils.NBT;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.GamePhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.TimedGamePhase;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.EnoughPlayersCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.StartCondition;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.SustainedPlayerCountCondition;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import net.minecraft.server.level.ServerLevel;

public class DestroyObjectivesGamemode extends TeamGamemode {
    // private static final int GAME_DURATION_MS = 10 * 60 * 1000; // 10 minutes
    private static final int GAME_DURATION_MS = 20 * 1000; // 10 minutes
    private static final int COUNTDOWN_DURATION_MS = 5 * 1000; // 10 seconds
    
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private GameStatisticsService gameStatistics;
    private GameTimer gameTimer;
    private MinecraftServer server;

    private DestructibleObjectivesHandler objectivesHandler;
    private StartCondition startCondition;

    public DestroyObjectivesGamemode(MinecraftServer server, GameStatisticsService leaderboardService, GameTimer gameTimer) {
        this.gameStatistics = leaderboardService;
        this.gameTimer = gameTimer;
        currentGamemode = this;
        this.server = server;

        TeamManager teamManager = new TeamManager();
        teamManager.Initialize();
        SetTeamManager(teamManager);

        TeamManager.Get(TeamColor.RED).setNotificationMessage(objective -> "Ваша §l§4команда§r атакует точку §b" + objective.GetName() + "§r. Гойда!");
        TeamManager.Get(TeamColor.BLUE).setNotificationMessage(objective -> "Ваша точка §b" + objective.GetName() + "§r атакована §l§4противниками§r");
        
        startGameTexts.put(TeamColor.RED, new StartGameText(
                "§c Игра началась, в бой!",
                "Необходимо уничтожить вражескую базу"));
        startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Продержитесь до конца раунда"));
        // TODO: Utils.LoadChunksInArea(server.getLevel(Level.OVERWORLD), -1024, -512, 1024, 512);

        ServerLifecycleHooks.getCurrentServer().setDifficulty(Difficulty.PEACEFUL, true);
        objectivesHandler = new DestructibleObjectivesHandler();
        var objectives = objectivesHandler.load();
        for (var objective : objectives) {
            for (Map.Entry<TeamColor, TeamContext> team : TeamManager.GetTeams().entrySet()) {
                objective.addNotificationRecipient(team.getValue());
            }
        }
    }

    @Override
    protected void Initialize() {
        super.Initialize();
        
        UnrealZaruba.LOGGER.info("Initializing DestroyObjectivesGamemode");


        AddPhase(
            new GamePhase(
                PhaseId.TEAM_SELECTION,
                this::StartTeamSelection,
                this::CompleteTeamSelection
            )   
        ).
        AddPhase(
            new TimedGamePhase(
                PhaseId.BATTLE,
                GAME_DURATION_MS,
                this::StartBattle,
                this::UpdateBattle,
                () -> { // On phase end
                    CompleteGame(server, TeamColor.BLUE);
                }
            )
        );

        ProceedToPhaseForced(PhaseId.TEAM_SELECTION);

        // startCondition = new SustainedPlayerCountCondition(TeamManager, 1, 10); // 5 players in each team for 10 sec
        startCondition = new EnoughPlayersCondition(1);
        startCondition.SetOnConditionMet(() -> {
            ProceedToPhaseForced(PhaseId.BATTLE);
        });

        objectivesHandler.OnObjectivesCompleted(() -> {
            CompleteGame(server, TeamColor.RED);
        });
    }

    public void StartTeamSelection() {
    }

    public void CompleteTeamSelection() {
    }

    public void StartBattle() {
        gameTimer.setupScoreboard();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TeamManager.teleportToSpawn(player);
        }
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
        TeamManager.PlayBattleSound();

        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            var team = TeamManager.GetPlayersTeam(serverPlayer);
            if (team == null) continue;
            
            TitleMessage.showTitle(serverPlayer, 
                startGameTexts.get(team.Color()).GetTitle(),
                startGameTexts.get(team.Color()).GetSubtitle());
        }

        var success = TeamManager.DeleteBarriersAtSpawn();
        if (!success) {
            UnrealZaruba.LOGGER.error("Спавны команд ещё не готовы");
        }
    }

    public void UpdateBattle(int ticks) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (GAME_DURATION_MS / 1000) - (ticks / 20);
        gameTimer.updateScoreboardTimerMinutes(secondsRemaining / 60);
        gameTimer.updateScoreboardTimerSeconds(secondsRemaining % 60);
    }

    @Override
    public void onServerTick(TickEvent.ServerTickEvent serverTickEvent) {
        startCondition.Update();
        objectivesHandler.onServerTick();
    }

    @Override
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        objectivesHandler.onPlayerTick(event);
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
        if (GetCurrentPhaseId() == PhaseId.BATTLE && !isInTeam) {
            MakePlayerSpectator(serverPlayer, server);
            return;
        }
        if (GetCurrentPhaseId() == PhaseId.BATTLE && isDead == 1) {
            ReturnToTeamSpawn(serverPlayer);
        }
    }

    // TODO: All methods below is like more util

    private void TeleportToLobby(ServerPlayer serverPlayer, MinecraftServer server) {
        serverPlayer.teleportTo(server.getLevel(WorldManager.LOBBY_DIMENSION), 0, 16, 0, Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot());
    }

    private void ReturnToTeamSpawn(ServerPlayer serverPlayer) {
        TeamManager.teleportToSpawn(serverPlayer);
        serverPlayer.setGameMode(GameType.ADVENTURE);
    }

    private void MakePlayerSpectator(ServerPlayer serverPlayer, MinecraftServer server) {
        serverPlayer.setGameMode(GameType.SPECTATOR);
        var spawn = server.overworld().getSharedSpawnPos();
        serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
    }

    public void CompleteGame(MinecraftServer server, TeamColor wonTeam) {
        ShowEndText(server, wonTeam);
        if (gameStatistics != null) gameStatistics.SendGameData(this.getClass().getSimpleName(), TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
        TimerManager.createRealTimeTimer(10 * 1000 /*10s*/, () -> CompleteGameDelayed(server), null);
        // scheduledExecutorService.schedule(() -> CompleteGameDelayed(server), 10, TimeUnit.SECONDS); // Vot tak ne delat'
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        WorldManager.TeleportAllPlayersToLobby(server);
        WorldManager.ResetGameWorldDelayed();
        startCondition.ResetCondition();
        objectivesHandler.reset();
        TeamManager.reset();
    }

    public void ShowEndText(MinecraftServer server, TeamColor wonTeam) {
        String colorCode = wonTeam.getColorCode();
        Component titleText = Component.literal(
                "Команда " + colorCode + wonTeam.toString() + ChatFormatting.RESET + " победила");
        Component wonText = Component.literal("Можешь сказать оппоненту \'Сори, что трахнул\'");
        Component loseText = Component.literal("Что могу сказать? Старайся лучше");
        for (var player : server.getPlayerList().getPlayers()) {
            TeamContext team = TeamManager.GetPlayersTeam(player);
            if (team == null)
                continue;
            if (team.Color() == wonTeam) {
                TitleMessage.showTitle(player, titleText, wonText);
            } else {
                TitleMessage.showTitle(player, titleText, loseText);
            }
        }
    }

    public void preventLevelSaving(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            level.noSave = true;
        }
    }

    public void preventLevelSaving(ServerLevel level) {
        if (level != null) {
            level.noSave = true;
        }
    }
}
