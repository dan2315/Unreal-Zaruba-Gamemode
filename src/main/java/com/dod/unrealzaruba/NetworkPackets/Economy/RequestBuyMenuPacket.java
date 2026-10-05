package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Economy.BuyMenuService;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Buy menu key pressed. The server answers with {@link OpenBuyMenuPacket} if the player may buy. */
public class RequestBuyMenuPacket implements ModPacket {
    public static void encode(RequestBuyMenuPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestBuyMenuPacket decode(FriendlyByteBuf buf) {
        return new RequestBuyMenuPacket();
    }

    public static void handle(RequestBuyMenuPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BuyMenuService.open(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
