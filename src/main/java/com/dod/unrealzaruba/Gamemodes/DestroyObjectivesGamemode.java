package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Gamemodes.Barriers.BarrierVolumesData;
import com.dod.unrealzaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.unrealzaruba.Gamemodes.Objectives.CapturePointObjective;
import com.dod.unrealzaruba.Gamemodes.Objectives.DestroyObjective;
import com.dod.unrealzaruba.Gamemodes.Objectives.GameObjective;
import com.dod.unrealzaruba.Gamemodes.StartCondition.CombinedOrCondition;
import com.dod.unrealzaruba.Gamemodes.StartCondition.TeamsHaveEnoughPlayersCondition;
import com.dod.unrealzaruba.Gamemodes.StartCondition.TimePassedCondition;
import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.ClientboundObjectivesPacket;
import com.dod.unrealzaruba.NetworkPackets.DeleteRenderableZonesPacket;
import com.dod.unrealzaruba.NetworkPackets.RenderableZonesPacket;
import com.dod.unrealzaruba.Renderers.ColoredSquareZone;
import com.dod.unrealzaruba.NetworkPackets.ClientboundRemoveObjectivesPacket;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.Services.GameStatisticsService;
import com.dod.unrealzaruba.Gamemodes.GameText.StartGameText;
import com.dod.unrealzaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.Title.TitleMessage;
import com.dod.unrealzaruba.UI.Objectives.HudDestroyObjective;
import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.HudStringObjective;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.BarrierRemovalTask;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.utils.NBT;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;

import java.util.ArrayList;
import java.util.List;

public class DestroyObjectivesGamemode extends TeamGamemode {
    // Everyone is brought here when the map has loaded, before teams go to their spawns
    private static final BlockPos ARRIVAL_POINT = new BlockPos(177, 75, -216);
    public static final String GAMEMODE_NAME = "destroyobjectives";
    private static final int STRATEGY_TIME_DURATION_MS = 30 * 1000; // 30 seconds
    private static final int GAME_DURATION_S = 40 * 60; // 10 minutes
    private static final int COUNTDOWN_DURATION_MS = 5 * 1000; // 5 seconds
    
    private GameStatisticsService gameStatistics;
    private IGameTimer gameTimer;

    public DestroyObjectivesGamemode(GameStatisticsService gameStatisticsService, IGameTimer gameTimer) {
        super();
        this.gameStatistics = gameStatisticsService;
        this.gameTimer = gameTimer;

        TeamManager.Get(TeamColor.RED)
                .setOnObjectiveChangedNotification((objective, team) ->
                        ((TeamContext) team).SendMessage("Ваша §l§4команда§r атакует точку §b" + objective.GetName() + "§r. Гойда!"));
        TeamManager.Get(TeamColor.BLUE)
                .setOnObjectiveChangedNotification((objective, team) ->
                        ((TeamContext) team).SendMessage("Ваша точка §b" + objective.GetName() + "§r атакована §l§4противниками§r"));
        
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
                new CombinedOrCondition(
                        new TeamsHaveEnoughPlayersCondition(TeamManager, 10, 10),
                        new TimePassedCondition(60)
                            .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Игра начнётся через " + integer + " секунд")))
        )))
        .AddPhase(new ConditionalPhase(
                PhaseId.STRATEGY_TIME,
                this::StartStrategyTime,
                this::CompleteStrategyTime,
                new TimePassedCondition(60)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Этап подготовки: " + integer + " секунд")))
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

        ProceedToPhaseForced(PhaseId.MAP_INITIALIZATION);
    }

    @Override
    public java.util.Collection<BlockPos> GetPreloadPositions() {
        var positions = new java.util.ArrayList<>(super.GetPreloadPositions());
        positions.add(ARRIVAL_POINT);
        return positions;
    }

    private void AfterMapLoaded() {
        LateInitialize();
        objectivesHandler.OnObjectivesCompleted(() -> {
            UnrealZaruba.LOGGER.warn("ALL OBJECTIVES COMPLETED");
            CompleteGame(server, TeamColor.RED);
        });
        WorldManager.TeleportAllPlayersTo(server, WorldManager.GAME_DIMENSION, ARRIVAL_POINT);
        // The respawn points to capture are on the map from the start, so players can pick where to spawn
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SendRespawnObjectives(player);
        }
    }

    /** Respawn points that can be captured: their zones and state. They advance no objective. */
    private void SendRespawnObjectives(ServerPlayer player) {
        List<ColoredSquareZone> zones = new ArrayList<>();
        ArrayList<HudObjective> hud = new ArrayList<>();
        for (GameObjective objective : objectivesHandler.getObjectives()) {
            if (objective instanceof CapturePointObjective point && point.IsRespawnOnly()) {
                var owner = (TeamContext) point.GetOwner();
                zones.add(new ColoredSquareZone(point.GetCaptureAreaAABB(), owner == null ? 0xDDDDEE : owner.GetIntColor()));
                hud.add(point.ToHudObjective());
            }
        }
        if (zones.isEmpty()) return;
        PacketDistributor.sendToPlayer(player, new RenderableZonesPacket(zones));
        PacketDistributor.sendToPlayer(player, new ClientboundObjectivesPacket(hud));
    }

    private void SendObjectivesDataToClients() {
        ArrayList<HudObjective> hudObjectives = new ArrayList<>();
        PrepareObjectiveDataToSendToClient(hudObjectives);
        for(var player : server.getPlayerList().getPlayers()) {
            if (PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext tPlayerContext) {
                if (tPlayerContext.Team() != null) {
                    switch (tPlayerContext.Team().Color()) {
                        case RED -> hudObjectives.add(new HudStringObjective("Уничтожьте все цели"));
                        case BLUE -> hudObjectives.add(new HudStringObjective("Защищайте цели до конца таймера"));
                    }
                }
            }
            PacketDistributor.sendToPlayer(player, new ClientboundObjectivesPacket(hudObjectives));
        }
    }

    public void StartTeamSelection() {
        server.getPlayerList().getPlayers().forEach(player -> {
            player.setGameMode(GameType.ADVENTURE);
            player.sendSystemMessage(Component.translatable("hud.unrealzaruba.respawn.pick_before_start"));
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
    }

    public void StartStrategyTime() {
        SendObjectivesDataToClients();
    }

    public void CompleteStrategyTime() {
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);
        server.setDifficulty(Difficulty.NORMAL, true);
    }

    public void StartBattle() {
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

        ArrayList<HudObjective> hudObjectives = new ArrayList<>();
        PrepareObjectiveDataToSendToClient(hudObjectives);
        if (PlayerContext.Get(serverPlayer.getUUID()) instanceof TeamPlayerContext tPlayerContext) { // TODO: blyat, again ebuchi duplication
            var team = tPlayerContext.Team();
            if (team != null) {
                switch (team.Color()) {
                    case RED -> hudObjectives.add(new HudStringObjective("Уничтожьте все цели"));
                    case BLUE -> hudObjectives.add(new HudStringObjective("Защищайте цели до конца таймера"));
                }
            }
        }
        PacketDistributor.sendToPlayer(serverPlayer, new ClientboundObjectivesPacket(hudObjectives));
        SendRespawnObjectives(serverPlayer);

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
        TeamManager.teleportToChosenOrPriority(serverPlayer);
        serverPlayer.setGameMode(GameType.ADVENTURE);
    }

    private void MakePlayerSpectator(ServerPlayer serverPlayer, MinecraftServer server) {
        serverPlayer.setGameMode(GameType.SPECTATOR);
        var spawn = server.overworld().getSharedSpawnPos();
        serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
    }

    public void CompleteGame(MinecraftServer server, TeamColor wonTeam) {
        gameTimer.stop();
        objectivesHandler.clear();
        ShowEndText(server, wonTeam);
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.SPECTATOR);
            player.getInventory().clearContent();
            PacketDistributor.sendToPlayer(player, new DeleteRenderableZonesPacket(
                    objectivesHandler.getObjectives().stream()
                            .filter(obj -> obj instanceof CapturePointObjective)
                            .map(obj -> ((CapturePointObjective) obj).GetCaptureAreaAABB().hashCode() + ColoredSquareZone.RenderableIDShift)
                            .toList()));
        }
        if (gameStatistics != null) gameStatistics.SendGameData(this.getClass().getSimpleName(), TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
        TimerManager.createRealTimeTimer(3 * 1000 /*10s*/, () -> CompleteGameDelayed(server), null);
        // scheduledExecutorService.schedule(() -> CompleteGameDelayed(server), 10, TimeUnit.SECONDS); // Vot tak ne delat'
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        WorldManager.TeleportAllPlayersTo(server, WorldManager.LOBBY_DIMENSION);
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, new ClientboundRemoveObjectivesPacket());
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
            if (PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext tPlayerContext) {
                tPlayerContext.DeselectRespawnPoint();
            }
        }

//        Blyaaaa, я изначально думал, что один и тот же объект будет жить всё время, но когда появилось множество режимов, оно пошло нахуй, как же мне обидно
//        TeamManager.reset();
//        objectivesHandler.reset();
//        GetCurrentPhase().Clear();
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

    private void PrepareObjectiveDataToSendToClient(ArrayList<HudObjective> hudObjectives) {

        for (GameObjective objective : objectivesHandler.getObjectives()) {
            if (objective instanceof DestroyObjective destructibleObjective) {
                var obj = new HudDestroyObjective(
                        destructibleObjective.GetRuntimeId(),
                        destructibleObjective.GetName(),
                        destructibleObjective.GetProgress(),
                        destructibleObjective.GetPosition()
                );
                hudObjectives.add(obj);
            } else if (objective instanceof CapturePointObjective point && point.IsRespawnOnly()) {
                hudObjectives.add(point.ToHudObjective());
            }
        }
    }
}
