package com.dod.unrealzaruba.Vehicles;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Events.BlockStateChangedEvent;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.BaseRespawnPoint;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.dod.unrealzaruba.SableIntegration.SableShips;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
public class Vehicle extends BaseRespawnPoint {
    private final ServerLevel level;
    private final List<UUID> subLevelIds;
    private String vehicleType;
    private TeamColor owner;
    private boolean isAlive;
    private Consumer<Vehicle> initialization;
    private final int hitPoints;
    private int currentHP;
    HashSet<BlockPos> damagedBlocks = new HashSet<>();


    public Vehicle(String vehicleType, TeamColor owner, ServerLevel level, List<UUID> subLevelIds, int totalBlocks, Consumer<Vehicle> initialization) {
        this.vehicleType = vehicleType;
        this.owner = owner;
        this.isAlive = true;
        this.level = level;
        this.subLevelIds = subLevelIds;
        this.initialization = initialization;

        hitPoints = totalBlocks;
        UnrealZaruba.LOGGER.info("Total {} health is: {}", vehicleType, hitPoints);
        currentHP = hitPoints;
    }

    public void Initialize() {
        if (initialization != null) {
            initialization.accept(this);
        }
    }

    public ServerLevel getLevel() {
        return level;
    }

    /** UUIDs of the Sable sub-levels this vehicle consists of. */
    public List<UUID> getSubLevelIds() {
        return subLevelIds;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    public boolean blockInsideShip(BlockPos pos) {
        var subLevel = SableShips.getContaining(level, pos);
        return subLevel != null && subLevelIds.contains(subLevel.getUniqueId());
    }

    public TeamColor getOwner() {
        return owner;
    }

    public void setOwner(TeamColor owner) {
        this.owner = owner;
    }

    @Override
    public String getDisplayName() {
        return "Vehicle";
    }

    @Override
    public BlockPos getSpawnPosition() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public int getPriority() {
        return 1;
    }

    public void OnBlockStateChanged(BlockStateChangedEvent event) {
        if (event.getLevel().equals(WorldManager.gameLevel) && blockInsideShip(event.getPos()) && !damagedBlocks.contains(event.getPos()) && event.getNewState().isAir()) {
            currentHP -= 1;
            damagedBlocks.add(event.getPos());

            UnrealZaruba.LOGGER.warn("HP left: {}", (float) currentHP/hitPoints);
            if ((float) currentHP /hitPoints < 0.8f) {
                // Read the position before the sub-level gets removed
                var subLevel = SableShips.get(level, subLevelIds.get(0));
                UnrealZaruba.vehicleManager.removeVehicle(this);
                if (subLevel == null) {
                    return;
                }
                var position = subLevel.logicalPose().position();
                event.getLevel().explode(
                        null,                // no entity (can also pass player/entity)
                        position.x(),                  // center X
                        position.y(),                  // center Y
                        position.z(),                  // center Z
                        6.0F,                          // explosion power (TNT is 4.0F)
                        Level.ExplosionInteraction.BLOCK // destroy blocks
                );
            }
        }
    }
}
