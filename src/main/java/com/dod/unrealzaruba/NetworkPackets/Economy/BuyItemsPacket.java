package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.dod.unrealzaruba.Economy.BuyMenuService;
import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Entries the player wants from one category, by their indices in the map's buy menu config. */
public class BuyItemsPacket implements ModPacket {
    private static final int MAX_ITEMS = 64;

    private final int tab;
    private final int group;
    private final int category;
    private final List<Integer> items;

    public BuyItemsPacket(int tab, int group, int category, List<Integer> items) {
        this.tab = tab;
        this.group = group;
        this.category = category;
        this.items = items;
    }

    public static void encode(BuyItemsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.tab);
        buf.writeVarInt(msg.group);
        buf.writeVarInt(msg.category);
        buf.writeCollection(msg.items, FriendlyByteBuf::writeVarInt);
    }

    public static BuyItemsPacket decode(FriendlyByteBuf buf) {
        int tab = buf.readVarInt();
        int group = buf.readVarInt();
        int category = buf.readVarInt();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ITEMS) {
            throw new IllegalArgumentException("Too many items in buy request: " + count);
        }
        List<Integer> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(buf.readVarInt());
        }
        return new BuyItemsPacket(tab, group, category, items);
    }

    public static void handle(BuyItemsPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BuyMenuService.buy(player, msg.tab, msg.group, msg.category, msg.items);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
