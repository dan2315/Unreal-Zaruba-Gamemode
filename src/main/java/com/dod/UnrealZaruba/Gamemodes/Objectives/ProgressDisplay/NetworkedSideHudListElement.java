package com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay;

import com.dod.unrealzaruba.Gamemodes.Objectives.PositionedGameobjective;
import com.dod.unrealzaruba.NetworkPackets.ClientboundUpdateObjectivePacket;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.UI.Objectives.HudObjectiveUpdate;
import com.dod.unrealzaruba.utils.Conversion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import static com.dod.unrealzaruba.WorldManager.WorldManager.server;

public class NetworkedSideHudListElement implements IProgressDisplay {

    private final PositionedGameobjective objective;

    public NetworkedSideHudListElement(PositionedGameobjective objective) {
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
