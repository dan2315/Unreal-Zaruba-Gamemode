package com.dod.unrealzaruba.Gamemodes.Objectives;

import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;

import com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay.CombinedProgressDisplay;
import com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay.NetworkedSideHudListElement;
import com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay.ProgressbarForObjective;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.FireworkLauncher;
import com.dod.unrealzaruba.utils.DataStructures.BlockVolume;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.dod.unrealzaruba.utils.IResettable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class DestroyObjective extends PositionedGameobjective implements IResettable, IPlayerTickAware {
    private static final int PROGRESS_UPDATE_INTERVAL = 30;
    private static final int VISIBILITY_UPDATE_INTERVAL = 20; // player ticks (Forge fired twice per tick, NeoForge once)
    private static final int NOTIFY_BLOCK_THRESHOLD = 10;

    BlockVolume volume;
    int blockAmount;
    int remainingBlockAmount;
    float requiredDegreeOfDestruction = 0.1f;

    transient Set<BlockPos> trackedBlocks;
    transient ServerLevel world;
    transient int updateCounter = 0;
    transient int notifyBlockCounter = 0;
    
    private transient Map<UUID, Integer> playerVisibilityTicks = new HashMap<>();

    public DestroyObjective(BlockVolume volume, String name) {
        super(name, "destructible", volume.GetCenter());
        this.volume = volume;
        this.world = WorldManager.gameLevel;
        this.trackedBlocks = InitializeTrackedBlocks(volume);
        setProgressDisplay(new CombinedProgressDisplay(List.of(
                new ProgressbarForObjective(this, name),
                new NetworkedSideHudListElement(this)
        )));
    }

    @Override
    public void InitializeAfterSerialization() {
        super.InitializeAfterSerialization();
        playerVisibilityTicks = new HashMap<>();
        this.world = WorldManager.gameLevel;
        this.trackedBlocks = InitializeTrackedBlocks(volume);
        setProgressDisplay(new CombinedProgressDisplay(List.of(
                new ProgressbarForObjective(this, name),
                new NetworkedSideHudListElement(this)
        )));
        UnrealZaruba.LOGGER.warn("Objective {} was initialized", name);
    }

    private Set<BlockPos> InitializeTrackedBlocks(BlockVolume volume) {
        UnrealZaruba.LOGGER.info("Начал прогружать: {}", name);
        Set<BlockPos> solidBlocks = new HashSet<>();
        // Counted afresh: the number saved in objectives.json is not the map's, and adding to it doubled the target
        this.blockAmount = 0;

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

        var destructionProgress = GetProgress();
        float degreeOfDestruction = 1 - destructionProgress;
        if (degreeOfDestruction >= requiredDegreeOfDestruction) {
            return true; // It means that objective is completed
        }

        destructionProgress = 1 - (degreeOfDestruction/requiredDegreeOfDestruction);
        SetProgress(destructionProgress);

        notifyBlockCounter += counter;
        if (notifyBlockCounter >= NOTIFY_BLOCK_THRESHOLD) {
            this.sendObjectiveChanged(this);
            notifyBlockCounter = 0;
        }
        return false;
    }

    public void onPlayerTick(PlayerTickEvent.Post event, ServerPlayer player) {
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

    @Override
    public float GetProgress() {
        return ((float) (remainingBlockAmount)) / blockAmount;
    }

    private void updateProgressDisplayForPlayer(ServerPlayer player) {
        if (progressDisplay != null) {
            progressDisplay.updatePlayerVisibility(player);
        }
    }

    @Override
    public void reset() {
        progressDisplay.clear();
        Reset();
    }
}