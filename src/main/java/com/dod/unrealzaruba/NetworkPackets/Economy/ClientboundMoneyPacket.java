package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.BuyMenu.ClientEconomyHandlers;

import net.minecraft.network.FriendlyByteBuf;

public class ClientboundMoneyPacket implements ModPacket {
    private final boolean visible;
    private final int balance;

    public ClientboundMoneyPacket(boolean visible, int balance) {
        this.visible = visible;
        this.balance = balance;
    }

    public static void encode(ClientboundMoneyPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.visible);
        buf.writeVarInt(msg.balance);
    }

    public static ClientboundMoneyPacket decode(FriendlyByteBuf buf) {
        return new ClientboundMoneyPacket(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(ClientboundMoneyPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientEconomyHandlers.setMoney(msg.visible, msg.balance));
        ctx.get().setPacketHandled(true);
    }
}
