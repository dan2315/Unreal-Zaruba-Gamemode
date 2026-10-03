package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DeleteRenderableZonesPacket {
    List<Integer> ids;

    public DeleteRenderableZonesPacket(List<Integer> ids) {
        this.ids = ids;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(ids.size());
        for (Integer id : ids) {
            buf.writeVarInt(id);
        }
    }

    public static DeleteRenderableZonesPacket decode(FriendlyByteBuf buf) {
        var size = buf.readVarInt();
        var list = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readVarInt());
        }
        return new DeleteRenderableZonesPacket(list);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            for(var id : ids) {
                UnrealZaruba.geometryRenderer.Remove(id);
            }
        });
    }
}
