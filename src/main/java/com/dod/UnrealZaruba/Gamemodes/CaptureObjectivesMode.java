package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
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

@Mod.EventBusSubscriber
public class CaptureObjectivesMode {
    private static GameStage gameStage = GameStage.Preparation;

    public static int StartPreparation(CommandContext<CommandSourceStack> context) {
        SetGamemodeAllExcludeOP(context.getSource().getServer().getPlayerList(), GameType.ADVENTURE);

        gameStage = GameStage.Preparation;

        return 1;
    }

    public static int StartGame(CommandContext<CommandSourceStack> context) {
        gameStage = GameStage.Battle;
        TeamManager.DeleteBarriersAtSpawn();
        TeamManager.ChangeGameModeOfAllParticipants(GameType.SURVIVAL);

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

        if (!TeamManager.IsInTeam(player)) {
            BlockPos spawn = server.overworld().getSharedSpawnPos();
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        }

        if (gameStage == GameStage.Preparation) {
            serverPlayer.setGameMode(GameType.ADVENTURE);
        } else if (gameStage == GameStage.Battle) {
            if (!TeamManager.IsInTeam(player)) {
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

    public static void deleteBarriers(BlockPos pos, int chunkRadius) {
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (world == null)
            return;

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                int chunkX = new ChunkPos(pos).x + x;
                int chunkZ = new ChunkPos(pos).z + z;

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
