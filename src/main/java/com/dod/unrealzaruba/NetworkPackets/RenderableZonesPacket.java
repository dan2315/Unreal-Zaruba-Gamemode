package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.Renderers.ColoredSquareZone;
import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RenderableZonesPacket {
    int zonesToUpdate;
    List<ColoredSquareZone> squareZones;

    public RenderableZonesPacket(List<ColoredSquareZone> zones) {
        this.squareZones = zones;
        this.zonesToUpdate = zones.size();
    }

    public RenderableZonesPacket() {}

    public static void encode(RenderableZonesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.zonesToUpdate);
        for (var zone : packet.squareZones) {
            SerializationUtils.encodeAABB(buffer, zone.getZone());
            buffer.writeVarInt(zone.getColor());
        }
    }

    public static RenderableZonesPacket decode(FriendlyByteBuf buffer) {
        var packet = new RenderableZonesPacket();
        int zonesToUpdate = buffer.readVarInt();
        packet.squareZones = new ArrayList<>();
        for (int i = 0; i < zonesToUpdate; i++) {
            var zone = SerializationUtils.decodeAABB(buffer);
            var color = buffer.readVarInt();
            packet.squareZones.add(new ColoredSquareZone(zone, color));
        }
        return packet;
    }


    public static void handle(RenderableZonesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                packet.squareZones.forEach(zone ->
                        UnrealZaruba.geometryRenderer.UpdateRenderableObject(zone)));
        ctx.get().setPacketHandled(true);
    }
}
