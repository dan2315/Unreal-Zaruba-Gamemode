package com.dod.unrealzaruba.NetworkPackets.CharacterClasses;

import com.dod.unrealzaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetClassPacket {
    private final BlockPos blockPos;
    private final String classId;

    public SetClassPacket(BlockPos blockPos, String classId) {
        this.blockPos = blockPos;
        this.classId = classId;
    }

    public static void encode(SetClassPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.classId);
    }

    public static SetClassPacket decode(FriendlyByteBuf buffer) {
        return new SetClassPacket(
            buffer.readBlockPos(),
            buffer.readUtf()
        );
    }

    public static void handle(SetClassPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) { // Only OPs can set class
                BlockEntity blockEntity = player.level().getBlockEntity(packet.blockPos);
                if (blockEntity instanceof ClassAssignerBlockEntity classAssigner) {
                    classAssigner.setClassId(packet.classId);
                    // Update the armor stand to reflect the new class
                    classAssigner.updateArmorStand();
                }
            }
        });
        context.setPacketHandled(true);
    }
}