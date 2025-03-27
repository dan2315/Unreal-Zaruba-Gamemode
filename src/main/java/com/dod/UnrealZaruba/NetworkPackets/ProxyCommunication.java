package com.dod.UnrealZaruba.NetworkPackets;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;

public class ProxyCommunication {
    
    public static void SentServerStatus(MinecraftServer server, PhaseId phaseId) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeInt(ProxyCommunicationType.SERVER_STATUS.ordinal());
        buffer.writeInt(phaseId.ordinal());

        server.getPlayerList().getPlayers().get(0).connection
        .send(new ClientboundCustomPayloadPacket(NetworkHandler.VELOCITY_CHANNEL_LOCATION, buffer));
    }
}


