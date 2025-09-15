package com.dod.UnrealZaruba.Gamemodes.Objectives;

import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.RenderableZonesPacket;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import com.dod.UnrealZaruba.Renderers.ColoredSquareZone;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

import static com.dod.UnrealZaruba.WorldManager.WorldManager.server;

public class CapturePointObjective extends PositionedGameobjective {
    private BlockVolume captureArea;

    private transient AABB captureAreaAABB;
    private transient CaptureStatus captureStatus = CaptureStatus.None;
    private transient ObjectiveOwner beingCapturedBy;
    private transient ObjectiveOwner owner;
    private transient int updateCounter;

    public CapturePointObjective(String name, BlockVolume volume) {
        super(name, "capturepoint", volume.GetCenter());
        captureArea = volume;
        captureAreaAABB = new AABB(captureArea.getMinPos(), captureArea.getMaxPos());
    }

    @Override
    public void InitializeAfterSerialization() {
        captureAreaAABB = new AABB(captureArea.getMinPos(), captureArea.getMaxPos());
    }

    public ObjectiveOwner GetOwner() {
        return owner;
    }

    public AABB GetCaptureAreaAABB() {
        return captureAreaAABB;
    }

    @Override
    protected boolean UpdateImplementation() {
        updateCounter++;
        if (updateCounter % 10 != 0) return false; // update every half second
        var level = WorldManager.gameLevel;
        if (level == null) return false;

        var players = level.getEntitiesOfClass(Player.class, captureAreaAABB);

        Map<TeamContext, Integer> teamsInArea = new HashMap();
        players.forEach(player -> {
            var playerContext = (TeamPlayerContext) PlayerContext.Get(player.getUUID());
            teamsInArea.merge(playerContext.Team(), 1, Integer::sum);
        });
        UnrealZaruba.LOGGER.warn("Teams in area are: {}", teamsInArea);

        if (teamsInArea.size() >= 2) {
            captureStatus = CaptureStatus.Blocked;
            return false;
        }

        if (teamsInArea.isEmpty()) {
            AdvanceCapturingProgress(null, 0);
            return false;
        }

        var team = teamsInArea.entrySet().iterator().next();
        UnrealZaruba.LOGGER.warn("Capturing team is: {}, amount: {}", team.getKey().Color().getDisplayName(), team.getValue());
        return AdvanceCapturingProgress(team.getKey(), team.getValue());
    }

    private boolean AdvanceCapturingProgress(TeamContext team, Integer playerAmount) {
        if (team == null && progress >= 0.0f) {                     // ticks back, while no players in area
            progress -= 0.005F;
            if (progress < 0) progress = 0;
            beingCapturedBy = null;
            captureStatus = CaptureStatus.None;
        }
        if (owner != null && team == owner && progress >= 0.0f) {   // being restored by owner
            progress -= (float) (0.01 * Math.log(playerAmount + 1));
            if (progress < 0) progress = 0;
            beingCapturedBy = null;
            captureStatus = CaptureStatus.None;
        }
        else {                                  // being captured by opponent
            progress += (float) (0.01 * Math.log(playerAmount + 1));
            beingCapturedBy = team;
            captureStatus = CaptureStatus.Capturing;
            if (progress >= 1) {
                return true;
            }
        }
//        progressDisplay.updateProgress(progress);
        UnrealZaruba.LOGGER.warn("Advancing progress: {}", progress);
        return false;
    }

    @Override
    protected void OnCompleted() {
        Capture();
        MakeItPossibleToCaptureAgain();
    }

    private void Capture() {
        owner = beingCapturedBy;
        var ownerTeam = (TeamContext) this.GetOwner();
        int color = ownerTeam == null ? 0xDDDDEE : ownerTeam.GetIntColor();
        for(var player : server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new RenderableZonesPacket(List.of(new ColoredSquareZone(captureAreaAABB, color))));
        }
    }

    private void MakeItPossibleToCaptureAgain() {
        SetCompleted(false);
        progress = 0;
    }

    public enum CaptureStatus {
        Blocked,
        Capturing,
        None
    }
}
