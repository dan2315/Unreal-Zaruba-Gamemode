package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Utils.TimerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.DiscordIntegration.LeaderboardReqs;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.OpenScreenPacket;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.NBT;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
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


public class DestroyObjectivesGamemode extends TeamGamemode {
    private static final int COMMANDER_VOTING_DURATION_TICKS = 3 * 60; // 3 minutes in seconds
    private static final int PREPARATION_DURATION_TICKS = 7 * 60; // 7 minutes in seconds
    private static final int GAME_DURATION_TICKS = 50 * 60; // 50 minutes in seconds
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    Scoreboard scoreboard;
    Objective objective;

    GameObjective[] objectives;

    public DestroyObjectivesGamemode(MinecraftServer server) {
        currentGamemode = this;
        scoreboard = ServerLifecycleHooks.getCurrentServer().getScoreboard();
        objective = scoreboard.getObjective(ScoreboardManager.OBJECTIVE_NAME);
        startGameTexts.put(TeamColor.RED, new StartGameText(
                "§c Игра началась, в бой!",
                "Необходимо уничтожить 3 цели"));
        startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Продержитесь 50 минут"));
        Utils.LoadChunksInArea(server.getLevel(Level.OVERWORLD), -1024, -512, 1024, 512);

        ServerLifecycleHooks.getCurrentServer().setDifficulty(Difficulty.PEACEFUL, true);
        objectives = DestructibleObjectivesHandler.Load(this);
    }

    @Override
    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ScoreboardManager.setupScoreboard(server, 999);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UnrealZaruba.LOGGER.warn(player.getName().toString());
            TeamManager.teleportToSpawn(player);
        }
        return StartVotingForCommander(context);
    }

    public int StartVotingForCommander(CommandContext<CommandSourceStack> context) {
        gameStage = GameStage.CommanderVoting;
        MinecraftServer server = context.getSource().getServer();

        HashMap<TeamColor, TeamU> teams = GamemodeManager.Get(context.getSource().getLevel(), TeamGamemode.class).TeamManager.GetTeams();

        for (var team : teams.entrySet()) {
            for (var player : team.getValue().Members()) {
                var serverPlayer = server.getPlayerList().getPlayer(player);
                if (serverPlayer == null) continue;
    
                TitleMessage.showTitle(serverPlayer, new TextComponent("§6Выбор командира"),
                        new TextComponent("Для того, чтобы проголосовать используй"));
    
                Map<String, Object> data = new HashMap<>();
                data.put("teammates", team.getValue().Members());
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new OpenScreenPacket(1, data));
            } 
        }


        TimerManager.Create(COMMANDER_VOTING_DURATION_TICKS * 1000,
                () -> {
                    try {
                        for (Map.Entry<TeamColor, TeamU> teamEntry : teams.entrySet()) {
                            TeamU team = teamEntry.getValue();
                            if (team.VoteList().isEmpty()) {
                                UnrealZaruba.LOGGER.warn("Vote list is empty");
                                continue;
                            }
        
                            
                            int listSize = team.VoteList().size();
                            Player most_voted_player = server.getPlayerList().getPlayer(team.VoteList().get(0));
                            StringBuilder message = new StringBuilder (
                                "===========================\nТоп 5 голосования:\n");
                            for (int i = 0; i < Math.min(5, listSize); i++) {
                                ServerPlayer topPlayer = server.getPlayerList().getPlayer(team.VoteList().get(i));
                                if (topPlayer != null) {
                                    Integer voteCount = PlayerContext.Get(topPlayer.getUUID()).Votes();
                                    message.append(i + 1 +". " + topPlayer.getName().getString() + ": " + voteCount + "\n");
                                }
                            }
                            message.append("===========================");
                            teamEntry.getValue().setCommander(context.getSource().getServer(), most_voted_player);
                            team.SendMessage(server, message.toString());
                        }
                        StartPreparation(context);
                    } catch (Exception e) {
                        context.getSource().sendFailure(new TextComponent(e.getMessage()));
                    }
                }, ticks -> {
                    if (ticks % 20 != 0)
                        return;
                    ScoreboardManager.UpdateScoreboardTimerMinutes(scoreboard, objective,
                            (COMMANDER_VOTING_DURATION_TICKS - (ticks / 20)) / 60);
                    ScoreboardManager.UpdateScoreboardTimerSeconds(scoreboard, objective,
                            ((COMMANDER_VOTING_DURATION_TICKS - (ticks / 20)) % 60));
                });

        return 1;
    }

    public int StartPreparation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Utils.SetGamemodeAllExcludeOP(context.getSource().getServer().getPlayerList(), GameType.ADVENTURE);
        gameStage = GameStage.StrategyTime;

        for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
            TitleMessage.showTitle(serverPlayer, new TextComponent("§6Стадия подготовки"),
                    new TextComponent("Коммандиром стал " +
                            TeamManager.GetPlayersTeam(serverPlayer).CommanderName() + ", он определяет стратегию"));
            TeamManager.GiveKitTo(context.getSource().getServer(), serverPlayer);
        }

        TimerManager.Create(PREPARATION_DURATION_TICKS * 1000,
                () -> {
                    try {
                        StartBattle(context);
                    } catch (Exception e) {
                        context.getSource().sendFailure(new TextComponent(e.getMessage()));
                    }
                }, ticks -> {
                    if (ticks % 20 != 0)
                        return;
                    ScoreboardManager.UpdateScoreboardTimerMinutes(scoreboard, objective,
                            (PREPARATION_DURATION_TICKS - (ticks / 20)) / 60);
                    ScoreboardManager.UpdateScoreboardTimerSeconds(scoreboard, objective,
                            ((PREPARATION_DURATION_TICKS - (ticks / 20)) % 60));
                });

        return 1;
    }

    public int StartBattle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        gameStage = GameStage.Battle;

        TeamManager.ChangeGameModeOfAllParticipants(GameType.ADVENTURE);
        ServerLifecycleHooks.getCurrentServer().setDifficulty(Difficulty.NORMAL, true);

        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos SpawnRed = TeamManager.Get(TeamColor.RED).Spawn();
        BlockPos SpawnBlue = TeamManager.Get(TeamColor.BLUE).Spawn();
        SoundHandler.playSoundFromPosition(player.getLevel(), SpawnRed, ModSounds.HORN_DIRE.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);
        SoundHandler.playSoundFromPosition(player.getLevel(), SpawnBlue, ModSounds.HORN_RADIANT.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);

        int timerDuration = 10;
        TimerManager.Create(timerDuration * 1000, () -> {
            for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
                var team = TeamManager.GetPlayersTeam(serverPlayer);
                if (team == null)
                    continue;
                TitleMessage.showTitle(serverPlayer, startGameTexts.get(team.Color()).GetTitle(),
                        startGameTexts.get(team.Color()).GetSubtitle());
            }

            var success = TeamManager.DeleteBarriersAtSpawn();

            if (!success)
                context.getSource().sendFailure(new TextComponent("Спавны команд ещё не готовы"));


            TimerManager.Create(GAME_DURATION_TICKS * 1000,
                    () -> {
                        CompleteGame(context.getSource().getServer(), TeamColor.BLUE);

                    }, ticks -> {
                        if (ticks % 20 != 0)
                            return;
                        ScoreboardManager.UpdateScoreboardTimerMinutes(scoreboard, objective,
                                (GAME_DURATION_TICKS - (ticks / 20)) / 60);
                        ScoreboardManager.UpdateScoreboardTimerSeconds(scoreboard, objective,
                                ((GAME_DURATION_TICKS - (ticks / 20)) % 60));
                    });
        },
                ticks -> {
                    if (ticks % 20 != 0)
                        return;
                    for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
                        TitleMessage.showTitle(serverPlayer, new TextComponent("§6До начала игры§r"),
                                new TextComponent("▌§l " + String.valueOf(timerDuration - ticks / 20) + " §r▌"));
                    }
                });
        return 1;
    }

    public void HandleConnectedPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();

        var isInTeam = TeamManager.IsInTeam(serverPlayer);
        var isDead = NBT.readEntityTag(serverPlayer, "isPlayerDead");

        if (server == null)
            return;

        if (gameStage == GameStage.Preparation) {
            ReturnToSpawn(serverPlayer, server);
        } else {
            if (isInTeam) {
                if (isDead == 1) {
                    ReturnToTeamSpawn(serverPlayer);
                } else {
                    serverPlayer.setGameMode(GameType.ADVENTURE);
                }
            } else {
                MakePlayerSpectator(serverPlayer, server);
            }
        }
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

    private void ReturnToSpawn(ServerPlayer serverPlayer, MinecraftServer server) {
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        server.overworld();
        serverPlayer.setRespawnPosition(Level.OVERWORLD, spawn, 0, true, false);
        serverPlayer.setGameMode(GameType.ADVENTURE);
    }

    public void CheckObjectives() {
        if (objectives == null)
            return;
        for (GameObjective gameObjective : objectives) {
            if (!gameObjective.IsCompleted())
                return;
        }
        CompleteGame(ServerLifecycleHooks.getCurrentServer(), TeamColor.RED);
    }

    public void CompleteGame(MinecraftServer server, TeamColor wonTeam) {
        ShowEndText(server, wonTeam);
        scheduledExecutorService.schedule(() -> CompleteGameDelayed(server), 10, TimeUnit.SECONDS);
        LeaderboardReqs.UpdatePlayerRanking(TeamManager.Get(wonTeam).Members(), TeamManager.GetOppositeTeamTo(wonTeam).Members());
    }

    public void CompleteGameDelayed(MinecraftServer server) {
        // WorldManager.ReloadMap(server);
    }

    public void ShowEndText(MinecraftServer server, TeamColor wonTeam) {
        String colorCode = TeamColor.getColorCodeForTeam(wonTeam);
        TextComponent titleText = new TextComponent(
                "Команда " + colorCode + wonTeam.toString() + ChatFormatting.RESET + " победила");
        TextComponent wonText = new TextComponent("Можешь сказать оппоненту \'Сори, что трахнул\'");
        TextComponent loseText = new TextComponent("Что могу сказать? Старайся лучше");
        for (var player : server.getPlayerList().getPlayers()) {
            TeamU team = TeamManager.GetPlayersTeam(player);
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
