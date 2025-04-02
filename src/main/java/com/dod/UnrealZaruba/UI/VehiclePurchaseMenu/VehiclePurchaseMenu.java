package com.dod.UnrealZaruba.UI.VehiclePurchaseMenu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Tuple;
import com.dod.UnrealZaruba.ModBlocks.BlockEntities.VehiclePurchaseBlockEntity;
import com.dod.UnrealZaruba.UI.ModMenus;
import com.dod.UnrealZaruba.Vehicles.VehicleData;
import com.dod.UnrealZaruba.Vehicles.VehicleRegistry;
import com.dod.UnrealZaruba.VsIntegration.ShipCreator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.FriendlyByteBuf;

public class VehiclePurchaseMenu extends AbstractContainerMenu {

    private final VehiclePurchaseBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    
    public VehiclePurchaseMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, 
            (VehiclePurchaseBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
            ContainerLevelAccess.NULL);
    }

    public VehiclePurchaseMenu(int containerId, Inventory playerInventory, VehiclePurchaseBlockEntity blockEntity, ContainerLevelAccess levelAccess) {
        super(ModMenus.VEHICLE_PURCHASE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.levelAccess = levelAccess;
    }

    public void setSelectedVehicle(String selectedVehicle) {
        this.blockEntity.setSelectedVehicle(selectedVehicle);
    }

    public String getSelectedVehicle() {
        return this.blockEntity.getSelectedVehicle();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, this.blockEntity.getBlockState().getBlock());
    }

    public Tuple<Boolean, String> purchaseVehicle(Player player) {
        String selectedVehicle = this.blockEntity.getSelectedVehicle();

        if (selectedVehicle == null) {
            return new Tuple<>(false, "No vehicle selected");
        }

        VehicleData vehicleData = VehicleRegistry.getVehicle(selectedVehicle);

        if (vehicleData == null) {
            return new Tuple<>(false, "Vehicle not found");
        }

        if (!vehicleData.consumeRequiredItems(player.getInventory())) {
            return new Tuple<>(false, "Not enough items");
        }

        levelAccess.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                Direction direction = serverLevel.getBlockState(pos).getValue(BlockStateProperties.FACING);
                ShipCreator.CreateShipFromTemplate(pos, vehicleData.getSchematicLocation(), serverLevel, direction);
            }
        });

        return new Tuple<>(true, "Vehicle purchased");
    }
}
