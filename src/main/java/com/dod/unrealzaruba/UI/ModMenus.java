package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseMenu;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, UnrealZaruba.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<VehiclePurchaseMenu>> VEHICLE_PURCHASE_MENU = MENUS.register("vehicle_purchase_menu",
     () -> IMenuTypeExtension.create(VehiclePurchaseMenu::new));

     public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Menus registered");
    }
}

