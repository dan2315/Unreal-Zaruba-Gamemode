package com.dod.UnrealZaruba.Events;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.dod.UnrealZaruba.UI.ModMenus;

import static com.dod.UnrealZaruba.UnrealZaruba.LOGGER;
import com.dod.UnrealZaruba.UI.TimerOverlay;
import com.dod.UnrealZaruba.UI.VehiclePurchaseMenu.VehiclePurchaseScreen;
import net.minecraft.client.gui.screens.MenuScreens;


public class ModSetupEvents {
    
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        LOGGER.info("[UnrealZaruba] Рисуюсь нахуй");
        event.registerAboveAll("gametimer", TimerOverlay.TIMER_OVERLAY);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[UnrealZaruba] Client setup");
            MenuScreens.register(ModMenus.VEHICLE_PURCHASE_MENU.get(), VehiclePurchaseScreen::new);
        });
    }
}
