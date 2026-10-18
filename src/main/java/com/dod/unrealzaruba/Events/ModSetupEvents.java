package com.dod.unrealzaruba.Events;

import com.dod.unrealzaruba.Renderers.GeometryRenderer;
import com.dod.unrealzaruba.UI.Compass.CompassOverlay;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.ScoreOverlay;
import com.dod.unrealzaruba.UI.MoneyOverlay;
import com.dod.unrealzaruba.UI.Respawn.RespawnOverlay;
import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.dod.unrealzaruba.UI.ModMenus;

import static com.dod.unrealzaruba.UnrealZaruba.geometryRenderer;

import com.dod.unrealzaruba.UI.TimerOverlay;
import com.dod.unrealzaruba.UI.VehiclePurchaseMenu.VehiclePurchaseScreen;


public class ModSetupEvents {

    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "gametimer"), TimerOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "objectives"), ObjectivesOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "tips"), com.dod.unrealzaruba.UI.TipsOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "score"), ScoreOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "objectives_strip"), com.dod.unrealzaruba.UI.Objectives.ObjectivesStrip.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "money"), MoneyOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "respawn"), RespawnOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "purchase"), com.dod.unrealzaruba.Schematics.PurchaseOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "lobby"), com.dod.unrealzaruba.UI.Lobby.LobbyOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "contraption_report"), com.dod.unrealzaruba.UI.ContraptionReportOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "compass"), CompassOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "feed"), com.dod.unrealzaruba.UI.Feed.FeedOverlay.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "tablist"), com.dod.unrealzaruba.UI.TabList.TabListOverlay.INSTANCE);
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.VEHICLE_PURCHASE_MENU.get(), VehiclePurchaseScreen::new);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Client setup");
            geometryRenderer = new GeometryRenderer();
            NeoForge.EVENT_BUS.register(geometryRenderer);
        });
    }
}
