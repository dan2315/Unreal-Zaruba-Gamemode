package com.dod.UnrealZaruba.NetworkPackets;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.CommanderSystem.CommanderSystem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class VotePlayerPacket {

    private final UUID uuid;

    public VotePlayerPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public static void encode(VotePlayerPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.getUuid());
    }

    public static VotePlayerPacket decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        return new VotePlayerPacket(uuid);
    }

    public static void handle(VotePlayerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer != null) {
                UUID votedPlayerUUID = msg.getUuid();
                UnrealZaruba.LOGGER.warn("Recieved message -> vote from " + serverPlayer.getName().getString());
                CommanderSystem.ProcessCommanderVote(serverPlayer.getUUID(), votedPlayerUUID);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
