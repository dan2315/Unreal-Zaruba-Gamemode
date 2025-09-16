package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UI.Objectives.HudObjectiveUpdate;
import com.dod.UnrealZaruba.UI.Objectives.ObjectivesOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ClientboundUpdateObjectivesPacket {
    int objectivesToUpdate;
    ArrayList<HudObjectiveUpdate> objectiveUpdates;

    public ClientboundUpdateObjectivesPacket(ArrayList<HudObjectiveUpdate> objectiveUpdates) {
        this.objectivesToUpdate = objectiveUpdates.size();
        this.objectiveUpdates = objectiveUpdates;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(objectivesToUpdate);
        for (var update : objectiveUpdates) {
            buffer.writeByte(update.getRuntimeId());
            buffer.writeByte(update.getProgress());
        }
    }

    public static ClientboundUpdateObjectivesPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        ArrayList<HudObjectiveUpdate> updates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            updates.add(new HudObjectiveUpdate(buffer.readByte(), buffer.readByte()));
        }
        return new ClientboundUpdateObjectivesPacket(updates);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ObjectivesOverlay.INSTANCE.UpdateObjectives(this.objectiveUpdates);
        });

    }
}
