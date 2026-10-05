package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.BuyMenu.ClientEconomyHandlers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public class BuyResultPacket implements ModPacket {
    private final boolean success;
    private final Component message;

    public BuyResultPacket(boolean success, Component message) {
        this.success = success;
        this.message = message;
    }

    public static void encode(BuyResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg.message);
    }

    public static BuyResultPacket decode(FriendlyByteBuf buf) {
        return new BuyResultPacket(buf.readBoolean(), ComponentSerialization.TRUSTED_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
    }

    public static void handle(BuyResultPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientEconomyHandlers.showBuyResult(msg.success, msg.message));
        ctx.get().setPacketHandled(true);
    }
}
