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

public class DestructibleObjective extends PositionedGameobjective implements IResettable {
    private static final int PROGRESS_UPDATE_INTERVAL = 40;
    private static final int VISIBILITY_UPDATE_INTERVAL = 40;
    private static final int NOTIFY_BLOCK_THRESHOLD = 10;

    BlockVolume volume;
    int blockAmount;
    int remainingBlockAmount;
    float requiredDegreeOfDestruction = 0.1f;

    transient Boolean isCompleted = false; 
    transient Set<BlockPos> trackedBlocks;
    transient ServerLevel world;
    transient int updateCounter = 0;
    transient int notifyBlockCounter = 0;
    
    private final transient Map<UUID, Integer> playerVisibilityTicks = new HashMap<>();
    private final transient List<IObjectiveNotifier> notificationRecipients = new ArrayList<>();

    public DestructibleObjective(BlockVolume volume, String name) {
        super(volume.GetCenter());
        this.volume = volume;
        this.name = name;
        this.world = WorldManager.gameLevel;
        this.trackedBlocks = InitializeTrackedBlocks(volume);
        this.progressDisplay = new ProgressbarForObjective(this, name);
    }
    

    public void addNotificationRecipient(IObjectiveNotifier recipient) {
        if (recipient != null && !notificationRecipients.contains(recipient)) {
            notificationRecipients.add(recipient);
        }
    }
    
    public void removeNotificationRecipient(IObjectiveNotifier recipient) {
        notificationRecipients.remove(recipient);
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
    public void Update() {
        updateCounter++;
        
        if (updateCounter % PROGRESS_UPDATE_INTERVAL == 0) {
            UpdateObjectiveState();
        }
    }
    
    private void UpdateObjectiveState() {
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

        trackedBlocks.removeAll(toRemove);
        remainingBlockAmount -= counter;

        progress = GetProgress();
        float degreeOfDestruction = 1 - progress;
        if (degreeOfDestruction >= requiredDegreeOfDestruction) {
            Complete();
        }

        progress = 1 - (degreeOfDestruction/requiredDegreeOfDestruction);
        
        updateProgressDisplay();

        notifyBlockCounter += counter;
        if (notifyBlockCounter >= NOTIFY_BLOCK_THRESHOLD) {
            for (IObjectiveNotifier notifier : notificationRecipients) {
                notifier.onObjectiveStateChanged(this); 
            }
            notifyBlockCounter = 0;
        }
    }
    
    /**
     * Handles player tick events for this objective
     */
    public void onPlayerTick(TickEvent.PlayerTickEvent event, ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        playerVisibilityTicks.putIfAbsent(playerId, 0);
        int ticks = playerVisibilityTicks.get(playerId) + 1;
        playerVisibilityTicks.put(playerId, ticks);
        
        if (ticks % VISIBILITY_UPDATE_INTERVAL == 0) {
            updateProgressDisplayForPlayer(player);
        }
    }

    public void Complete() {
        isCompleted = true;

        for (IObjectiveNotifier notifier : notificationRecipients) {
            notifier.onObjectiveCompleted(this);
        }

        String border = "==================================";
        String paddedName = String.format("||           §b%s§r был уничтожен           ", name);
        String message = border + "\n" + paddedName + "\n" + border;

        // TODO: Add to Notifier
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
        isCompleted = false;
        trackedBlocks = InitializeTrackedBlocks(volume);
    }
}