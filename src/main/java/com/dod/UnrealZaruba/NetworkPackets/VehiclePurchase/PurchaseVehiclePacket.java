package com.dod.UnrealZaruba.NetworkPackets.VehiclePurchase;

import com.dod.UnrealZaruba.ModBlocks.BlockEntities.VehiclePurchaseBlockEntity;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PurchaseVehiclePacket {
    private final BlockPos blockPos;
    private final String vehicleName;

    public PurchaseVehiclePacket(BlockPos blockPos, String vehicleName) {
        this.blockPos = blockPos;
        this.vehicleName = vehicleName;
    }

    public static void encode(PurchaseVehiclePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos);
        buf.writeUtf(msg.vehicleName);
    }

    public static PurchaseVehiclePacket decode(FriendlyByteBuf buf) {
        return new PurchaseVehiclePacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(PurchaseVehiclePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Received purchase request for vehicle: {} at {}", 
                    msg.vehicleName, msg.blockPos);
                try {
                    BlockEntity blockEntity = player.level().getBlockEntity(msg.blockPos);
                    if (blockEntity instanceof VehiclePurchaseBlockEntity vehiclePurchaseBlockEntity) {
                        UnrealZaruba.LOGGER.info("[UnrealZaruba] Setting selected vehicle to: {}", msg.vehicleName);
                        vehiclePurchaseBlockEntity.setSelectedVehicle(msg.vehicleName);
                        Tuple<Boolean, String> result = vehiclePurchaseBlockEntity.purchaseVehicle(player);
                        
                        NetworkHandler.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new PurchaseResultPacket(result.getA(), result.getB())
                        );
                    }
                } catch (Exception e) {
                    UnrealZaruba.LOGGER.error("[UnrealZaruba] Error purchasing vehicle", e);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}