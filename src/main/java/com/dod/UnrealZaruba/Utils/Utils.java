package com.dod.UnrealZaruba.Utils;

import com.dod.UnrealZaruba.unrealzaruba;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;

public class Utils {
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
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (world == null){
            unrealzaruba.LOGGER.warn("[Ай, бля] World not found");
            return;
        }

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

    public static void SetGamemodeAllExcludeOP(PlayerList playerList, GameType gameType) {
        for (ServerPlayer player : playerList.getPlayers()) {

            if (!playerList.isOp(player.getGameProfile())) {
                player.setGameMode(gameType);
            }
        }
    }

    public static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static void ClearInventoryAllPlayerExcludeOP(PlayerList playerList, GameType gameType) {
        for (ServerPlayer player : playerList.getPlayers()) {

            player.getInventory().clearContent();
        }
    }
}
