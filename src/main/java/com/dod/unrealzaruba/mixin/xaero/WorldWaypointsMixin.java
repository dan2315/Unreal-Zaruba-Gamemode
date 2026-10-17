package com.dod.unrealzaruba.mixin.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderContext;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderProvider;

/**
 * On the game map the waypoints are not drawn in the world, so they do not clutter the view; they appear while the
 * player list key (TAB) is held. Only the drawing is skipped: Xaero's settings and the minimap are left alone.
 */
@Mixin(value = WaypointWorldRenderProvider.class, remap = false)
public class WorldWaypointsMixin {
    private static final ResourceLocation GAME_DIM = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "game_dim");

    @Inject(method = "hasNext(ILxaero/hud/minimap/waypoint/render/world/WaypointWorldRenderContext;)Z", at = @At("HEAD"), cancellable = true)
    private void unrealzaruba$onlyWithTab(int location, WaypointWorldRenderContext context, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && GAME_DIM.equals(minecraft.level.dimension().location())
                && !minecraft.options.keyPlayerList.isDown()) {
            cir.setReturnValue(false);
        }
    }
}
