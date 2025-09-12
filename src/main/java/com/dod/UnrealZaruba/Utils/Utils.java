package com.dod.UnrealZaruba.Utils;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.server.ServerLifecycleHooks;

public class Utils {

    @Deprecated
    public static void setSpawnPoint(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            CommandSourceStack commandSourceStack = server.createCommandSourceStack();
            String command = String.format("/spawnpoint %s %d %d %d", player.getName().getString(),
                    pos.getX(), pos.getY(), pos.getZ());
            server.getCommands().performPrefixedCommand(commandSourceStack, command);
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

    public static void SetPrefixTo(ServerPlayer player, String prefix) {
        player.setCustomName(Component.literal("[" + prefix + "] " + player.getName().toString()));
    }

    public static void LoadChunksInArea(ServerLevel world, int minX, int minZ, int maxX, int maxZ) {
        ServerChunkCache chunkProvider = world.getChunkSource();

        for (int x = minX; x <= maxX; x += 16) {
            for (int z = minZ; z <= maxZ; z += 16) {
                ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
                LevelChunk chunk = chunkProvider.getChunk(chunkPos.x, chunkPos.z, true);
                if (chunk != null) {
                    BlockPos ownerPos = new BlockPos(chunkPos.x << 4, 0, chunkPos.z << 4);

                    ForgeChunkManager.forceChunk(world, UnrealZaruba.MOD_ID, ownerPos, chunkPos.x, chunkPos.z, true,
                            true);
                }
            }
        }
    }
}
