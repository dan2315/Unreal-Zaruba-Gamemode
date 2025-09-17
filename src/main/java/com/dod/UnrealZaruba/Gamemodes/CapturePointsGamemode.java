package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Config.MainConfig;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.GamePhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.GameTimer.IGameTimer;
import com.dod.UnrealZaruba.Gamemodes.Objectives.CapturePointObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectiveOwner;
import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.RespawnPoint;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.*;
import com.dod.UnrealZaruba.NetworkPackets.ClientboundObjectivesPacket;
import com.dod.UnrealZaruba.NetworkPackets.ClientboundRemoveObjectivesPacket;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.RenderableZonesPacket;
import com.dod.UnrealZaruba.Renderers.ColoredSquareZone;
import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.UnrealZaruba.UI.Objectives.HudObjective;
import com.dod.UnrealZaruba.UI.Objectives.HudStringObjective;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.NBT;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
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

import static com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective.LastRuntimeId;

public class CapturePointsGamemode extends TeamGamemode {
    public static final String GAMEMODE_NAME = "capturepoints";

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
                "Необходимо уничтожить вражескую базу"));
        startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Продержитесь до конца раунда"));

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
                PhaseId.STRATEGY_TIME,
                () -> {},
                () -> {},
                new TimePassedCondition(60)
                    .OnEverySecond(integer -> TitleMessage.sendActionbarToEveryone(server, Component.literal("Игра начнётся через " + integer + " секунд")))
        )).
        AddPhase(new GamePhase(
                PhaseId.BATTLE,
                () -> {},
                () -> { // On phase end
                    CompleteGame(server, TeamColor.BLUE);
                }
        ));

        ProceedToPhaseForced(PhaseId.PREPARATION);
    }

    private void AfterMapLoaded() {
        LateInitialize();

        List<ColoredSquareZone> zones = new ArrayList<>();
        ArrayList<HudObjective> hudObjectives = new ArrayList<>();
        hudObjectives.add(new HudStringObjective("Захватите все точки"));
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
                        capturePointObjective.GetProgress()));
            }
            for(var player : server.getPlayerList().getPlayers()) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RenderableZonesPacket(zones));
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundObjectivesPacket(hudObjectives));
            }

            objective.SubscribeOnCompleted(completedObjective -> {
                if (completedObjective instanceof CapturePointObjective cpObjective) {
                    var team = (TeamContext) cpObjective.GetOwner();
                    team.AddRespawnPoint(new RespawnPoint(cpObjective.getPosition(), cpObjective.GetName(),1));
                }

                if (CheckIfAllPointsCaptured()) {
                    EndGame();
                }
            });
        }
        objectivesHandler.OnObjectivesCompleted(() -> {
            UnrealZaruba.LOGGER.warn("ALL OBJECTIVES COMPLETED");
            CompleteGame(server, TeamColor.RED);
        });
        WorldManager.TeleportAllPlayersTo(server, WorldManager.GAME_DIMENSION, new BlockPos(177, 75, -216));
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
}
