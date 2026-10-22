package com.dod.unrealzaruba.NetworkPackets;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.IRespawnPoint;
import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsSync;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** The sender picks one of their team's respawn points. */
public class SelectRespawnPointPacket implements ModPacket {
    private final byte respawnPointId;

    public SelectRespawnPointPacket(byte respawnPointId) {
        this.respawnPointId = respawnPointId;
    }

    public static void encode(SelectRespawnPointPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.respawnPointId);
    }

    public static SelectRespawnPointPacket decode(FriendlyByteBuf buffer) {
        return new SelectRespawnPointPacket(buffer.readByte());
    }

    public static void handle(SelectRespawnPointPacket packet, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            // Use the connection's player, never an id from the packet: clients could pick for someone else
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !(PlayerContext.Get(sender.getUUID()) instanceof TeamPlayerContext player)) return;
            TeamContext team = player.Team();
            if (team == null) return;
            IRespawnPoint respawnPoint = team.RespawnPoints().stream()
                    .filter(point -> point.getRuntimeId() == packet.respawnPointId)
                    .findFirst().orElse(null);
            if (respawnPoint != null) {
                player.SelectRespawnPoint(respawnPoint);
            }
            RespawnPointsSync.sendTo(sender);
            // For a player whose waiting is over, choosing a point is the confirmation: they come back there
            if (respawnPoint != null) {
                com.dod.unrealzaruba.Gamemodes.GamemodeManager.instance.ForGamemode(gamemode -> gamemode.confirmRespawn(sender));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
