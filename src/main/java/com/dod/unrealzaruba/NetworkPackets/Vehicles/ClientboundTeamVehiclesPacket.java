package com.dod.unrealzaruba.NetworkPackets.Vehicles;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.Vehicles.TeamVehiclesState;

import net.minecraft.network.FriendlyByteBuf;

/** Where the team's vehicles are and what they are called: the whole list, so a vehicle that is gone just is not in it. */
public class ClientboundTeamVehiclesPacket implements ModPacket {
    /** {@code topY}: the top of the vehicle, for its label; {@code centerY}: for the map. */
    public record Entry(UUID id, String name, TeamColor team, double x, double topY, double z, double centerY) {}

    public final List<Entry> vehicles;

    public ClientboundTeamVehiclesPacket(List<Entry> vehicles) {
        this.vehicles = vehicles;
    }

    public static void encode(ClientboundTeamVehiclesPacket msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.vehicles, (b, entry) -> {
            b.writeUUID(entry.id());
            b.writeUtf(entry.name(), 64);
            b.writeEnum(entry.team());
            b.writeDouble(entry.x());
            b.writeDouble(entry.topY());
            b.writeDouble(entry.z());
            b.writeDouble(entry.centerY());
        });
    }

    public static ClientboundTeamVehiclesPacket decode(FriendlyByteBuf buf) {
        return new ClientboundTeamVehiclesPacket(buf.readList(b -> new Entry(b.readUUID(), b.readUtf(64),
                b.readEnum(TeamColor.class), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble())));
    }

    public static void handle(ClientboundTeamVehiclesPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> TeamVehiclesState.set(msg.vehicles));
        ctx.get().setPacketHandled(true);
    }
}
