package com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.dod.unrealzaruba.Gamemodes.Objectives.PositionedGameobjective;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

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
        bossBar.setProgress(progress);
    }
    
    @Override
    public void updatePlayerVisibility(ServerPlayer player) {
        if (player.level() != WorldManager.gameLevel)
            return;

        boolean isNearTarget = isPlayerNearTarget(player, objective.GetPosition());
        
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
    public void clear() {
        var server = ServerLifecycleHooks.getCurrentServer();
        Set<UUID> playersToRemove = new HashSet<>(playersWithBossBar);
        for (UUID playerId : playersToRemove) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                bossBar.removePlayer(player);
            }
        }
        playersWithBossBar.clear();
        updateProgress(0);
    }
} 