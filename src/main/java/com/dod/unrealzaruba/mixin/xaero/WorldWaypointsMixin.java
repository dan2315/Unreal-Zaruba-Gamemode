package com.dod.unrealzaruba.mixin.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;

/**
 * On the game map the waypoints are not drawn in the world, so they do not clutter the view; they appear while Alt is
 * held. Only the drawing is skipped: Xaero's settings and the minimap are left alone.
 *
 * {@code shouldRender} is the renderer's own switch (it reads the "waypoints in world" option), so this is the
 * place that is consulted for every frame.
 */
@Mixin(value = WaypointWorldRenderer.class, remap = false)
public class WorldWaypointsMixin {
    private static final ResourceLocation GAME_DIM = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "game_dim");

    @Inject(method = "shouldRender(Lxaero/hud/minimap/element/render/MinimapElementRenderLocation;)Z", at = @At("HEAD"), cancellable = true)
    private void unrealzaruba$onlyWithTab(MinimapElementRenderLocation location, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && GAME_DIM.equals(minecraft.level.dimension().location())
                && !net.minecraft.client.gui.screens.Screen.hasAltDown()) {
            cir.setReturnValue(false);
        }
    }
}
