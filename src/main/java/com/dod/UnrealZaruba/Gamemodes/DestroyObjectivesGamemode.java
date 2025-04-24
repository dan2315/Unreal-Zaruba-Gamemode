package com.dod.UnrealZaruba.Gamemodes;

import java.util.Map;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassEquipper;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectivesHandler;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Utils.NBT;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.GamePhase;
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
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnDataHandler;
public class DestroyObjectivesGamemode extends TeamGamemode {
    public static final String GAMEMODE_NAME = "destroyobjectives";
    private static final int STRATEGY_TIME_DURATION_MS = 30 * 1000; // 30 seconds
    private static final int GAME_DURATION_MS = 10 * 60 * 1000; // 10 minutes
    private static final int COUNTDOWN_DURATION_MS = 5 * 1000; // 5 seconds
    
    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;

    public DestroyObjectivesGamemode(GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        super();
        this.gameStatistics = gameStatisticsService;
        this.gameTimer = gameTimer;
        gameTimer.setup();

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
        objectivesHandler.OnObjectivesCompleted(() -> {
            CompleteGame(server, TeamColor.RED);
        });
        Initialize();
    }

    @Override
    protected void Initialize() {
        super.Initialize();

        MainConfig.Mode mode = MainConfig.getInstance().getMode();
        if (mode != MainConfig.Mode.GAME) return;

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
                PhaseId.STRATEGY_TIME,
                STRATEGY_TIME_DURATION_MS, // 30 seconds
                this::StartStrategyTime,
                this::UpdateStrategyTime,
                this::CompleteStrategyTime
            )
        );
        AddPhase(
            new TimedGamePhase(
                PhaseId.BATTLE,
                GAME_DURATION_MS, // 10 minutes
                this::StartBattle,
                this::UpdateBattle,
                () -> { // On phase end
                    CompleteGame(server, TeamColor.BLUE);
                }
            )
        );

        var startCondition = new CombinedOrCondition(
            new TeamsHaveEnoughPlayersCondition(TeamManager, 5, 10),
            new AllPlayersReadyCondition(10)
        );
        startCondition.SetOnConditionMet(() -> {
            CompletePhase(PhaseId.TEAM_SELECTION);
        });

        objectivesHandler.OnObjectivesCompleted(() -> {
            CompleteGame(server, TeamColor.RED);
        });
    }

    public void StartTeamSelection() {
        server.getPlayerList().getPlayers().forEach(player -> {
            player.setGameMode(GameType.ADVENTURE);
        });
    }

    public void CompleteTeamSelection() {
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            if (!TeamManager.IsInTeam(player)) {
                var team = TeamManager.GetWithMinimumMembers();
                if (team != null) {
                    UnrealZaruba.LOGGER.info("[UnrealZaruba] Player not in team {} joined team {}", player.getName().getString(), team.Color());
                    TeamManager.AssignToTeam(team.Color(), player);
                }
            }

            TeamManager.teleportToSpawn(player);
        }
        CharacterClassEquipper.equipTeamWithSelectedClasses(players);
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
        objectivesHandler.load();
        objectivesHandler.addRecipients(TeamManager.GetTeams());
    }

    public void StartStrategyTime() {
    }

    public void UpdateStrategyTime(int ticks) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (STRATEGY_TIME_DURATION_MS / 1000) - (ticks / 20);
        gameTimer.update(secondsRemaining % 60, secondsRemaining / 60, true);
    }

    public void CompleteStrategyTime() {
        TransitToNextPhase();
    }

    public void StartBattle() {
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Starting battle");

        TeamManager.PlayBattleSound();

        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            var team = TeamManager.GetPlayersTeam(serverPlayer);
            if (team == null) continue;
            
            TitleMessage.showTitle(serverPlayer, 
                startGameTexts.get(team.Color()).GetTitle(),
                startGameTexts.get(team.Color()).GetSubtitle());
        }

        UnrealZaruba.LOGGER.info("[UnrealZaruba] Triggering vehicle spawn blocks");

        GamemodeDataManager
        .getDataHandler(this.getClass(), VehicleSpawnDataHandler.class)
        .triggerVehicleSpawns(server);
    }

    public void UpdateBattle(int ticks) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (GAME_DURATION_MS / 1000) - (ticks / 20);
        gameTimer.update(secondsRemaining % 60, secondsRemaining / 60, true);
    }

    @Override
    public void onServerTick(TickEvent.ServerTickEvent serverTickEvent) {
    }

    @Override
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
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
        WorldManager.teleportPlayerToDimension(serverPlayer, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
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
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.SPECTATOR);
            player.getInventory().clearContent();
        }
        if (gameStatistics != null) gameStatistics.SendGameData(this.getClass().getSimpleName(), TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
        TimerManager.createRealTimeTimer(3 * 1000 /*10s*/, () -> CompleteGameDelayed(server), null);
        // scheduledExecutorService.schedule(() -> CompleteGameDelayed(server), 10, TimeUnit.SECONDS); // Vot tak ne delat'
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION);
        WorldManager.ResetGameWorldDelayed();
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.SPECTATOR);
            player.getInventory().clearContent();
        }
        
        objectivesHandler.reset();
        TeamManager.reset();
        GetCurrentPhase().Clear();
        ProceedToPhaseForced(PhaseId.TEAM_SELECTION);
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
}
