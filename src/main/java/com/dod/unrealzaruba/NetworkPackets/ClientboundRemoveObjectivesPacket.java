package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.Gamemodes.List;

import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import net.minecraft.network.FriendlyByteBuf;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ClientboundRemoveObjectivesPacket implements ModPacket {
    Integer size; // Можно отправить пустой пакет, что значит - удалить все обжективы из списка
    ArrayList<Byte> runtimeIds;

    public ClientboundRemoveObjectivesPacket() {
        size = 0;
        runtimeIds = new ArrayList<>();
    }

    public ClientboundRemoveObjectivesPacket(ArrayList<Byte> idsToDelete) {
        size = idsToDelete.size();
        runtimeIds = idsToDelete;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(size);
        for (var id : runtimeIds) {
            buffer.writeByte(id);
        }
    }

    public static ClientboundRemoveObjectivesPacket decode(FriendlyByteBuf buffer) {
        var ids = new ArrayList<Byte>();
        var size = buffer.readByte();
        for (int i = 0; i < size; i++) {
            ids.add(buffer.readByte());
        }
        return new ClientboundRemoveObjectivesPacket(ids);
    }

    public void handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> {
            if (size == 0) {
                ObjectivesOverlay.INSTANCE.Clear();
                RespawnClientState.reset();
                // the team vehicles (map diamonds, labels) and any joint purchase ghost belong to the match too
                com.dod.unrealzaruba.UI.Vehicles.TeamVehiclesState.reset();
                com.dod.unrealzaruba.Schematics.PurchaseClientState.reset();
            } else {
                ObjectivesOverlay.INSTANCE.RemoveById(runtimeIds);
            }
        });
    }
}
