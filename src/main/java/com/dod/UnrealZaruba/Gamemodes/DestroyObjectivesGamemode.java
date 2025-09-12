package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Gamemodes.Barriers.BarrierVolumesData;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.CombinedOrCondition;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.TeamsHaveEnoughPlayersCondition;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.TimePassedCondition;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassEquipper;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Utils.BarrierRemovalTask;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Utils.NBT;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import com.dod.UnrealZaruba.Config.MainConfig;
import com.dod.UnrealZaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;

public class DestroyObjectivesGamemode extends TeamGamemode {
    public static final String GAMEMODE_NAME = "destroyobjectives";
    private static final int STRATEGY_TIME_DURATION_MS = 30 * 1000; // 30 seconds
    private static final int GAME_DURATION_S = 10 * 60; // 10 minutes
    private static final int COUNTDOWN_DURATION_MS = 5 * 1000; // 5 seconds
    
    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;

    public DestroyObjectivesGamemode(GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        super();
        this.gameStatistics = gameStatisticsService;
        this.gameTimer = gameTimer;

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
        Initialize();
    }

    @Override
    protected void Initialize() {
        super.Initialize();

        MainConfig.Mode mode = MainConfig.getInstance().getMode();
        if (mode != MainConfig.Mode.GAME) return;

        UnrealZaruba.LOGGER.info("Initializing DestroyObjectivesGamemode");

        AddPhase(new ConditionalPhase(
                PhaseId.PREPARATION,
                () -> {},
                this::AfterMapLoaded,
                new TimePassedCondition(9)
                .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Загрузка карты: " + integer)))
        ))
        .AddPhase(new ConditionalPhase(
            PhaseId.TEAM_SELECTION,
            this::StartTeamSelection,
            this::CompleteTeamSelection,
            new CombinedOrCondition(
                    new TeamsHaveEnoughPlayersCondition(TeamManager, 10, 10),
                    new TimePassedCondition(60)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Выбор команды закончится через " + integer + " секунд")))
        )))
        .AddPhase(new ConditionalPhase(
                PhaseId.STRATEGY_TIME,
                this::StartStrategyTime,
                this::CompleteStrategyTime,
                new TimePassedCondition(60)
                .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Игра начнётся через " + integer + " секунд")))
            )
        ).
        AddPhase(new ConditionalPhase(
                PhaseId.BATTLE,
                this::StartBattle,
                () -> { // On phase end
                    CompleteGame(server, TeamColor.BLUE);
                },
                new TimePassedCondition(GAME_DURATION_S)
            )
        );

        ProceedToPhaseForced(PhaseId.PREPARATION);
    }

    private void AfterMapLoaded() {
        LateInitialize();
        objectivesHandler.OnObjectivesCompleted(() -> {
            UnrealZaruba.LOGGER.warn("ALL OBJECTIVES COMPLETED");
            CompleteGame(server, TeamColor.RED);
        });
        WorldManager.TeleportAllPlayersTo(server, WorldManager.GAME_DIMENSION, new BlockPos(177, 75, -216));
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

            TeamManager.teleportToSpawnByPriority(player);
        }
        objectivesHandler.load();
        objectivesHandler.addRecipients(TeamManager.GetTeams());
    }

    public void StartStrategyTime() {
    }

    public void CompleteStrategyTime() {
        var players = server.getPlayerList().getPlayers();
        CharacterClassEquipper.equipTeamWithSelectedClasses(players);
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
    }

    public void StartBattle() {
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Starting battle");
        gameTimer.startCountDown(System.currentTimeMillis(), GAME_DURATION_S);
        TeamManager.PlayBattleSound();

        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            var team = TeamManager.GetPlayersTeam(serverPlayer);
            if (team == null) continue;
            
            TitleMessage.showTitle(serverPlayer, 
                startGameTexts.get(team.Color()).GetTitle(),
                startGameTexts.get(team.Color()).GetSubtitle());
        }

        UnrealZaruba.LOGGER.info("[UnrealZaruba] Triggering vehicle spawn blocks");

        var barriers = GamemodeDataManager
        .getHandler(this.getClass(), BarrierVolumesData.class)
        .getData().getBarriers();

        if (barriers != null) {
            barriers.forEach(barrier -> {
                BarrierRemovalTask.removeBarriersAsync(WorldManager.gameLevel, barrier);
            });
        }

        GamemodeDataManager
        .getHandler(this.getClass(), VehicleSpawnData.class)
        .triggerVehicleSpawns(server);
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
        TeamManager.teleportToSpawnByPriority(serverPlayer);
        serverPlayer.setGameMode(GameType.ADVENTURE);
    }

    private void MakePlayerSpectator(ServerPlayer serverPlayer, MinecraftServer server) {
        serverPlayer.setGameMode(GameType.SPECTATOR);
        var spawn = server.overworld().getSharedSpawnPos();
        serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
    }

    public void CompleteGame(MinecraftServer server, TeamColor wonTeam) {
        gameTimer.stop();
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
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
        }
//        Blyaaaa, я изначально думал, что один и тот же объект будет жить всё время, но когда появилось множество режимов, оно пошло нахуй, как же мне обидно
//        objectivesHandler.reset();
//        TeamManager.reset();
//        GetCurrentPhase().Clear();
        GamemodeManager.instance.CleanupCurrentGamemode();
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
