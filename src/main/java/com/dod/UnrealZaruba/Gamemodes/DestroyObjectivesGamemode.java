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
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.OpenScreenPacket;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.dod.UnrealZaruba.WorldManager.SimpleWorldManager;
import com.dod.UnrealZaruba.Utils.NBT;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.StartCondition;
import com.dod.UnrealZaruba.Gamemodes.StartCondition.SustainedPlayerCountCondition;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;

public class DestroyObjectivesGamemode extends TeamGamemode {
    private static final int COMMANDER_VOTING_DURATION_MS = 3 * 60 * 1000; // 3 minutes
    private static final int PREPARATION_DURATION_MS = 7 * 60 * 1000; // 7 minutes
    private static final int GAME_DURATION_MS = 50 * 60 * 1000; // 50 minutes
    private static final int COUNTDOWN_DURATION_MS = 10 * 1000; // 10 seconds
    
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private GameStatisticsService leaderboardService;
    private Scoreboard scoreboard;
    private Objective minecraftObjective;
    private GameTimer gameTimer;

    private DestructibleObjectivesHandler objectivesHandler;
    private StartCondition startCondition;

    public DestroyObjectivesGamemode(MinecraftServer server, GameStatisticsService leaderboardService, GameTimer gameTimer) {
        this.leaderboardService = leaderboardService;
        this.gameTimer = gameTimer;
        currentGamemode = this;
        scoreboard = ServerLifecycleHooks.getCurrentServer().getScoreboard();
        //TODO: minecraftObjective = scoreboard.getObjective(ScoreboardManager.OBJECTIVE_NAME);

        TeamManager teamManager = new TeamManager();
        teamManager.Initialize();
        SetTeamManager(teamManager);

        TeamManager.Get(TeamColor.RED).setNotificationMessage(objective -> "Ваша §l§4команда§r атакует точку §b" + objective.GetName() + "§r. Гойда!");

        TeamManager.Get(TeamColor.BLUE).setNotificationMessage(objective -> "Ваша точка §b" + objective.GetName() + "§r атакована §l§4противниками§r");
        
        startGameTexts.put(TeamColor.RED, new StartGameText(
                "§c Игра началась, в бой!",
                "Необходимо уничтожить 3 цели"));
        startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Продержитесь 50 минут"));
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
        startCondition = new SustainedPlayerCountCondition(TeamManager, 5, 10); // 5 players in each team for 10 sec
        startCondition.SetOnConditionMet(this::StartGame);
    }

    @Override
    public void onServerTick(TickEvent.ServerTickEvent serverTickEvent) {
        startCondition.Update();
        objectivesHandler.updateObjectives();
    }

    @Override
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        objectivesHandler.onPlayerTick(event);
    }

    public void StartGame() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                UnrealZaruba.LOGGER.error("Cannot start game: Server is null");
                return;
            }
            
            UnrealZaruba.LOGGER.info("Starting game with " + 
                                   server.getPlayerList().getPlayerCount() + " players");
            
            gameTimer.setupScoreboard();
            
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UnrealZaruba.LOGGER.info("Teleporting player: " + player.getName().getString());
                TeamManager.teleportToSpawn(player);
            }
            
            StartVotingForCommander(server);
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Failed to start game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        gameTimer.setupScoreboard();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UnrealZaruba.LOGGER.info("Teleporting player: " + player.getName().getString());
            TeamManager.teleportToSpawn(player);
        }
        
        return StartVotingForCommander(context);
    }

    private void StartVotingForCommander(MinecraftServer server) {
        gameStage = GameStage.CommanderVoting;
        HashMap<TeamColor, TeamContext> teams = TeamManager.GetTeams();
        
        for (var team : teams.entrySet()) {
            for (var player : team.getValue().Members()) {
                var serverPlayer = server.getPlayerList().getPlayer(player);
                if (serverPlayer == null) continue;

                TitleMessage.showTitle(serverPlayer, Component.literal("§6Выбор командира"),
                        Component.literal("Для того, чтобы проголосовать используй"));

                Map<String, Object> data = new HashMap<>();
                data.put("teammates", team.getValue().Members());
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new OpenScreenPacket(1, data));
            } 
        }
        
        TimerManager.createRealTimeTimer(
            COMMANDER_VOTING_DURATION_MS,
            () -> handleVotingCompleted(teams, server),
            ticks -> updateVotingTimer(ticks)
        );
    }
    
    public int StartVotingForCommander(CommandContext<CommandSourceStack> context) {
        StartVotingForCommander(context.getSource().getServer());
        return 1;
    }

    private void handleVotingCompleted(HashMap<TeamColor, TeamContext> teams, MinecraftServer server) {
        try {
            for (Map.Entry<TeamColor, TeamContext> teamEntry : teams.entrySet()) {
                TeamContext team = teamEntry.getValue();
                if (team.VoteList().isEmpty()) {
                    UnrealZaruba.LOGGER.warn("Vote list is empty");
                    continue;
                }
                
                int listSize = team.VoteList().size();
                Player most_voted_player = server.getPlayerList().getPlayer(team.VoteList().get(0));
                StringBuilder message = new StringBuilder(
                    "===========================\nТоп 5 голосования:\n");
                
                for (int i = 0; i < Math.min(5, listSize); i++) {
                    ServerPlayer topPlayer = server.getPlayerList().getPlayer(team.VoteList().get(i));
                    if (topPlayer != null) {
                        Integer voteCount = PlayerContext.Get(topPlayer.getUUID()).Votes();
                        message.append(i + 1 +". " + topPlayer.getName().getString() + ": " + voteCount + "\n");
                    }
                }
                
                message.append("===========================");
                teamEntry.getValue().setCommander(server, most_voted_player);
                team.SendMessage(server, message.toString());
            }
            
            StartPreparation(server);
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Error handling voting completion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void StartPreparation(MinecraftServer server) {
        Utils.SetGamemodeAllExcludeOP(server.getPlayerList(), GameType.ADVENTURE);
        gameStage = GameStage.StrategyTime;

        // Show preparation phase message to all players
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            TitleMessage.showTitle(serverPlayer, Component.literal("§6Стадия подготовки"),
                    Component.literal("Коммандиром стал " +
                            TeamManager.GetPlayersTeam(serverPlayer).CommanderName() + ", он определяет стратегию"));
            TeamManager.GiveKitTo(server, serverPlayer);
        }

        // Create a timer for the preparation phase
        TimerManager.createRealTimeTimer(
            PREPARATION_DURATION_MS,
            () -> StartBattle(server),
            ticks -> updatePreparationTimer(ticks)
        );
    }
    
    public int StartPreparation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        StartPreparation(context.getSource().getServer());
        return 1;
    }

    private void StartBattle(MinecraftServer server) {
        gameStage = GameStage.Battle;

        TeamManager.ChangeGameModeOfAllParticipants(GameType.ADVENTURE);
        server.setDifficulty(Difficulty.NORMAL, true);

        BlockPos SpawnRed = TeamManager.Get(TeamColor.RED).Spawn();
        BlockPos SpawnBlue = TeamManager.Get(TeamColor.BLUE).Spawn();
        ServerLevel serverLevel = server.getLevel(Level.OVERWORLD);

        // Play horn sounds
        SoundHandler.playSoundFromPosition(serverLevel, SpawnRed, ModSounds.HORN_DIRE.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);
        SoundHandler.playSoundFromPosition(serverLevel, SpawnBlue, ModSounds.HORN_RADIANT.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);

        // Create a countdown timer before the battle starts
        TimerManager.createRealTimeTimer(
            COUNTDOWN_DURATION_MS,
            () -> handleCountdownCompleted(server),
            ticks -> updateCountdownTimer(ticks, server)
        );
    }
    
    public int StartBattle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        StartBattle(context.getSource().getServer());
        return 1;
    }

    private void handleCountdownCompleted(MinecraftServer server) {
        // Show game start messages to all players
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            var team = TeamManager.GetPlayersTeam(serverPlayer);
            if (team == null) continue;
            
            TitleMessage.showTitle(serverPlayer, 
                startGameTexts.get(team.Color()).GetTitle(),
                startGameTexts.get(team.Color()).GetSubtitle());
        }

        // Remove barriers at spawn points
        var success = TeamManager.DeleteBarriersAtSpawn();
        if (!success) {
            UnrealZaruba.LOGGER.error("Спавны команд ещё не готовы");
        }

        // Start the main game timer
        TimerManager.createRealTimeTimer(
            GAME_DURATION_MS,
            () -> CompleteGame(server, TeamColor.BLUE),
            ticks -> updateGameTimer(ticks)
        );
    }

    private void updateVotingTimer(int ticks) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (COMMANDER_VOTING_DURATION_MS / 1000) - (ticks / 20);
        gameTimer.updateScoreboardTimerMinutes(secondsRemaining / 60);
        gameTimer.updateScoreboardTimerSeconds(secondsRemaining % 60);
    }

    private void updatePreparationTimer(int ticks) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (PREPARATION_DURATION_MS / 1000) - (ticks / 20);
        gameTimer.updateScoreboardTimerMinutes(secondsRemaining / 60);
        gameTimer.updateScoreboardTimerSeconds(secondsRemaining % 60);
    }

    private void updateCountdownTimer(int ticks, MinecraftServer server) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (COUNTDOWN_DURATION_MS / 1000) - (ticks / 20);
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            TitleMessage.showTitle(serverPlayer, Component.literal("§6До начала игры§r"),
                    Component.literal("▌§l " + secondsRemaining + " §r▌"));
        }
    }

    private void updateGameTimer(int ticks) {
        if (ticks % 20 != 0) return;
        
        int secondsRemaining = (GAME_DURATION_MS / 1000) - (ticks / 20);
        gameTimer.updateScoreboardTimerMinutes(secondsRemaining / 60);
        gameTimer.updateScoreboardTimerSeconds(secondsRemaining % 60);
    }

    public void HandleConnectedPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();

        var isInTeam = TeamManager.IsInTeam(serverPlayer);
        var isDead = NBT.readEntityTag(serverPlayer, "isPlayerDead");

        if (server == null)
            return;

        if (gameStage == GameStage.Preparation) {
            TeleportToLobby(serverPlayer, server);
            return;
        }
        if (!isInTeam) {
            MakePlayerSpectator(serverPlayer, server);
            return;
        }
        if (isDead == 1) {
            ReturnToTeamSpawn(serverPlayer);
        }
    }

    private void TeleportToLobby(ServerPlayer serverPlayer, MinecraftServer server) {
        serverPlayer.setGameMode(GameType.ADVENTURE);
        BlockPos lobby = server.getLevel(SimpleWorldManager.LOBBY_DIMENSION).getSharedSpawnPos();
        serverPlayer.teleportTo(server.getLevel(SimpleWorldManager.LOBBY_DIMENSION), 0, 0, 0, Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot());
        serverPlayer.teleportTo(lobby.getX(), lobby.getY(), lobby.getZ());
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
        scheduledExecutorService.schedule(() -> CompleteGameDelayed(server), 10, TimeUnit.SECONDS);
        leaderboardService.UpdatePlayerRanking(TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        // TODO: WorldManager.ReloadMap(server);
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
