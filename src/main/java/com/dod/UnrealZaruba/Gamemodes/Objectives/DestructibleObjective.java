package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.FireworkLauncher;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.Utils.IResettable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DestructibleObjective extends PositionedGameobjective implements IResettable, IPlayerTickAware {
    private static final int PROGRESS_UPDATE_INTERVAL = 40;
    private static final int VISIBILITY_UPDATE_INTERVAL = 40;
    private static final int NOTIFY_BLOCK_THRESHOLD = 10;

    BlockVolume volume;
    int blockAmount;
    int remainingBlockAmount;
    float requiredDegreeOfDestruction = 0.1f;

    transient Set<BlockPos> trackedBlocks;
    transient ServerLevel world;
    transient int updateCounter = 0;
    transient int notifyBlockCounter = 0;
    
    private final transient Map<UUID, Integer> playerVisibilityTicks = new HashMap<>();

    public DestructibleObjective(BlockVolume volume, String name) {
        super(name, "destructible", volume.GetCenter());
        this.volume = volume;
        this.world = WorldManager.gameLevel;
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

    @Override
    protected boolean UpdateImplementation() {
        updateCounter++;
        
        if (updateCounter % PROGRESS_UPDATE_INTERVAL == 0) {
            return UpdateObjectiveState();
        }
        return false;
    }
    
    private boolean UpdateObjectiveState() {
        if (IsCompleted()) return false; 
        if (trackedBlocks == null) return false;

        int counter = 0;
        List<BlockPos> toRemove = new ArrayList<>();

        for (BlockPos pos : trackedBlocks) {
            LevelChunk chunk = world.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            if (chunk == null) return false;

            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                toRemove.add(pos);
                counter++;
            }
        }

        trackedBlocks.removeAll(toRemove);
        remainingBlockAmount -= counter;

        progress = GetProgress();
        float degreeOfDestruction = 1 - progress;
        if (degreeOfDestruction >= requiredDegreeOfDestruction) {
            return true; // It means that objective is completed
        }

        progress = 1 - (degreeOfDestruction/requiredDegreeOfDestruction);
        
        updateProgressDisplay();

        notifyBlockCounter += counter;
        if (notifyBlockCounter >= NOTIFY_BLOCK_THRESHOLD) {
            this.sendObjectiveChanged(this);
            notifyBlockCounter = 0;
        }
        return false;
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, ServerPlayer player) {
        if (IsCompleted()) return;
        UUID playerId = player.getUUID();
        
        playerVisibilityTicks.putIfAbsent(playerId, 0);
        int ticks = playerVisibilityTicks.get(playerId) + 1;
        playerVisibilityTicks.put(playerId, ticks);
        
        if (ticks % VISIBILITY_UPDATE_INTERVAL == 0) {
            updateProgressDisplayForPlayer(player);
        }
    }

    @Override
    public void OnCompleted() {
        UnrealZaruba.LOGGER.info("Objective {} completed", name);
        this.sendObjectiveCompleted(this);
        progressDisplay.clear();

        String border = "==================================";
        String paddedName = String.format("||           §b%s§r был уничтожен           ", name);
        String message = border + "\n" + paddedName + "\n" + border;

        // TODO: Add to Notifier
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(message));
        }

        FireworkLauncher.launchFireworks(world, volume.GetCenter(), 10);
    }

    public String GetName() {
        return name;
    }

    private float GetProgress() {
        return ((float) (remainingBlockAmount)) / blockAmount;
    }

    private void updateProgressDisplayForPlayer(ServerPlayer player) {
        if (progressDisplay != null) {
            progressDisplay.updatePlayerVisibility(player);
        }
    }


    public void setProgressBarActivationDistance(float distance) {
        if (progressDisplay != null) {
            progressDisplay.setActivationDistance(distance);
        }
    }


    @Override
    public void reset() {
        Reset();
    }
}