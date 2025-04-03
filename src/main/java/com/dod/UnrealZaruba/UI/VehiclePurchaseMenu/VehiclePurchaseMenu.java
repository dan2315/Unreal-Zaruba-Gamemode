package com.dod.UnrealZaruba.UI.VehiclePurchaseMenu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import com.dod.UnrealZaruba.ModBlocks.BlockEntities.VehiclePurchaseBlockEntity;
import com.dod.UnrealZaruba.UI.ModMenus;
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

    public VehiclePurchaseBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, this.blockEntity.getBlockState().getBlock());
    }
}
