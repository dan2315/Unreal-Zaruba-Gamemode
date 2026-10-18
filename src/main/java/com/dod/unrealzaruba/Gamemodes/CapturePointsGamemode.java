package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Commands.CommandHandlers.RemoveVehicleSpawnCommand;
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
import com.dod.unrealzaruba.NetworkPackets.*;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CapturePointsGamemode extends TeamGamemode {
    // Everyone is brought here when the map has loaded, before teams go to their spawns
    private static final BlockPos ARRIVAL_POINT = new BlockPos(177, 75, -216);
    public static final String GAMEMODE_NAME = "capturepoints";
    /** From scoring.json of the map. */
    private final ScoringData.Config scoring;
    private final short requiredPointsToWin;
    private final ScorePointsObjective scorePointsObjectiveRed;
    private final ScorePointsObjective scorePointsObjectiveBlue;

    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;

    public CapturePointsGamemode(GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        super();
        this.gameStatistics = gameStatisticsService;
        this.gameTimer = gameTimer;
        this.scoring = ScoringData.of(this.getClass());
        this.requiredPointsToWin = (short) Math.min(Short.MAX_VALUE, scoring.pointsToWin);

        TeamManager.GetTeams().forEach((teamColor, teamContext) -> {
            teamContext.setOnObjectiveChangedNotification((objective, team) -> {
                ((CapturePointObjective) objective).SendUpdateHudElement();
            });
        });

        startGameTexts.put(TeamColor.RED, new StartGameText(
                "§c Игра началась, в бой!",
                "Наберите " + scoring.pointsToWin + " очков контроля точек"));
        startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Наберите " + scoring.pointsToWin + " очков контроля точек"));

        scorePointsObjectiveRed = new ScorePointsObjective(0, requiredPointsToWin);
        var topHudLeft = new NetworkedTopHud((byte) 0, requiredPointsToWin);
        topHudLeft.updateProgress(0);
        scorePointsObjectiveRed.setProgressDisplay(topHudLeft);
        scorePointsObjectiveRed.SubscribeOnCompleted(objective -> CompleteGame(server, TeamColor.RED));

        scorePointsObjectiveBlue = new ScorePointsObjective(0, requiredPointsToWin);
        var topHudRight = new NetworkedTopHud((byte) 1, requiredPointsToWin);
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
                new TimePassedCondition(MIN_MAP_LOADING_SECONDS)
                    .WaitFor(WorldManager::isGameWorldReady)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server,
                            Component.literal("Загрузка карты: " + Math.round(WorldManager.preloadProgress() * 100) + "%")))
        ))
        .AddPhase(new ConditionalPhase(
                PhaseId.PREPARATION,
                this::StartTeamSelection,
                this::CompleteTeamSelection,
                new TimePassedCondition(scoring.teamSelectionSeconds)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Игра начнётся через " + integer + " секунд")))
        ))
        .AddPhase(new GamePhase(
                PhaseId.BATTLE,
                this::StartBattle,
                () -> { // On phase end
                    CompleteGame(server, TeamColor.BLUE);
                }
        ));

        ProceedToPhaseForced(PhaseId.MAP_INITIALIZATION);
    }

    private final java.util.Set<String> announcedScores = new java.util.HashSet<>();
    private int announceTicks;

    @Override
    public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        super.onServerTick(event);
        if (IsBattleActive() && ++announceTicks % 20 == 0) announceScoreMarks();
    }

    /** Everybody hears when a team reaches a mark of the score it needs (see scoring.json), once per team and mark. */
    private void announceScoreMarks() {
        for (TeamColor color : new TeamColor[]{TeamColor.RED, TeamColor.BLUE}) {
            Integer score = GetTeamScore(color);
            if (score == null) continue;
            for (int percent : scoring.announceScoreAtPercent) {
                if (score * 100 >= percent * requiredPointsToWin && announcedScores.add(color.name() + ":" + percent)) {
                    com.dod.unrealzaruba.Feed.Feed.event(server, com.dod.unrealzaruba.Feed.FeedKind.SCORE, com.dod.unrealzaruba.Feed.Feed.colorOf(color), "feed.score_mark",
                            color.getDisplayName(), String.valueOf(score), String.valueOf(requiredPointsToWin));
                    com.dod.unrealzaruba.SoundHandler.VoiceLine ally = percent >= 90 ? com.dod.unrealzaruba.SoundHandler.VoiceLine.SCORE_90_ALLY : percent >= 75 ? com.dod.unrealzaruba.SoundHandler.VoiceLine.SCORE_75_ALLY : percent >= 50 ? com.dod.unrealzaruba.SoundHandler.VoiceLine.SCORE_50_ALLY : null;
                    com.dod.unrealzaruba.SoundHandler.VoiceLine enemy = percent >= 90 ? com.dod.unrealzaruba.SoundHandler.VoiceLine.SCORE_90_ENEMY : percent >= 75 ? com.dod.unrealzaruba.SoundHandler.VoiceLine.SCORE_75_ENEMY : percent >= 50 ? com.dod.unrealzaruba.SoundHandler.VoiceLine.SCORE_50_ENEMY : null;
                    com.dod.unrealzaruba.SoundHandler.Voice.playSplit(server, color, ally, enemy);
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, percent >= 90 ? 0.6f : 1.0f);
                    }
                }
            }
        }
    }

    @Override
    public Integer GetTeamScore(TeamColor team) {
        return switch (team) {
            case RED -> Math.round(requiredPointsToWin * scorePointsObjectiveRed.GetProgress());
            case BLUE -> Math.round(requiredPointsToWin * scorePointsObjectiveBlue.GetProgress());
            default -> null;
        };
    }

    @Override
    public java.util.Collection<BlockPos> GetPreloadPositions() {
        var positions = new java.util.ArrayList<>(super.GetPreloadPositions());
        positions.add(ARRIVAL_POINT);
        return positions;
    }

    private void AfterMapLoaded() {
        LateInitialize();

        objectivesHandler.addObjective(scorePointsObjectiveRed);
        objectivesHandler.addObjective(scorePointsObjectiveBlue);
        UpdateScorePointObjectivesSpeed();


        for (GameObjective objective : objectivesHandler.getObjectives().stream().filter(obj -> obj instanceof CapturePointObjective).toList()) {
            objective.SubscribeOnCompleted(completedObjective -> {
                if (completedObjective instanceof CapturePointObjective cpObjective) {
                    var team = (TeamContext) cpObjective.GetOwner();
                }
                UpdateScorePointObjectivesSpeed();
            });
        }

        com.dod.unrealzaruba.WorldManager.MapMobs.sweep(WorldManager.gameLevel);
        WorldManager.TeleportAllPlayersTo(server, WorldManager.GAME_DIMENSION, ARRIVAL_POINT);

        List<ColoredSquareZone> zones = new ArrayList<>();
        ArrayList<HudObjective> hudObjectives = new ArrayList<>();
        PrepareObjectiveDataToSendToClient(hudObjectives, zones);
        for(var player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new RenderableZonesPacket(zones));
            PacketDistributor.sendToPlayer(player, new ClientboundObjectivesPacket(hudObjectives));
        }
    }

    private void PrepareObjectiveDataToSendToClient(ArrayList<HudObjective> hudObjectives, List<ColoredSquareZone> zones) {
        hudObjectives.add(new HudStringObjective("Наберите " + scoring.pointsToWin + " очков, чтобы выиграть"));
        for (GameObjective objective : objectivesHandler.getObjectives()) {
            if (objective instanceof CapturePointObjective capturePointObjective) {
                var ownerTeam = (TeamContext) capturePointObjective.GetOwner();
                int ownerColor = ownerTeam == null ? 0xDDDDEE : ownerTeam.GetIntColor();
                zones.add(new ColoredSquareZone(capturePointObjective.GetCaptureAreaAABB(), ownerColor));

                hudObjectives.add(capturePointObjective.ToHudObjective());
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

        // Every team holds a few points from the start; only the ones above that number earn score
        scorePointsObjectiveRed.SetIncrementationSpeed(Math.max(0, redPoints - scoring.defaultPointsPerTeam) * scoring.scorePerExtraPoint);
        scorePointsObjectiveBlue.SetIncrementationSpeed(Math.max(0, bluePoints - scoring.defaultPointsPerTeam) * scoring.scorePerExtraPoint);
    }

    // -- DUPLICATION TODO: Think how to reduce it
    public void HandleConnectedPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();

        var isInTeam = TeamManager.IsInTeam(serverPlayer);
        var isDead = NBT.readEntityTag(serverPlayer, "isPlayerDead");

        List<ColoredSquareZone> zones = new ArrayList<>();
        ArrayList<HudObjective> hudObjectives = new ArrayList<>();
        PrepareObjectiveDataToSendToClient(hudObjectives, zones);
        PacketDistributor.sendToPlayer(serverPlayer, new RenderableZonesPacket(zones));
        PacketDistributor.sendToPlayer(serverPlayer, new ClientboundObjectivesPacket(hudObjectives));
        // Scores are only sent when they change, so a player joining during a stalemate needs them now
        PacketDistributor.sendToPlayer(serverPlayer, new ClientboundUpdateTopScorePacket((byte) 0,
                (short) (requiredPointsToWin * scorePointsObjectiveRed.GetProgress()), requiredPointsToWin));
        PacketDistributor.sendToPlayer(serverPlayer, new ClientboundUpdateTopScorePacket((byte) 1,
                (short) (requiredPointsToWin * scorePointsObjectiveBlue.GetProgress()), requiredPointsToWin));

        if (gameTimer != null) gameTimer.syncTo(serverPlayer);

        if (server == null)
            return;

        if (GetCurrentPhaseId() == PhaseId.TEAM_SELECTION) {
            TeleportToLobby(serverPlayer, server);
            return;
        }
        // Joining while the teams are being picked or in the middle of the match: to the zone where a team is chosen
        if ((GetCurrentPhaseId() == PhaseId.PREPARATION || GetCurrentPhaseId() == PhaseId.BATTLE) && !isInTeam) {
            SendToTeamSelection(serverPlayer);
            return;
        }
        if (GetCurrentPhaseId() == PhaseId.BATTLE && isDead == 1) {
            ReturnToTeamSpawn(serverPlayer);
        }
    }

    /** The arrival zone of the game world, where the team blocks are: a team chosen there takes the player into the battle. */
    private void SendToTeamSelection(ServerPlayer serverPlayer) {
        serverPlayer.setGameMode(GameType.ADVENTURE);
        WorldManager.teleportPlayerToDimension(serverPlayer, WorldManager.GAME_DIMENSION, ARRIVAL_POINT);
        serverPlayer.sendSystemMessage(Component.translatable("hud.unrealzaruba.late_join.pick_team"));
        MatchRules.tell(serverPlayer);
    }

    private void TeleportToLobby(ServerPlayer serverPlayer, MinecraftServer server) {
        WorldManager.teleportPlayerToDimension(serverPlayer, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
    }

    private void ReturnToTeamSpawn(ServerPlayer serverPlayer) {
        TeamManager.teleportToChosenOrPriority(serverPlayer);
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
            if (PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext tPlayerContext) {
                tPlayerContext.DeselectRespawnPoint();
            }
            PacketDistributor.sendToPlayer(player, new DeleteRenderableZonesPacket(
                            objectivesHandler.getObjectives().stream()
                                    .filter(obj -> obj instanceof CapturePointObjective)
                                    .map(obj -> ((CapturePointObjective) obj).GetCaptureAreaAABB().hashCode() + ColoredSquareZone.RenderableIDShift)
                                    .toList()
                    )
            );
        }
        if (gameStatistics != null) gameStatistics.SendGameData(this.getClass().getSimpleName(), TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
        TimerManager.createRealTimeTimer(3 * 1000 /*10s*/, () -> CompleteGameDelayed(server), null);
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION);
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, new ClientboundRemoveObjectivesPacket());
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
        }
        GamemodeManager.instance.CleanupCurrentGamemode();
    }

    public void ShowEndText(MinecraftServer server, TeamColor wonTeam) {
        com.dod.unrealzaruba.Feed.Feed.event(server, com.dod.unrealzaruba.Feed.FeedKind.MATCH, com.dod.unrealzaruba.Feed.Feed.colorOf(wonTeam), "feed.match_won", wonTeam.getDisplayName());
        com.dod.unrealzaruba.SoundHandler.Voice.playSplit(server, wonTeam, com.dod.unrealzaruba.SoundHandler.VoiceLine.MATCH_WIN, com.dod.unrealzaruba.SoundHandler.VoiceLine.MATCH_LOSE);
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
            player.sendSystemMessage(Component.translatable("hud.unrealzaruba.respawn.pick_before_start"));
            MatchRules.tell(player);
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

            TeamManager.teleportToChosenOrPriority(player);
        }
        CharacterClassEquipper.equipTeamWithSelectedClasses(players);
        objectivesHandler.addRecipients(TeamManager.GetTeams());

        // No waiting on the bases: the battle starts as soon as the teams are set
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
    }

    public void StartBattle() {
        com.dod.unrealzaruba.Feed.Feed.event(server, com.dod.unrealzaruba.Feed.FeedKind.MATCH, 0xFFFFFF, "feed.match_started");
        com.dod.unrealzaruba.SoundHandler.Voice.playAll(server, com.dod.unrealzaruba.SoundHandler.VoiceLine.MATCH_START);
        com.dod.unrealzaruba.WorldManager.MapMobs.sweep(WorldManager.gameLevel);
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
