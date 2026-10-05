package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.Economy.BuyMenuView;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.BuyMenu.ClientEconomyHandlers;

import net.minecraft.network.FriendlyByteBuf;

public class OpenBuyMenuPacket implements ModPacket {
    private final BuyMenuView view;

    public OpenBuyMenuPacket(BuyMenuView view) {
        this.view = view;
    }

    public static void encode(OpenBuyMenuPacket msg, FriendlyByteBuf buf) {
        msg.view.encode(buf);
    }

    public static OpenBuyMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenBuyMenuPacket(BuyMenuView.decode(buf));
    }

    public static void handle(OpenBuyMenuPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientEconomyHandlers.openBuyMenu(msg.view));
        ctx.get().setPacketHandled(true);
    }
}
