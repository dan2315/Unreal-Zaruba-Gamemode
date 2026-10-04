package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.ModIntegrations.WaypointManager;
import com.dod.unrealzaruba.UI.Objectives.HudObjectiveUpdate;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import net.minecraft.network.FriendlyByteBuf;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import java.util.List;
import java.util.function.Supplier;

public class ClientboundUpdateObjectivePacket implements ModPacket {
    HudObjectiveUpdate objectiveUpdate;

    public ClientboundUpdateObjectivePacket(HudObjectiveUpdate objectiveUpdate) {
        this.objectiveUpdate = objectiveUpdate;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(objectiveUpdate.getRuntimeId());
        buffer.writeByte(objectiveUpdate.getProgress());
    }

    public static ClientboundUpdateObjectivePacket decode(FriendlyByteBuf buffer) {
        return new ClientboundUpdateObjectivePacket(new HudObjectiveUpdate(buffer.readByte(), buffer.readByte()));
    }

    public void handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> {
            ObjectivesOverlay.INSTANCE.UpdateObjectives(List.of(this.objectiveUpdate));
        });
    }
}
