package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.List;
import java.util.Set;

import org.valkyrienskies.core.impl.chunk_tracking.c;

import java.util.ArrayList;
import java.util.HashSet;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Utils.FireworkLauncher;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DestructibleObjective extends GameObjective {
    BlockVolume volume;
    int remainingBlockAmount;
    String name;
    float requiredDegreeOfDestruction = 0.1f;

    transient Boolean isCompleted = false; 
    transient Set<BlockPos>  trackedBlocks;
    transient ServerLevel world;

    public DestructibleObjective(BlockVolume volume, String name) {
        this.volume = volume;
        this.name = name;
        this.remainingBlockAmount = volume.GetBlockAmount();
        this.world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        this.trackedBlocks = InitializeTrackedBlocks(volume);
    }

    private Set<BlockPos> InitializeTrackedBlocks(BlockVolume volume) {
        Set<BlockPos> solidBlocks = new HashSet<>();

        volume.ForEachBlock(pos -> {
            if (world.getBlockState(pos).getBlock() != Blocks.AIR) {
                solidBlocks.add(pos);
            }
        });

        return solidBlocks;
    }

    public float Update() {
        if (isCompleted) return 0; 
        if (trackedBlocks == null) return 0;

        int counter = 0;
        List<BlockPos> toRemove = new ArrayList<>();

        for (BlockPos pos : trackedBlocks) {
            LevelChunk chunk = world.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            if (chunk == null) return 1;

            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                toRemove.add(pos);
                counter++;
            }
        }

        trackedBlocks.removeAll(toRemove);
        remainingBlockAmount -= counter;

        float progress = GetProgress();
        float degreeOfDestruction = 1 - progress;
        if (degreeOfDestruction >= requiredDegreeOfDestruction) {
            Complete();
        }

        return 1 - (degreeOfDestruction/requiredDegreeOfDestruction);
    }

   
    public void Complete() {
        isCompleted = true;
        BaseGamemode.currentGamemode.CheckObjectives();

        String border = "==================================";
        String paddedName = String.format("||           %s был уничтожен           ", name);
        String message = border + "\n" + paddedName + "\n" + border;

        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            player.sendMessage(new TextComponent(message), player.getUUID());
        }

        FireworkLauncher.launchFireworks(world, volume.GetCenter(), 10);
    }

    @Override
    public Boolean IsCompleted() {
        return isCompleted;
    }

    public String GetName() {
        return name;
    }

    private float GetProgress() {
        return ((float) (remainingBlockAmount)) / volume.GetBlockAmount();
    }
}