package com.dod.unrealzaruba.ModBlocks.VehiclePurchase;

import net.minecraft.core.HolderLookup;
import com.dod.unrealzaruba.ModBlocks.ModBlocks;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.Vehicles.VehicleData;
import com.dod.unrealzaruba.Vehicles.VehicleRegistry;
import com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseMenu;
import com.dod.unrealzaruba.SableIntegration.ShipCreator;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;

public class VehiclePurchaseBlockEntity extends BlockEntity implements MenuProvider {

    private ResourceLocation schematicLocation;
    private String selectedVehicle;
    private int cooldownTicks = 0;

    public VehiclePurchaseBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlocks.VEHICLE_PURCHASE_BLOCK_ENTITY.get(), blockPos, blockState);
    }
    
    public String getSelectedVehicle() {
        return selectedVehicle;
    }

    public void setSelectedVehicle(String selectedVehicle) {
        this.selectedVehicle = selectedVehicle;
        this.schematicLocation = VehicleRegistry.getVehicle(selectedVehicle).getSchematicLocation();
        setChanged();

        if (level != null && !level.isClientSide) {
            SendUpdatedSchematicToClient();
        }
    }

    private void SendUpdatedSchematicToClient() {
        if (level instanceof ServerLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag, registries);
        if (schematicLocation != null) {
            compoundTag.putString("schematicLocation", schematicLocation.toString());
        }
        if (selectedVehicle != null) {
            compoundTag.putString("selectedVehicle", selectedVehicle);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        if (compoundTag.contains("schematicLocation")) {
            schematicLocation = ResourceLocation.parse(compoundTag.getString("schematicLocation"));
        }
        if (compoundTag.contains("selectedVehicle")) {
            selectedVehicle = compoundTag.getString("selectedVehicle");
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vehicle_purchase");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VehiclePurchaseMenu(containerId, inventory, this, ContainerLevelAccess.create(level, worldPosition));
    }

    public Tuple<Boolean, String> purchaseVehicle(ServerPlayer player) {
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Purchasing vehicle: {}", selectedVehicle);
        if (selectedVehicle == null) {
            return new Tuple<>(false, "No vehicle selected");
        }
    
        VehicleData vehicleData = VehicleRegistry.getVehicle(selectedVehicle);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Vehicle data: {}", vehicleData);
        if (vehicleData == null) {
            return new Tuple<>(false, "Invalid vehicle selected");
        }
    

        if (!vehicleData.hasRequiredItems(player.getInventory())) {
            player.sendSystemMessage(Component.translatable("container.vehicle_purchase.missing_items"));
            return new Tuple<>(false, "Missing items");
        }
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Consuming items");
    
        if (cooldownTicks > 0) {
            player.sendSystemMessage(Component.translatable("container.vehicle_purchase.cooldown")
                    .append(" (" + (cooldownTicks / 20) + " seconds)"));
            return new Tuple<>(false, "Cooldown");
        }
        
    
        if (level instanceof ServerLevel serverLevel) {
            Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
            BlockPos targetPos = worldPosition.relative(facing, -3);
            boolean success = ShipCreator.CreateShipFromTemplate(vehicleData.getSchematicLocation(), targetPos, facing, serverLevel, player);
            if (success) {
                cooldownTicks = 600;
                
                vehicleData.consumeRequiredItems(player.getInventory());
                return new Tuple<>(true, "Vehicle deployed successfully");
            } else {
                return new Tuple<>(false, "Error: Failed to deploy vehicle");
            }
        } else {
            return new Tuple<>(false, "Error: Not on server level");
        }
    }

    public void serverTick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }
}