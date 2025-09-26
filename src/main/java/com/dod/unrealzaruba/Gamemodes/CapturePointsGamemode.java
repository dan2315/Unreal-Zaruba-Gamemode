package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.Gamemodes.Barriers.BarrierVolumesData;
import com.dod.unrealzaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.unrealzaruba.Gamemodes.GamePhases.GamePhase;
import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.unrealzaruba.Gamemodes.GameText.StartGameText;
import com.dod.unrealzaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.Objectives.CapturePointObjective;
import com.dod.unrealzaruba.Gamemodes.Objectives.GameObjective;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectiveOwner;
import com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay.NetworkedTopHud;
import com.dod.unrealzaruba.Gamemodes.Objectives.ScorePointsObjective;
import com.dod.unrealzaruba.Gamemodes.StartCondition.*;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.unrealzaruba.NetworkPackets.ClientboundObjectivesPacket;
import com.dod.unrealzaruba.NetworkPackets.ClientboundRemoveObjectivesPacket;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.NetworkPackets.RenderableZonesPacket;
import com.dod.unrealzaruba.Renderers.ColoredSquareZone;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.Title.TitleMessage;
import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.HudStringObjective;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.BarrierRemovalTask;
import com.dod.unrealzaruba.utils.NBT;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CapturePointsGamemode extends TeamGamemode {
    public static final String GAMEMODE_NAME = "capturepoints";
    private static final short REQUIRED_POINTS_TO_WIN = 200;
    private final ScorePointsObjective scorePointsObjectiveRed;
    private final ScorePointsObjective scorePointsObjectiveBlue;

    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;

    public CapturePointsGamemode(GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        super();
        this.gameStatistics = gameStatisticsService;
        this.gameTimer = gameTimer;

        TeamManager.GetTeams().forEach((teamColor, teamContext) -> {
            teamContext.setOnObjectiveChangedNotification((objective, team) -> {
                ((CapturePointObjective) objective).SendUpdateHudElement();
            });
        });

        startGameTexts.put(TeamColor.RED, new StartGameText(
                "§c Игра началась, в бой!",
                "Наберите 200 очков контроля точек"));
        startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Наберите 200 очков контроля точек"));

        scorePointsObjectiveRed = new ScorePointsObjective(0, REQUIRED_POINTS_TO_WIN);
        var topHudLeft = new NetworkedTopHud((byte) 0, REQUIRED_POINTS_TO_WIN);
        topHudLeft.updateProgress(0);
        scorePointsObjectiveRed.setProgressDisplay(topHudLeft);
        scorePointsObjectiveRed.SubscribeOnCompleted(objective -> CompleteGame(server, TeamColor.RED));

        scorePointsObjectiveBlue = new ScorePointsObjective(0, REQUIRED_POINTS_TO_WIN);
        var topHudRight = new NetworkedTopHud((byte) 1, REQUIRED_POINTS_TO_WIN);
        topHudRight.updateProgress(0);
        scorePointsObjectiveBlue.setProgressDisplay(topHudRight);
        scorePointsObjectiveBlue.SubscribeOnCompleted(objective -> CompleteGame(server, TeamColor.BLUE));

        ServerLifecycleHooks.getCurrentServer().setDifficulty(Difficulty.PEACEFUL, true);
        Initialize();
    }

    @Override
    protected void Initialize() {
        super.Initialize();

        MainConfig.Mode mode = MainConfig.getInstance().getMode();
        if (mode != MainConfig.Mode.GAME) return;

        AddPhase(new ConditionalPhase(
                PhaseId.MAP_INITIALIZATION,
                () -> {},
                this::AfterMapLoaded,
                new TimePassedCondition(9)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Загрузка карты: " + integer)))
        ))
        .AddPhase(new ConditionalPhase(
                PhaseId.PREPARATION,
                this::StartTeamSelection,
                this::CompleteTeamSelection,
                new TimePassedCondition(60)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Игра начнётся через " + integer + " секунд")))
        ))
        .AddPhase(new ConditionalPhase(
                PhaseId.STRATEGY_TIME,
                this::StartStrategyTime,
                this::CompleteStrategyTime,
                new TimePassedCondition(60)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Этап подготовки: " + integer + " секунд")))
        )).
        AddPhase(new GamePhase(
                PhaseId.BATTLE,
                this::StartBattle,
                () -> { // On phase end
                    CompleteGame(server, TeamColor.BLUE);
                }
        ));

        ProceedToPhaseForced(PhaseId.MAP_INITIALIZATION);
    }

    private void AfterMapLoaded() {
        LateInitialize();

        objectivesHandler.addObjective(scorePointsObjectiveRed);
        objectivesHandler.addObjective(scorePointsObjectiveBlue);
        UpdateScorePointObjectivesSpeed();

        List<ColoredSquareZone> zones = new ArrayList<>();
        ArrayList<HudObjective> hudObjectives = new ArrayList<>();
        PrepareObjectiveDataToSendToClient(hudObjectives, zones);

        for (GameObjective objective : objectivesHandler.getObjectives().stream().filter(obj -> obj instanceof CapturePointObjective).toList()) {
            objective.SubscribeOnCompleted(completedObjective -> {
                if (completedObjective instanceof CapturePointObjective cpObjective) {
                    var team = (TeamContext) cpObjective.GetOwner();
                }
                UpdateScorePointObjectivesSpeed();
            });
        }

        WorldManager.TeleportAllPlayersTo(server, WorldManager.GAME_DIMENSION, new BlockPos(177, 75, -216));
        for(var player : server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RenderableZonesPacket(zones));
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundObjectivesPacket(hudObjectives));
        }
    }

    private void PrepareObjectiveDataToSendToClient(ArrayList<HudObjective> hudObjectives, List<ColoredSquareZone> zones) {
        hudObjectives.add(new HudStringObjective("Наберите 200 очков, чтобы выиграть"));
        for (GameObjective objective : objectivesHandler.getObjectives()) {
            if (objective instanceof CapturePointObjective capturePointObjective) {
                var ownerTeam = (TeamContext) capturePointObjective.GetOwner();
                int ownerColor = ownerTeam == null ? 0xDDDDEE : ownerTeam.GetIntColor();
                var beingCapturedBy = (TeamContext) capturePointObjective.GetBeingCapturedBy();
                int capturedByColor = beingCapturedBy == null ?  0xDDDDEE : beingCapturedBy.GetIntColor();
                zones.add(new ColoredSquareZone(capturePointObjective.GetCaptureAreaAABB(), ownerColor));

                hudObjectives.add(new HudCapturePointObjective(
                        capturePointObjective.GetRuntimeId(),
                        capturePointObjective.GetName(),
                        ownerColor,
                        capturedByColor,
                        capturePointObjective.GetProgress(),
                        capturePointObjective.GetPosition()));
            }
        }
    }

    private void UpdateScorePointObjectivesSpeed() {
        int redPoints = 0;
        int bluePoints = 0;
        for (var obj : objectivesHandler.getObjectives()) {
            if (obj instanceof CapturePointObjective cpObj) {
                if (cpObj.GetOwner() == null) continue;
                redPoints += cpObj.GetOwner().equals(TeamManager.Get(TeamColor.RED)) ? 1 : 0;
                bluePoints += cpObj.GetOwner().equals(TeamManager.Get(TeamColor.BLUE)) ? 1 : 0;
            }
        }

        scorePointsObjectiveRed.SetIncrementationSpeed((float) (Math.max(0, redPoints - 1)) / 10);
        scorePointsObjectiveBlue.SetIncrementationSpeed((float) (Math.max(0, bluePoints - 1)) / 10);
    }

    // -- DUPLICATION TODO: Think how to reduce it
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
            List<ColoredSquareZone> zones = new ArrayList<>();
            ArrayList<HudObjective> hudObjectives = new ArrayList<>();
            PrepareObjectiveDataToSendToClient(hudObjectives, zones);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new RenderableZonesPacket(zones));
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ClientboundObjectivesPacket(hudObjectives));
        }
    }

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
    // -- DUPLICATION

    public boolean CheckIfAllPointsCaptured() {
        ObjectiveOwner sameOwner = null;
        for(var objective : objectivesHandler.getObjectives()) {
            if (objective instanceof CapturePointObjective capturePointObjective) {
                ObjectiveOwner owner = capturePointObjective.GetOwner();
                if (sameOwner != null && !Objects.equals(sameOwner, owner)) {
                    return false;
                }
                sameOwner = owner;
            }
        }
        return sameOwner != null;
    }

    public void CompleteGame(MinecraftServer server, TeamColor wonTeam) {
        UnrealZaruba.LOGGER.info("COMPLETING GAME");
        gameTimer.stop();
        ShowEndText(server, wonTeam);
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.SPECTATOR);
            player.getInventory().clearContent();
        }
        if (gameStatistics != null) gameStatistics.SendGameData(this.getClass().getSimpleName(), TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
        TimerManager.createRealTimeTimer(3 * 1000 /*10s*/, () -> CompleteGameDelayed(server), null);
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION);
        for (ServerPlayer player : players) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new ClientboundRemoveObjectivesPacket());
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
        }
        GamemodeManager.instance.CleanupCurrentGamemode();
    }

    public void ShowEndText(MinecraftServer server, TeamColor wonTeam) {
        String colorCode = wonTeam.getColorCode();
        Component titleText = Component.literal("Команда ")
                .append(Component.literal(wonTeam.getDisplayName()).withStyle(ChatFormatting.valueOf(colorCode)))
                .append(Component.literal(" победила"));
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
        CharacterClassEquipper.equipTeamWithSelectedClasses(players);
        objectivesHandler.addRecipients(TeamManager.GetTeams());
    }

    public void StartStrategyTime() {
    }

    public void CompleteStrategyTime() {
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
    }

    public void StartBattle() {
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
}
