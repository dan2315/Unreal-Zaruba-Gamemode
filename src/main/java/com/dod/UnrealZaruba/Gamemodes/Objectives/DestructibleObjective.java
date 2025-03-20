package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Utils.FireworkLauncher;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DestructibleObjective extends PositionedGameobjective {
    BlockVolume volume;
    int blockAmount;
    int remainingBlockAmount;
    String name;
    float requiredDegreeOfDestruction = 0.1f;

    transient Boolean isCompleted = false; 
    transient Set<BlockPos>  trackedBlocks;
    transient ServerLevel world;
    transient BaseGamemode containingGamemode;

    public DestructibleObjective(BlockVolume volume, String name) {
        super(volume.GetCenter());
        this.volume = volume;
        this.name = name;
        this.world = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        this.trackedBlocks = InitializeTrackedBlocks(volume);
        this.progressDisplay = new ProgressbarForObjective(this, name);
    }
    
    private Set<BlockPos> InitializeTrackedBlocks(BlockVolume volume) {
        UnrealZaruba.LOGGER.info("Начал прогружать: {}", name);
        Set<BlockPos> solidBlocks = new HashSet<>();
        
        volume.ForEachBlock(pos -> {
            if (world.getBlockState(pos).getBlock() != Blocks.AIR) {
                solidBlocks.add(pos);
                this.blockAmount++;
            }
        });
        
        this.remainingBlockAmount = blockAmount;

        UnrealZaruba.LOGGER.info("Цель инициализирована: {}", name);
        return solidBlocks;
    }

    public void SetContainingGamemode(BaseGamemode containingGamemode) {
        this.containingGamemode = containingGamemode;
    }
    
    @Override
    public void Update() {
        if (isCompleted) return; 
        if (trackedBlocks == null) return;

        int counter = 0;
        List<BlockPos> toRemove = new ArrayList<>();

        for (BlockPos pos : trackedBlocks) {
            LevelChunk chunk = world.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            if (chunk == null) return;

            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                toRemove.add(pos);
                counter++;
            }
        }

        if (counter >= 6) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (containingGamemode instanceof TeamGamemode teamGamemode) {
                teamGamemode.GetTeamManager().GetTeams().get(TeamColor.RED).SendMessage(server, 
                "Ваша §l§4команда§r атакует точку §b" + name + "§r. Гойда!");
                teamGamemode.GetTeamManager().GetTeams().get(TeamColor.BLUE).SendMessage(server, 
                "Ваша точка §b" + name + "§r атакована §l§4противниками§r");
            }
        }

        trackedBlocks.removeAll(toRemove);
        remainingBlockAmount -= counter;

        progress = GetProgress();
        float degreeOfDestruction = 1 - progress;
        if (degreeOfDestruction >= requiredDegreeOfDestruction) {
            Complete();
        }

        progress = 1 - (degreeOfDestruction/requiredDegreeOfDestruction);
        
        updateProgressDisplay();
    }

   
    public void Complete() {
        isCompleted = true;
        containingGamemode.CheckObjectives();

        String border = "==================================";
        String paddedName = String.format("||           §b%s§r был уничтожен           ", name);
        String message = border + "\n" + paddedName + "\n" + border;

        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(message));
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
        return ((float) (remainingBlockAmount)) / blockAmount;
    }
}