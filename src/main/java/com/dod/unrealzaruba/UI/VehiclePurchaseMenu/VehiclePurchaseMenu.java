package com.dod.unrealzaruba.UI.VehiclePurchaseMenu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

import com.dod.unrealzaruba.Economy.EconomyConfig;
import com.dod.unrealzaruba.ModBlocks.VehiclePurchase.VehiclePurchaseBlockEntity;
import com.dod.unrealzaruba.UI.ModMenus;
import com.dod.unrealzaruba.Vehicles.VehicleData;
import com.dod.unrealzaruba.Vehicles.VehicleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class VehiclePurchaseMenu extends AbstractContainerMenu {

    /** A vehicle this map sells, keyed like {@link VehicleRegistry} (e.g. "unrealzaruba:car"). */
    public record Offer(String vehicleKey, int price) {}

    private final VehiclePurchaseBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final List<Offer> offers;

    public VehiclePurchaseMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
            (VehiclePurchaseBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
            ContainerLevelAccess.NULL,
            extraData.readList(buf -> new Offer(buf.readUtf(), buf.readVarInt())));
    }

    public VehiclePurchaseMenu(int containerId, Inventory playerInventory, VehiclePurchaseBlockEntity blockEntity, ContainerLevelAccess levelAccess, List<Offer> offers) {
        super(ModMenus.VEHICLE_PURCHASE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.levelAccess = levelAccess;
        this.offers = offers;
    }

    /** Vehicles and prices from the map's economy config. */
    public static List<Offer> offersFor(EconomyConfig config) {
        List<Offer> offers = new ArrayList<>();
        for (String vehicle : config.vehicles) {
            VehicleData data = VehicleRegistry.getVehicle(VehicleRegistry.GetLocation(vehicle).toString());
            if (data != null) {
                offers.add(new Offer(data.getKey().toString(), config.vehiclePrice(vehicle)));
            }
        }
        return offers;
    }

    public static void writeExtraData(FriendlyByteBuf buf, BlockPos pos, List<Offer> offers) {
        buf.writeBlockPos(pos);
        buf.writeCollection(offers, (b, offer) -> {
            b.writeUtf(offer.vehicleKey());
            b.writeVarInt(offer.price());
        });
    }

    public List<Offer> getOffers() {
        return offers;
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
