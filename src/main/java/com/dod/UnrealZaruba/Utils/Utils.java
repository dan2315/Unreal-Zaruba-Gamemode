package com.dod.UnrealZaruba.Utils;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
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


    public static void deleteBarriers(BlockVolume volume) {
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (world == null) {
            UnrealZaruba.LOGGER.warn("[Ай, бля] World not found");
            return;
        }
        
        UnrealZaruba.LOGGER.warn("[Во, бля] Удаляю барьеры " + volume.GetCenter());
        
        volume.ForEachBlock(pos -> {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.getBlock() == Blocks.BARRIER) {
                world.removeBlock(pos, false);
            }
        });
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
        player.setCustomName(new TextComponent("[" + prefix + "] " + player.getName().toString()));
    }
}
