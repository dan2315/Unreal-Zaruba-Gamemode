package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.UI.BuyMenu.ClientEconomyHandlers;

import net.minecraft.network.FriendlyByteBuf;

/** The shop and team storage hints under the money: whether they are shown, and whether each works where the player stands. */
public class ClientboundZoneHintsPacket implements ModPacket {
    private final boolean shown;
    private final boolean shopEnabled;
    private final boolean shopOpen;
    private final boolean storageOpen;

    public ClientboundZoneHintsPacket(boolean shown, boolean shopEnabled, boolean shopOpen, boolean storageOpen) {
        this.shown = shown;
        this.shopEnabled = shopEnabled;
        this.shopOpen = shopOpen;
        this.storageOpen = storageOpen;
    }

    public static void encode(ClientboundZoneHintsPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.shown);
        buf.writeBoolean(msg.shopEnabled);
        buf.writeBoolean(msg.shopOpen);
        buf.writeBoolean(msg.storageOpen);
    }

    public static ClientboundZoneHintsPacket decode(FriendlyByteBuf buf) {
        return new ClientboundZoneHintsPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ClientboundZoneHintsPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ClientEconomyHandlers.setZoneHints(msg.shown, msg.shopEnabled, msg.shopOpen, msg.storageOpen));
        ctx.get().setPacketHandled(true);
    }
}
