package com.dod.UnrealZaruba.Events;

import com.dod.UnrealZaruba.Renderers.GeometryRenderer;
import com.dod.UnrealZaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.dod.UnrealZaruba.UI.ModMenus;

import static com.dod.UnrealZaruba.UnrealZaruba.LOGGER;
import static com.dod.UnrealZaruba.UnrealZaruba.geometryRenderer;

import com.dod.UnrealZaruba.UI.TimerOverlay;
import com.dod.UnrealZaruba.UI.VehiclePurchaseMenu.VehiclePurchaseScreen;
import net.minecraft.client.gui.screens.MenuScreens;


public class ModSetupEvents {
    
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        LOGGER.info("[UnrealZaruba] Рисуюсь нахуй");
        event.registerAboveAll("gametimer", TimerOverlay.INSTANCE);
        event.registerAboveAll("objectives", ObjectivesOverlay.INSTANCE);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[UnrealZaruba] Client setup");
            geometryRenderer = new GeometryRenderer();
            MinecraftForge.EVENT_BUS.register(geometryRenderer);
            MenuScreens.register(ModMenus.VEHICLE_PURCHASE_MENU.get(), VehiclePurchaseScreen::new);
        });
    }
}
