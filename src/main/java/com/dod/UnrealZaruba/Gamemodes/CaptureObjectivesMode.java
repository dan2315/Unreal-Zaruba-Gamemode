package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Title.TitleMessage;

import static com.dod.UnrealZaruba.SoundHandler.SoundHandler.playSound;
import static com.dod.UnrealZaruba.TeamLogic.TeamManager.GetPlayersTeam;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber
public class CaptureObjectivesMode {

    private static GameStage gameStage = GameStage.Preparation;

    public static int StartPreparation(CommandContext<CommandSourceStack> context) {
        SetGamemodeAllExcludeOP(context.getSource().getServer().getPlayerList(), GameType.ADVENTURE);

        gameStage = GameStage.Preparation;

        return 1;
    }

    public static int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AtomicInteger until_time = new AtomicInteger(11);
        gameStage = GameStage.Battle;

        var success = TeamManager.DeleteBarriersAtSpawn();
        if (!success) context.getSource().sendFailure(new TextComponent("Спавны команд ещё не готовы"));
        TeamManager.ChangeGameModeOfAllParticipants(GameType.ADVENTURE);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (GetPlayersTeam(player) == null) {
                player.sendMessage(new TextComponent("Лох пидр"), player.getUUID());
                continue;
            }
            if (GetPlayersTeam(player).Color() == TeamColor.RED) {
                playSound(player, ModSounds.horn_dire, player.position(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if (GetPlayersTeam(player).Color() == TeamColor.BLUE) {
                playSound(player, ModSounds.horn_radiant, player.position(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        Runnable task = () -> {
            //Pray to God this code works
            until_time.set(until_time.get() - 1);
            System.out.println("Task executed at Пенис" + System.currentTimeMillis());

            if (until_time.get() < 0){
                scheduler.shutdown();
                until_time.set(11);
            }
        };

        // Schedule the task to run every 5 seconds with an initial delay of 0 seconds
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                TitleMessage.showTitle(player, new TextComponent("§6 Игра началась, в бой!"),
                        new TextComponent("Рассаживайтесь по технике и едьте крушить оппонентов"));
            }
            scheduler.shutdown();
//          until_time = 11;
        }, 10, TimeUnit.SECONDS);// stops the scheduler after 10 seconds
        return 1;
    }

    public static void SetGamemodeAllExcludeOP(PlayerList playerList, GameType gameType) {
        for (ServerPlayer player : playerList.getPlayers()) {

            if (!playerList.isOp(player.getGameProfile())) {
                player.setGameMode(gameType);
            }
        }
    }

    public static void ClearInventoryAllPlayerExcludeOP(PlayerList playerList, GameType gameType) {
        for (ServerPlayer player : playerList.getPlayers()) {

            player.getInventory().clearContent();
        }
    }

    public static void ProcessNewPlayer(Player player) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null)
            return;

        if (!TeamManager.IsInTeam(serverPlayer)) {
            BlockPos spawn = server.overworld().getSharedSpawnPos();
            setSpawnPoint(serverPlayer, new BlockPos(62, 55, 163));
            serverPlayer.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        }

        if (gameStage == GameStage.Preparation) {
            serverPlayer.setGameMode(GameType.ADVENTURE);
        } else if (gameStage == GameStage.Battle) {
            if (!TeamManager.IsInTeam(serverPlayer)) {
                serverPlayer.setGameMode(GameType.SPECTATOR);
            }
        }
    }

    public static void setupScoreboard(MinecraftServer server) {

        Scoreboard scoreboard = server.getScoreboard();

        Objective defendersObjective = null;
        Objective attackersObjective = null;

        if (scoreboard.getObjective("defendersTimer") == null) {
            defendersObjective = scoreboard.addObjective("defendersTimer", ObjectiveCriteria.AIR,
                    new TextComponent("Защищай цели"), ObjectiveCriteria.RenderType.INTEGER);
        }
        if (scoreboard.getObjective("defendersTimer") == null) {
            attackersObjective = scoreboard.addObjective("attackersScore", ObjectiveCriteria.AIR,
                    new TextComponent("Набери 100 очков"), ObjectiveCriteria.RenderType.INTEGER);
        }

        scoreboard.getOrCreatePlayerScore("Villager", attackersObjective).setScore(30);
        scoreboard.getOrCreatePlayerScore("Container", attackersObjective).setScore(50);
        scoreboard.getOrCreatePlayerScore("Infrastructure", attackersObjective).setScore(10);
    }

    public static void setSpawnPoint(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            CommandSourceStack commandSourceStack = server.createCommandSourceStack();
            String command = String.format("/spawnpoint %s %d %d %d", player.getName().getString(),
             pos.getX(), pos.getY(), pos.getZ());
            server.getCommands().performCommand(commandSourceStack, command);
        }
    }

    public static void deleteBarriers(BlockPos pos, int chunkRadius) {
        unrealzaruba.LOGGER.info("РАЗ БЛЯ");
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        unrealzaruba.LOGGER.info("РАЗ БЛЯ");
        if (world == null){
            unrealzaruba.LOGGER.warn("[Ай, бля] World not found");
            return;
        }
        unrealzaruba.LOGGER.info("РАЗ БЛЯ");

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                int chunkX = new ChunkPos(pos).x + x;
                int chunkZ = new ChunkPos(pos).z + z;
                unrealzaruba.LOGGER.info("РАЗ БЛЯ");
                if (world.hasChunk(chunkX, chunkZ)) {
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            for (int by = world.getMinBuildHeight(); by < world.getMaxBuildHeight(); by++) {
                                BlockPos blockPos = new BlockPos((chunkX << 4) + bx, by, (chunkZ << 4) + bz);
                                BlockState blockState = world.getBlockState(blockPos);
                                if (blockState.getBlock() == Blocks.BARRIER) {
                                    world.removeBlock(blockPos, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

