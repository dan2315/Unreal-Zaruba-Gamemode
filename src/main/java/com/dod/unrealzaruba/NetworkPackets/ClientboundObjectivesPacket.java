package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.UI.Objectives.HudObjective;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import net.minecraft.network.FriendlyByteBuf;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundObjectivesPacket implements ModPacket {
    int objectivesToUpdate;
    List<HudObjective> objectives;

    public ClientboundObjectivesPacket(List<HudObjective> objectives) {
        objectivesToUpdate = objectives.size();
        this.objectives = objectives;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(objectivesToUpdate);
        for (var objective : objectives) {
            objective.Serialize(buffer);
        }
    }

    public static ClientboundObjectivesPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        ArrayList<HudObjective> objectives = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            objectives.add(HudObjective.Deserialize(buffer));
        }
        return new ClientboundObjectivesPacket(objectives);
    }

    public void handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> {
            // Xaero waypoints follow the overlay on their own, see XaeroWaypoints
            ObjectivesOverlay.INSTANCE.SetObjectives(objectives);
        });
        context.get().setPacketHandled(true);
    }
}
