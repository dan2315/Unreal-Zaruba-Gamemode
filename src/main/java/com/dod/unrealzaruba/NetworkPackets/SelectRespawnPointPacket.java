package com.dod.unrealzaruba.NetworkPackets;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.IRespawnPoint;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SelectRespawnPointPacket {
    
    private final UUID playerID;
    private final byte respawnPointIndex;

    public SelectRespawnPointPacket(UUID playerID, byte respawnPointIndex) {
        this.playerID = playerID;
        this.respawnPointIndex = respawnPointIndex;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public byte getRespawnPointIndex() {
        return respawnPointIndex;
    }

    public static void encode(SelectRespawnPointPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.getPlayerID());
        buffer.writeByte(packet.getRespawnPointIndex());
    }

    public static SelectRespawnPointPacket decode(FriendlyByteBuf buffer) {
        UUID playerID = buffer.readUUID();
        byte respawnPointIndex = buffer.readByte();
        return new SelectRespawnPointPacket(playerID, respawnPointIndex);
    }

    public static void handle(SelectRespawnPointPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TeamPlayerContext player = ((TeamPlayerContext)PlayerContext.Get(packet.getPlayerID()));
            if (player == null) return;
            TeamContext team = player.Team();
            if (team == null) return;
            IRespawnPoint respawnPoint = team.RespawnPoints().stream().filter(point -> point.getRuntimeId() == packet.respawnPointIndex).findFirst().orElseThrow();
            player.SelectRespawnPoint(respawnPoint);
        });
        ctx.get().setPacketHandled(true);
    }
}
