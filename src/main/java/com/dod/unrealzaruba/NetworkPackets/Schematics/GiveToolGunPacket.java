package com.dod.unrealzaruba.NetworkPackets.Schematics;

import java.util.function.Supplier;

import com.dod.unrealzaruba.ModItems.ModItems;
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
 * The guide on uploading a schematic offers the two tools of the work: the Tool Gun, which saves a vehicle as a blueprint, and the
 * Contraption Scanner, which shows its size, price and publishing problems. This puts the chosen one in the player's inventory unless
 * they already carry one. Only in a single-player world: a dedicated server never gives them out.
 */
public class GiveToolGunPacket implements ModPacket {
    public static final byte TOOL_GUN = 0;
    public static final byte SCANNER = 1;
    private static final ResourceLocation TOOL_GUN_ID = ResourceLocation.fromNamespaceAndPath("create_aeronautics_toolgun", "structure_tool");

    private final byte which;

    public GiveToolGunPacket(byte which) {
        this.which = which;
    }

    public static void encode(GiveToolGunPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.which);
    }

    public static GiveToolGunPacket decode(FriendlyByteBuf buf) {
        return new GiveToolGunPacket(buf.readByte());
    }

    public static void handle(GiveToolGunPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // Only in a single-player world: on a server these tools are not given out
            if (player.getServer() == null || player.getServer().isDedicatedServer()) {
                player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.singleplayer_only"), true);
                return;
            }
            Item item = msg.which == SCANNER ? ModItems.CONTRAPTION_SCANNER.get() : BuiltInRegistries.ITEM.getOptional(TOOL_GUN_ID).orElse(null);
            if (item == null) {
                player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.toolgun_missing"), true);
                return;
            }
            ItemStack stack = new ItemStack(item);
            if (player.getInventory().contains(stack)) {
                player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.item_have", stack.getHoverName()), true);
                return;
            }
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            player.displayClientMessage(Component.translatable("schematics.unrealzaruba.guide.item_given", stack.getHoverName()), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
