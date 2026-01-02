package com.dod.unrealzaruba.Events;

import com.dod.unrealzaruba.Renderers.GeometryRenderer;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.ScoreOverlay;
import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.dod.unrealzaruba.UI.ModMenus;

import static com.dod.unrealzaruba.UnrealZaruba.geometryRenderer;

import com.dod.unrealzaruba.UI.TimerOverlay;
import com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseScreen;
import net.minecraft.client.gui.screens.MenuScreens;


public class ModSetupEvents {
    
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("gametimer", TimerOverlay.INSTANCE);
        event.registerAboveAll("objectives", ObjectivesOverlay.INSTANCE);
        event.registerAboveAll("score", ScoreOverlay.INSTANCE);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Client setup");
            geometryRenderer = new GeometryRenderer();
            MinecraftForge.EVENT_BUS.register(geometryRenderer);
            MenuScreens.register(ModMenus.VEHICLE_PURCHASE_MENU.get(), VehiclePurchaseScreen::new);
        });
    }
}
