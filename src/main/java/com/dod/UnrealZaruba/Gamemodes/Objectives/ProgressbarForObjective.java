package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class ProgressbarForObjective implements IProgressDisplay {
    private final ServerBossEvent bossBar;
    private final Set<UUID> playersWithBossBar = new HashSet<>();
    private final PositionedGameobjective objective;
    private float activationDistance = 10000f;
    
    public ProgressbarForObjective(PositionedGameobjective objective, String name) {
        this.objective = objective;
        this.bossBar = new ServerBossEvent(
            Component.literal(name),
            BossEvent.BossBarColor.BLUE, 
            BossEvent.BossBarOverlay.PROGRESS
        );
    }
    
    @Override
    public void updateProgress(float progress) {
        UnrealZaruba.LOGGER.info("Updating progress for objective: {}", objective.GetName());
        bossBar.setProgress(progress);
    }
    
    @Override
    public void updatePlayerVisibility(ServerPlayer player) {
        boolean isNearTarget = isPlayerNearTarget(player, objective.getPosition());
        
        if (isNearTarget && !playersWithBossBar.contains(player.getUUID())) {
            addPlayerToBossBar(player);
        } else if (!isNearTarget && playersWithBossBar.contains(player.getUUID())) {
            removePlayerFromBossBar(player);
        }
    }
    
    private boolean isPlayerNearTarget(ServerPlayer player, BlockPos pos) {
        double distance = player.blockPosition().distSqr(pos);
        return distance <= activationDistance;
    }
    
    private void addPlayerToBossBar(ServerPlayer player) {
        bossBar.addPlayer(player);
        playersWithBossBar.add(player.getUUID());
    }
    
    private void removePlayerFromBossBar(ServerPlayer player) {
        bossBar.removePlayer(player);
        playersWithBossBar.remove(player.getUUID());
    }
    
    @Override
    public void setActivationDistance(float distance) {
        this.activationDistance = distance;
    }
    
    @Override
    public void clear() {
        playersWithBossBar.clear();
    }
} 