package com.dod.UnrealZaruba.NetworkPackets;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SelectTentPacket {
    
    private final UUID playerID;
    private final boolean chosen;

    public SelectTentPacket(UUID playerID, boolean chosen) {
        this.playerID = playerID;
        this.chosen = chosen;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public boolean isChosen() {
        return chosen;
    }

    public static void encode(SelectTentPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.getPlayerID());
        buffer.writeBoolean(packet.isChosen());
    }

    public static SelectTentPacket decode(FriendlyByteBuf buffer) {
        UUID playerID = buffer.readUUID();
        boolean chosen = buffer.readBoolean();
        return new SelectTentPacket(playerID, chosen);
    }

    public static void handle(SelectTentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ((TeamPlayerContext)PlayerContext.Get(packet.getPlayerID())).SelectTent(packet.isChosen());
        });
        ctx.get().setPacketHandled(true);
    }
}
