package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The guide on uploading a schematic offers the Tool Gun, which saves a vehicle as a blueprint: this puts one in the player's hand
 * (or inventory) unless they already carry one. Only in a single-player world: a dedicated server never gives it out.
 */
public class GiveToolGunPacket implements ModPacket {
    private static final ResourceLocation TOOL_GUN = ResourceLocation.fromNamespaceAndPath("create_aeronautics_toolgun", "structure_tool");

    public static void encode(GiveToolGunPacket msg, FriendlyByteBuf buf) {}

    public static GiveToolGunPacket decode(FriendlyByteBuf buf) {
        return new GiveToolGunPacket();
    }

    public static void handle(GiveToolGunPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // Only in a single-player world: on a server the Tool Gun is not given out
            if (player.getServer() == null || player.getServer().isDedicatedServer()) {
                player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.singleplayer_only"), true);
                return;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(TOOL_GUN).orElse(null);
            if (item == null) {
                player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.toolgun_missing"), true);
                return;
            }
            if (player.getInventory().contains(new ItemStack(item))) {
                player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.toolgun_have"), true);
                return;
            }
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.toolgun_given"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
