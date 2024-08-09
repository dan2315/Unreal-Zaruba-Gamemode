package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Utils.TimerManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.DiscordIntegration.LeaderboardReqs;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
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


public class DestroyObjectivesGamemode extends BaseGamemode {
    private static final int COMMANDER_VOTING_DURATION_TICKS = 3 * 60; // 3 minutes in seconds
    private static final int PREPARATION_DURATION_TICKS = 7 * 60; // 7 minutes in seconds
    private static final int GAME_DURATION_TICKS = 50 * 60; // 50 minutes in seconds
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    Scoreboard scoreboard;
    Objective objective;

    GameObjective[] objectives;

    public DestroyObjectivesGamemode() {
        currentGamemode = this;
        scoreboard = ServerLifecycleHooks.getCurrentServer().getScoreboard();
        objective = scoreboard.getObjective(ScoreboardManager.OBJECTIVE_NAME);
        currentGamemode.startGameTexts.put(TeamColor.RED, new StartGameText(
                "§c Игра началась, в бой!",
                "Необходимо уничтожить 3 цели"));
        currentGamemode.startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "§9 Игра началась, в бой!",
                "Продержитесь 50 минут"));

        ServerLifecycleHooks.getCurrentServer().setDifficulty(Difficulty.PEACEFUL, true);
        objectives = DestructibleObjectivesHandler.Load();
    }

    @Override
    public int StartBattle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ScoreboardManager.setupScoreboard(server, 999);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            unrealzaruba.LOGGER.warn(player.getName().toString());
            BaseGamemode.currentGamemode.TeamManager.teleportToSpawn(player);
        }
        return StartVotingForCommander(context);
    }

    public int StartVotingForCommander(CommandContext<CommandSourceStack> context) {
        gameStage = GameStage.CommanderVoting;

        for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
            TitleMessage.showTitle(serverPlayer, new TextComponent("§6Выбор командира"),
                    new TextComponent("Это сложно, но попробуйте выделить одного командира"));
        }

        TimerManager.Create(COMMANDER_VOTING_DURATION_TICKS * 1000,
                () -> {
                    try {
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
                    new TextComponent("Обсудите стратегию со своей командой"));
            TeamManager.GiveKitTo(context.getSource().getServer(), serverPlayer);
        }

        TimerManager.Create(PREPARATION_DURATION_TICKS * 1000,
                () -> {
                    try {
                        StartGame(context);
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

    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        gameStage = GameStage.Battle;

        BaseGamemode.currentGamemode.TeamManager.ChangeGameModeOfAllParticipants(GameType.ADVENTURE);
        ServerLifecycleHooks.getCurrentServer().setDifficulty(Difficulty.NORMAL, true);

        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos SpawnRed = BaseGamemode.currentGamemode.TeamManager.Get(TeamColor.RED).Spawn();
        BlockPos SpawnBlue = BaseGamemode.currentGamemode.TeamManager.Get(TeamColor.BLUE).Spawn();
        SoundHandler.playSoundFromPosition(player.getLevel(), SpawnRed, ModSounds.HORN_DIRE.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);
        SoundHandler.playSoundFromPosition(player.getLevel(), SpawnBlue, ModSounds.HORN_RADIANT.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);

        int timerDuration = 10;
        TimerManager.Create(timerDuration * 1000, () -> {
            for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
                var team = TeamManager.GetPlayersTeam(player);
                if (team == null)
                    continue;
                TitleMessage.showTitle(serverPlayer, currentGamemode.startGameTexts.get(team.Color()).GetTitle(),
                        currentGamemode.startGameTexts.get(team.Color()).GetSubtitle());
            }

            var success = BaseGamemode.currentGamemode.TeamManager.DeleteBarriersAtSpawn();

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

    public void HandleNewPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();

        var isInTeam = BaseGamemode.currentGamemode.TeamManager.IsInTeam(serverPlayer);
        var isDead = NBT.readEntityTag(serverPlayer, "isPlayerDead");

        if (server == null)
            return;

        if (gameStage == GameStage.Preparation) {
            ReturnToSpawn(serverPlayer, server);
        } else {
            if (isInTeam) {
                if (isDead == 1) ReturnToTeamSpawn(serverPlayer);
            } else {
                MakePlayerSpectator(serverPlayer, server);
            }
        }
    }

    private void ReturnToTeamSpawn(ServerPlayer serverPlayer) {
        BaseGamemode.currentGamemode.TeamManager.teleportToSpawn(serverPlayer);
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
