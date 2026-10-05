package com.dod.unrealzaruba.ModIntegrations.Xaero;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.RespawnPointsView;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.Objectives.ObjectivesOverlay;
import com.dod.unrealzaruba.UI.Respawn.ClientRespawnHandlers;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.Waypoint;

/**
 * Xaero's World Map as the respawn picker: it opens on death with a panel showing the countdown and the chosen point,
 * and right-clicking one of our waypoints offers "choose as respawn point". Players without OP get only our options
 * in waypoint menus and no menu on the empty map (no Xaero teleport / waypoint editing).
 */
public class XaeroWorldMap {
    private static final int PANEL_WIDTH = 220;

    private final XaeroWaypoints waypoints;
    private boolean openedForRespawn;

    public XaeroWorldMap(XaeroWaypoints waypoints) {
        this.waypoints = waypoints;
        NeoForge.EVENT_BUS.addListener(this::onScreenRender);
    }

    public static boolean isOp() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasPermissions(2);
    }

    public boolean openForRespawn() {
        try {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            Minecraft mc = Minecraft.getInstance();
            if (session == null || session.getMapProcessor() == null || mc.getCameraEntity() == null) return false;
            mc.setScreen(new GuiMap(null, null, session.getMapProcessor(), mc.getCameraEntity()));
            openedForRespawn = true;
            return true;
        } catch (Throwable e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Could not open Xaero's world map for respawn", e);
            return false;
        }
    }

    public void closeAfterRespawn() {
        Minecraft mc = Minecraft.getInstance();
        if (openedForRespawn && mc.screen instanceof GuiMap) {
            mc.setScreen(null);
        }
        openedForRespawn = false;
    }

    public void refresh() {
        if (SupportMods.minimap() && SupportMods.xaeroMinimap != null) {
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
        }
    }

    /** Called from the WaypointReader mixin with Xaero's own options for a waypoint. */
    public ArrayList<RightClickOption> waypointOptions(Waypoint element, IRightClickableElement target, ArrayList<RightClickOption> xaeroOptions) {
        Byte respawnId = waypoints.respawnIdFor(element.getOriginal());
        boolean ours = waypoints.isOurs(element.getOriginal());
        if (isOp() && !ours) {
            return xaeroOptions;
        }

        ArrayList<RightClickOption> options = new ArrayList<>();
        if (isOp()) {
            options.addAll(xaeroOptions);
        } else {
            options.add(label(element.getName(), options.size(), target));
        }

        RespawnPointsView view = RespawnClientState.view();
        if (respawnId == null) {
            options.add(label("hud.unrealzaruba.respawn.not_available", options.size(), target));
        } else if (view.effectiveId() != null && view.effectiveId().byteValue() == respawnId) {
            options.add(label("hud.unrealzaruba.respawn.current", options.size(), target));
        } else {
            byte id = respawnId;
            options.add(new RightClickOption("hud.unrealzaruba.respawn.choose", Style.EMPTY.withColor(ChatFormatting.GOLD), options.size(), target) {
                @Override
                public void onAction(Screen screen) {
                    ClientRespawnHandlers.select(id);
                }
            });
        }
        return options;
    }

    private static RightClickOption label(String text, int index, IRightClickableElement target) {
        return new RightClickOption(text, index, target) {
            @Override
            public void onAction(Screen screen) {
            }
        }.setActive(false);
    }

    private void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof GuiMap)) return;
        RespawnPointsView view = RespawnClientState.view();
        if (!RespawnClientState.isDead() && view.points().isEmpty()) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (RespawnClientState.isDead()) {
            lines.add(Component.translatable("hud.unrealzaruba.respawn.in", Math.max(0, RespawnClientState.secondsLeft())).getString().toUpperCase());
            colors.add(HudStyle.ACCENT);
        }
        lines.add(Component.translatable("hud.unrealzaruba.respawn.point", describe(view.effective())).getString());
        colors.add(HudStyle.TEXT);
        lines.add(Component.translatable("hud.unrealzaruba.respawn.hint").getString());
        colors.add(HudStyle.MUTED);

        int width = PANEL_WIDTH;
        for (String line : lines) {
            width = Math.max(width, font.width(line) + 16);
        }
        int height = lines.size() * (font.lineHeight + 3) + 9;
        int x = (event.getScreen().width - width) / 2;
        int y = HudStyle.MARGIN + 18;
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        HudStyle.panel(g, x, y, width, height);
        int lineY = y + 6;
        for (int i = 0; i < lines.size(); i++) {
            g.drawCenteredString(font, lines.get(i), x + width / 2, lineY, colors.get(i));
            lineY += font.lineHeight + 3;
        }
        g.pose().popPose();
    }

    static String describe(RespawnPointsView.Entry entry) {
        return RespawnClientState.describe(entry);
    }
}
