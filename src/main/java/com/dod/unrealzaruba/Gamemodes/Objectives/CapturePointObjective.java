package com.dod.unrealzaruba.Gamemodes.Objectives;

import net.minecraft.world.phys.Vec3;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay.NetworkedSideHudListElement;
import com.dod.unrealzaruba.Gamemodes.TeamGamemode;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.CapturePointRespawnPoint;
import net.minecraft.core.BlockPos;
import com.dod.unrealzaruba.NetworkPackets.ClientboundObjectivesPacket;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.NetworkPackets.RenderableZonesPacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.Renderers.ColoredSquareZone;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.TeamLogic.TeamManager;
import com.dod.unrealzaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.DataStructures.BlockVolume;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class CapturePointObjective extends PositionedGameobjective {
    private BlockVolume captureArea;
    // Where the owners respawn (feet position). Null: on the floor at the center of the capture area
    private BlockPos respawnPosition;

    private transient AABB captureAreaAABB;
    private transient CaptureStatus captureStatus = CaptureStatus.None;
    private transient ObjectiveOwner beingCapturedBy;
    private transient ObjectiveOwner owner;
    private transient int updateCounter;
    private transient CapturePointRespawnPoint respawnPoint;

    public CapturePointObjective(String name, BlockVolume volume) {
        super(name, "capturepoint", volume.GetCenter());
        captureArea = volume;
        captureAreaAABB = new AABB(Vec3.atLowerCornerOf(captureArea.getMinPos()), Vec3.atLowerCornerOf(captureArea.getMaxPos()));
        setProgressDisplay(new NetworkedSideHudListElement(this));
    }

    @Override
    public void InitializeAfterSerialization() {
        super.InitializeAfterSerialization();
        captureAreaAABB = new AABB(Vec3.atLowerCornerOf(captureArea.getMinPos()), Vec3.atLowerCornerOf(captureArea.getMaxPos()));
        setProgressDisplay(new NetworkedSideHudListElement(this));
        TeamManager teamManager = ((TeamGamemode) GamemodeManager.instance.GetActiveGamemode()).GetTeamManager();
        respawnPoint = new CapturePointRespawnPoint(this);
        if (Objects.equals(name, "Вертолётная площадка")) {
            owner = teamManager.Get(TeamColor.RED);
        } else if (Objects.equals(name, "Склад боеприпасов")) {
            owner = teamManager.Get(TeamColor.BLUE);
        }
        MoveRespawnPoint(null, owner);
    }

    public BlockPos GetRespawnPosition() {
        return respawnPosition;
    }

    public BlockVolume GetCaptureArea() {
        return captureArea;
    }

    public CapturePointRespawnPoint GetRespawnPoint() {
        return respawnPoint;
    }

    // The owning team can respawn here
    private void MoveRespawnPoint(ObjectiveOwner from, ObjectiveOwner to) {
        if (respawnPoint == null || from == to) return;
        if (from instanceof TeamContext oldTeam) {
            oldTeam.RemoveRespawnPoint(respawnPoint);
        }
        if (to instanceof TeamContext newTeam) {
            newTeam.AddRespawnPoint(respawnPoint);
        }
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

        if (teamsInArea.size() >= 2) {
            captureStatus = CaptureStatus.Blocked;
            return false;
        }

        if (teamsInArea.isEmpty()) {
            AdvanceCapturingProgress(null, 0);
            return false;
        }

        var team = teamsInArea.entrySet().iterator().next();
        return AdvanceCapturingProgress(team.getKey(), team.getValue());
    }

    private boolean AdvanceCapturingProgress(TeamContext team, int playerAmount) {
        double factor = Math.log(playerAmount + 1); // shared scaling
        float delta = 0;
        var progress = GetProgress();

        if (team == null) {                         // decay when no players
            delta = -0.005F;
            captureStatus = CaptureStatus.None;
            if (progress + delta <= 0) {
                beingCapturedBy = null;
            }
        }
        else if (team.equals(owner)) {              // restore by owner
            delta = (float) (-0.01 * factor);
            captureStatus = CaptureStatus.None;
            if (progress + delta <= 0) {
                beingCapturedBy = null;
            }
        }
        else if (!team.equals(beingCapturedBy)) {   // new opponent contesting
            delta = (float) (-0.1 * factor);
            captureStatus = CaptureStatus.Capturing;
            if (progress + delta <= 0) {
                beingCapturedBy = team;
                SendUpdateHudElement();
            }
        }
        else {                                      // being captured by current opponent
            delta = (float) (0.1 * factor);
            captureStatus = CaptureStatus.Capturing;
        }

        AddProgress(delta);
        return progress >= 1;
    }

    @Override
    protected void OnCompleted() {
        Capture();
        SendUpdateHudElement();
        MakeItPossibleToCaptureAgain();
    }

    private void Capture() {
        MoveRespawnPoint(owner, beingCapturedBy);
        owner = beingCapturedBy;
        var ownerTeam = (TeamContext) this.GetOwner();
        int color = ownerTeam == null ? 0xDDDDEE : ownerTeam.GetIntColor();
        for(var player : UnrealZaruba.server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new RenderableZonesPacket(List.of(new ColoredSquareZone(captureAreaAABB, color))));
        }
    }

    private void MakeItPossibleToCaptureAgain() {
        SetCompleted(false);
        SetProgress(0);
    }

    public ObjectiveOwner GetBeingCapturedBy() {
        return beingCapturedBy;
    }

    public HudCapturePointObjective ToHudObjective() {
        var ownerTeam = (TeamContext) GetOwner();
        var capturingTeam = (TeamContext) GetBeingCapturedBy();
        int ownerColor = ownerTeam == null ? 0xDDDDEE : ownerTeam.GetIntColor();
        int capturedByColor = capturingTeam == null ? 0xDDDDEE : capturingTeam.GetIntColor();
        return new HudCapturePointObjective(GetRuntimeId(), GetName(), ownerColor, capturedByColor,
                ownerTeam == null ? null : ownerTeam.Color(), capturingTeam == null ? null : capturingTeam.Color(),
                GetProgress(), GetPosition());
    }

    public void SendUpdateHudElement() {
        var hudObjective = ToHudObjective();
        for (var player : UnrealZaruba.server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new ClientboundObjectivesPacket(List.of(hudObjective)));
        }
    }

    public enum CaptureStatus {
        Blocked,
        Capturing,
        None
    }
}
