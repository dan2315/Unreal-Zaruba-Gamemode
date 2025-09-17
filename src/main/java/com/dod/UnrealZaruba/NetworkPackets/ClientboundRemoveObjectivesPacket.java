package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.Gamemodes.List;

import com.dod.UnrealZaruba.UI.Objectives.ObjectivesOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ClientboundRemoveObjectivesPacket {
    Integer size; // Можно отправить пустой пакет, что значит - удалить все обжективы из списка
    ArrayList<Byte> runtimeIds;

    public ClientboundRemoveObjectivesPacket() {
        size = 0;
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

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (size == 0) {
                ObjectivesOverlay.INSTANCE.Clear();
            } else {
                ObjectivesOverlay.INSTANCE.RemoveById(runtimeIds);
            }
        });
    }
}
