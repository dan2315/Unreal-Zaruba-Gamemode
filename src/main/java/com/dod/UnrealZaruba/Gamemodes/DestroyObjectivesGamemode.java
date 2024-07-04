package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Utils.TickTimer;
import com.dod.UnrealZaruba.Utils.TimerManager;
import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.Aaaaaaaa.StartGameText;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.Utils.Utils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import javax.security.auth.login.CredentialException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DestroyObjectivesGamemode extends BaseGamemode {
    int until_time = 11;

    public static void initialize() {
        TeamManager.AddTeam(TeamColor.RED);
        TeamManager.AddTeam(TeamColor.BLUE);
        currentGamemode = new DestroyObjectivesGamemode();
        currentGamemode.startGameTexts.put(TeamColor.RED, new StartGameText(
                "\"§c Игра началась, в бой!\"",
                "Необходимо уничтожить 3 цели"));
        currentGamemode.startGameTexts.put(TeamColor.BLUE, new StartGameText(
                "\"§9 Игра началась, в бой!\"",
                "Продержитесь 40 минут"));
    }

    public int StartPreparation(CommandContext<CommandSourceStack> context) {
        Utils.SetGamemodeAllExcludeOP(context.getSource().getServer().getPlayerList(), GameType.ADVENTURE);

        gameStage = GameStage.Preparation;

        return 1;
    }

    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        gameStage = GameStage.Battle;

        var success = DestroyObjectivesGamemode.TeamManager.DeleteBarriersAtSpawn();
        if (!success)
            context.getSource().sendFailure(new TextComponent("Спавны команд ещё не готовы"));
        DestroyObjectivesGamemode.TeamManager.ChangeGameModeOfAllParticipants(GameType.ADVENTURE);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos SpawnRed = DestroyObjectivesGamemode.TeamManager.Get(TeamColor.RED).GetSpawn();
        BlockPos SpawnBlue = DestroyObjectivesGamemode.TeamManager.Get(TeamColor.BLUE).GetSpawn();
        SoundHandler.playSoundFromPosition(player.getLevel(), SpawnRed, ModSounds.HORN_DIRE.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);
        SoundHandler.playSoundFromPosition(player.getLevel(), SpawnBlue, ModSounds.HORN_RADIANT.get(),
                SoundSource.BLOCKS, 5.0F, 1.0F);

        int timerDuration = 10;
        TimerManager.Create(timerDuration * 1000
                ,() -> {
                    for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
                        var team = TeamManager.GetPlayersTeam(player);
                        TitleMessage.showTitle(serverPlayer, new TextComponent("§6 Игра началась, в бой!"),
                                currentGamemode.startGameTexts.get(team.Color()).GetSubtitle());
                    }
                },
                ticks -> {
                    if (ticks % 20 != 0) return;
                    for (ServerPlayer serverPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
                        TitleMessage.showTitle(serverPlayer, new TextComponent(String.valueOf(timerDuration - ticks / 20)),
                                new TextComponent(""));
                    }
                });
        return 1;
    }

    public void ProcessNewPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null)
            return;

        if (!DestroyObjectivesGamemode.TeamManager.IsInTeam(serverPlayer)) {
            BlockPos spawn = server.overworld().getSharedSpawnPos();
            Utils.setSpawnPoint(serverPlayer, new BlockPos(62, 55, 163));
            serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        }

        if (gameStage == GameStage.Preparation) {
            serverPlayer.setGameMode(GameType.ADVENTURE);
        } else if (gameStage == GameStage.Battle) {
            if (!DestroyObjectivesGamemode.TeamManager.IsInTeam(serverPlayer)) {
                serverPlayer.setGameMode(GameType.SPECTATOR);
            }
        }
    }

}
