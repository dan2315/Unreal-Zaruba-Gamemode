package com.dod.unrealzaruba.ModIntegrations.Xaero;

import java.util.Iterator;

import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.Vehicles.TeamVehiclesState;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import xaero.map.WorldMap;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/**
 * The team's vehicles on Xaero's world map: a big diamond in the team's colour with the vehicle's name under it,
 * drawn the way the map draws entities (through Xaero's own element renderer list), not as waypoints.
 */
final class VehicleMapLayer {
    private static final int RADIUS = 16;
    private static boolean registered;

    private VehicleMapLayer() {}

    /** Adds the layer once Xaero's world map has made its renderer list. Returns true when it is in. */
    static boolean register() {
        if (registered) return true;
        try {
            if (WorldMap.mapElementRenderHandler == null) return false;
            Context context = new Context();
            Provider provider = new Provider();
            Reader reader = new Reader();
            WorldMap.mapElementRenderHandler.add(new Renderer(context, provider, reader));
            registered = true;
            UnrealZaruba.LOGGER.info("[VehicleMapLayer] Vehicles are drawn on Xaero's world map");
            return true;
        } catch (Throwable e) {
            registered = true; // do not retry every tick
            UnrealZaruba.LOGGER.warn("[VehicleMapLayer] Could not add the vehicle layer to Xaero's world map: {}", e.toString());
            return false;
        }
    }

    /** Nothing to carry between the elements. */
    private static final class Context {}

    private static final class Provider extends ElementRenderProvider<TeamVehiclesState.Shown, Context> {
        private Iterator<TeamVehiclesState.Shown> iterator;

        @Override
        public void begin(ElementRenderLocation location, Context context) {
            iterator = TeamVehiclesState.all().iterator();
        }

        @Override
        public boolean hasNext(ElementRenderLocation location, Context context) {
            return iterator != null && iterator.hasNext();
        }

        @Override
        public TeamVehiclesState.Shown getNext(ElementRenderLocation location, Context context) {
            return iterator.next();
        }

        @Override
        public void end(ElementRenderLocation location, Context context) {
            iterator = null;
        }
    }

    private static final class Reader extends ElementReader<TeamVehiclesState.Shown, Context, Renderer> {
        @Override public boolean isHidden(TeamVehiclesState.Shown vehicle, Context context) { return false; }
        @Override public double getRenderX(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return vehicle.x(); }
        @Override public double getRenderZ(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return vehicle.z(); }
        @Override public int getInteractionBoxLeft(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return -RADIUS; }
        @Override public int getInteractionBoxRight(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return RADIUS; }
        @Override public int getInteractionBoxTop(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return -RADIUS; }
        @Override public int getInteractionBoxBottom(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return RADIUS; }
        @Override public int getRenderBoxLeft(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return -RADIUS - 2; }
        @Override public int getRenderBoxRight(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return RADIUS + 2; }
        @Override public int getRenderBoxTop(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return -RADIUS - 2; }
        @Override public int getRenderBoxBottom(TeamVehiclesState.Shown vehicle, Context context, float partialTicks) { return RADIUS + 2; }
        @Override public int getLeftSideLength(TeamVehiclesState.Shown vehicle, Minecraft minecraft) { return 0; }
        @Override public String getMenuName(TeamVehiclesState.Shown vehicle) { return vehicle.name(); }
        @Override public String getFilterName(TeamVehiclesState.Shown vehicle) { return vehicle.name(); }
        @Override public int getMenuTextFillLeftPadding(TeamVehiclesState.Shown vehicle) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(TeamVehiclesState.Shown vehicle) { return HudStyle.teamColor(vehicle.team()); }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return true; }
        /** The diamond is only to look at: no hover, no right-click menu. */
        @Override public boolean isInteractable(ElementRenderLocation location, TeamVehiclesState.Shown vehicle) { return false; }
    }

    private static final class Renderer extends ElementRenderer<TeamVehiclesState.Shown, Context, Renderer> {
        Renderer(Context context, Provider provider, Reader reader) {
            super(context, provider, reader);
        }

        @Override
        public void preRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider textures, boolean shadow) {}

        @Override
        public void postRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider textures, boolean shadow) {}

        @Override
        public void renderElementShadow(TeamVehiclesState.Shown vehicle, boolean hovered, float optionalScale, double partialX, double partialY,
                                        ElementRenderInfo info, GuiGraphics graphics, MultiBufferSource.BufferSource buffers,
                                        MultiTextureRenderTypeRendererProvider textures) {}

        @Override
        public boolean renderElement(TeamVehiclesState.Shown vehicle, boolean hovered, double optionalDepth, float optionalScale,
                                     double partialX, double partialY, ElementRenderInfo info, GuiGraphics graphics,
                                     MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider textures) {
            // Xaero places the icon on whole pixels and passes the rest in partialX / partialY: adding it back lets the
            // diamond move smoothly instead of in steps
            graphics.pose().pushPose();
            graphics.pose().translate((float) partialX, (float) partialY, 0f);
            int color = HudStyle.teamColor(vehicle.team());
            // A black rim, then the diamond in the team colour, a smaller light one in the middle
            diamond(graphics, RADIUS + 2, 0xFF000000);
            diamond(graphics, RADIUS, color);
            diamond(graphics, RADIUS / 3, 0xFFF2F5F3);
            graphics.pose().popPose();
            return true;
        }

        @Override
        public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
            return location == ElementRenderLocation.WORLD_MAP && !shadow;
        }

        private static void diamond(GuiGraphics graphics, int radius, int color) {
            for (int dy = -radius; dy <= radius; dy++) {
                int half = radius - Math.abs(dy);
                graphics.fill(-half, dy, half + 1, dy + 1, color);
            }
        }
    }
}
