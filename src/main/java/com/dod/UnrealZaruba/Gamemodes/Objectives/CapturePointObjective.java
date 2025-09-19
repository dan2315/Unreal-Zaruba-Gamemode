package com.dod.UnrealZaruba.Gamemodes.Objectives;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ProgressDisplay.NetworkedHudElement;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.NetworkPackets.ClientboundObjectivesPacket;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.RenderableZonesPacket;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import com.dod.UnrealZaruba.Renderers.ColoredSquareZone;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.UI.Objectives.HudCapturePointObjective;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import net.minecraft.world.entity.player.Player;
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
        setProgressDisplay(new NetworkedHudElement(this));
    }

    @Override
    public void InitializeAfterSerialization() {
        super.InitializeAfterSerialization();
        captureAreaAABB = new AABB(captureArea.getMinPos(), captureArea.getMaxPos());
        setProgressDisplay(new NetworkedHudElement(this));
        TeamManager teamManager = ((TeamGamemode) GamemodeManager.instance.GetActiveGamemode()).GetTeamManager();
        if (Objects.equals(name, "Вертолётная площадка")) {
            owner = teamManager.Get(TeamColor.RED);
        } else if (Objects.equals(name, "Склад боеприпасов")) {
            owner = teamManager.Get(TeamColor.BLUE);
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
        SetProgress(0);
    }

    public ObjectiveOwner GetBeingCapturedBy() {
        return beingCapturedBy;
    }

    public void SendUpdateHudElement() {
        var ownerTeam = (TeamContext) GetOwner();
        int ownerColor = ownerTeam == null ? 0xDDDDEE : ownerTeam.GetIntColor();
        var beingCapturedBy = (TeamContext) GetBeingCapturedBy();
        int capturedByColor = beingCapturedBy == null ?  0xDDDDEE : beingCapturedBy.GetIntColor();
        var players = server.getPlayerList().getPlayers();
        for (var player : players) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundObjectivesPacket(
                    List.of(new HudCapturePointObjective(GetRuntimeId(), GetName(), ownerColor, capturedByColor, GetProgress(), GetPosition()))
            ));
        }
    }

    public enum CaptureStatus {
        Blocked,
        Capturing,
        None
    }
}
