package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseMenu;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, UnrealZaruba.MOD_ID);

    public static final RegistryObject<MenuType<VehiclePurchaseMenu>> VEHICLE_PURCHASE_MENU = MENUS.register("vehicle_purchase_menu",
     () -> IForgeMenuType.create(VehiclePurchaseMenu::new));

     public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Menus registered");
    }
}

