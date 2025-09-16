package com.dod.UnrealZaruba.Gamemodes.Objectives.ProgressDisplay;

import com.dod.UnrealZaruba.Gamemodes.Objectives.PositionedGameobjective;
import com.dod.UnrealZaruba.NetworkPackets.ClientboundUpdateObjectivePacket;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.UI.Objectives.HudObjectiveUpdate;
import com.dod.UnrealZaruba.Utils.Conversion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import static com.dod.UnrealZaruba.WorldManager.WorldManager.server;

public class NetworkedHudElement implements IProgressDisplay {

    private final PositionedGameobjective objective;

    public NetworkedHudElement(PositionedGameobjective objective) {
        this.objective = objective;
    }

    @Override
    public void updateProgress(float progress) {
        var players = server.getPlayerList().getPlayers();
        for (var player : players) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new ClientboundUpdateObjectivePacket(new HudObjectiveUpdate(objective.GetRuntimeId(), Conversion.fromNormalizedFloatToByte(progress))));
        }
    }

    @Override
    public void updatePlayerVisibility(ServerPlayer player) {
        // idk, just always visible
    }

    @Override
    public void clear() {

    }
}
