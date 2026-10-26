package com.dod.unrealzaruba.ModIntegrations.Xaero;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
import xaero.map.MapProcessor;
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
    private static final String PACK_MARKER = ".unrealzaruba-pack";

    private final XaeroWaypoints waypoints;
    private boolean openedForRespawn;

    public XaeroWorldMap(XaeroWaypoints waypoints) {
        this.waypoints = waypoints;
        NeoForge.EVENT_BUS.addListener(this::onScreenRender);
        NeoForge.EVENT_BUS.addListener(this::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    /** Puts the vehicle diamonds on the world map as soon as Xaero has made its element list. */
    public boolean ensureVehicleLayer() {
        return VehicleMapLayer.register();
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

    // ---- which map the game dimension shows ----
    //
    // Xaero keeps a dimension's map per multiworld, in a folder mw$<id> named after the level id the server sends,
    // and switches folders (saving the old map, reading the new one from disk) as soon as that id changes, even while
    // the player stays in the dimension. Each map pack gets its own folder and id, so showing another map is only
    // a matter of handing Xaero another id in the game dimension; the lobby keeps the server's own.

    private Integer packLevelId;   // the installed pack the game dimension shows, or null for the server's own map
    private Integer scanLevelId;   // during /mapscan: the empty map the scan renders into
    private Integer serverLevelId; // the level id Xaero's server part sent
    private boolean settingLevelId;

    /** The level id (and folder mw$<id>) of a pack. Packs stay below 2^28, scans get ids above. */
    public static int packLevelId(String hash) {
        return Integer.parseInt(hash.substring(0, 7), 16);
    }

    private static MapProcessor processor() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        return session == null ? null : session.getMapProcessor();
    }

    private static boolean inGameDimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(XaeroWaypoints.GAME_DIMENSION);
    }

    /** Xaero's folder with the game dimension's map for this server under a level id, or null if not known yet. */
    private static Path gameDimensionFolder(int levelId) {
        MapProcessor processor = processor();
        String worldId = processor == null ? null : processor.getCurrentWorldId();
        if (worldId == null) return null;
        String dimName = processor.getDimensionName(XaeroWaypoints.GAME_DIMENSION);
        return processor.getMapSaveLoad().getMWSubFolder(worldId, dimName, "mw$" + levelId);
    }

    /**
     * Puts a map pack's region files into the pack's own folder for the game dimension and makes it the map shown
     * there. Returns false if Xaero isn't ready yet.
     */
    public boolean installMapPack(Path source, List<String> fileNames, String hash) {
        try {
            Path target = gameDimensionFolder(packLevelId(hash));
            if (target == null) return false;

            Path marker = target.resolve(PACK_MARKER);
            if (!Files.exists(marker) || !hash.equals(Files.readString(marker, StandardCharsets.UTF_8).trim())) {
                Files.createDirectories(target);
                for (String name : fileNames) {
                    Files.copy(source.resolve(name), target.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.writeString(marker, hash, StandardCharsets.UTF_8);
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Installed world map pack {} into {}", hash, target);
            }
            packLevelId = packLevelId(hash);
            return true;
        } catch (Throwable e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Could not install world map pack", e);
            return false;
        }
    }

    /** The server has no pack for the running map: the game dimension shows the server's own map again. */
    public void clearMapPack() {
        packLevelId = null;
    }

    /** /mapscan: the game dimension's map switches to a new empty folder, so the scan holds only what it renders. */
    public void startScan() {
        scanLevelId = 0x40000000 | java.util.concurrent.ThreadLocalRandom.current().nextInt(0x10000000);
    }

    /** The folder the running scan renders into, or null if no scan is running. */
    public Path scanFolder() {
        return scanLevelId == null ? null : gameDimensionFolder(scanLevelId);
    }

    public void endScan() {
        scanLevelId = null;
    }

    private Integer wantedLevelId() {
        if (scanLevelId != null) return scanLevelId;
        return packLevelId != null ? packLevelId : serverLevelId;
    }

    /** Called from the MapProcessor mixin with the level id the server sent: remembered, and swapped for ours in the game dimension. */
    public int onServerLevelId(int id) {
        if (settingLevelId) return id;
        serverLevelId = id;
        Integer wanted = wantedLevelId();
        return wanted != null && inGameDimension() ? wanted : id;
    }

    /** Every tick: in the game dimension Xaero is moved to the map it should show when that changed. */
    private void tickLevelId() {
        if (!inGameDimension()) return;
        try {
            Integer wanted = wantedLevelId();
            MapProcessor processor = processor();
            var data = xaero.map.mcworld.WorldMapClientWorldDataHelper.getCurrentWorldData();
            if (wanted == null || processor == null || data == null || wanted.equals(data.serverLevelId)) return;
            settingLevelId = true;
            try {
                processor.onServerLevelId(wanted);
            } finally {
                settingLevelId = false;
            }
            UnrealZaruba.LOGGER.info("[UnrealZaruba] World map of the game dimension switched to mw${}", wanted);
        } catch (Throwable e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Could not switch the world map", e);
        }
    }

    /** Disconnected: the next server says again what to show. */
    public void resetLevelIds() {
        packLevelId = null;
        scanLevelId = null;
        serverLevelId = null;
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
            options.add(info(element.getName(), ChatFormatting.WHITE, options.size(), target));
        }

        RespawnPointsView view = RespawnClientState.view();
        if (respawnId == null) {
            options.add(label("hud.unrealzaruba.respawn.not_available", options.size(), target));
        } else if (!RespawnClientState.isChoosing() && view.effectiveId() != null && view.effectiveId().byteValue() == respawnId) {
            options.add(info("hud.unrealzaruba.respawn.current", ChatFormatting.GREEN, options.size(), target));
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

    /**
     * Xaero's menu of the empty map, cut down to the lines that show the coordinates (they start with X:). Other lines
     * (chunk, waypoints, teleport, sharing, export, settings) are left out, and the ones kept are renumbered.
     */
    public static ArrayList<RightClickOption> coordinatesOnly(ArrayList<RightClickOption> xaeroOptions, IRightClickableElement target) {
        ArrayList<RightClickOption> options = new ArrayList<>();
        for (RightClickOption option : xaeroOptions) {
            String text = option.getDisplayName().getString();
            if (text.startsWith("X:")) options.add(info(text.replace("%", "%%"), ChatFormatting.AQUA, options.size(), target));
        }
        return options;
    }

    /**
     * A line that only tells something (the name of a waypoint, coordinates, the current respawn point). It is drawn in its own colour like an
     * option that can be used, not greyed out the way Xaero draws a disabled one; choosing it does nothing.
     */
    private static RightClickOption info(String text, ChatFormatting color, int index, IRightClickableElement target) {
        return new RightClickOption(text, Style.EMPTY.withColor(color), index, target) {
            @Override
            public void onAction(Screen screen) {
            }
        };
    }

    /** A line that is greyed out: something that is not available. */
    private static RightClickOption label(String text, int index, IRightClickableElement target) {
        return new RightClickOption(text, index, target) {
            @Override
            public void onAction(Screen screen) {
            }
        }.setActive(false);
    }

    /** While the player waits to respawn the map stays open: Esc and the map key do not close it. */
    private void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof GuiMap) || !RespawnClientState.isChoosing()) return;
        if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            event.setCanceled(true);
            return;
        }
        for (var mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getName().equals("gui.xaero_open_map") && mapping.matches(event.getKeyCode(), event.getScanCode())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** If the map got closed anyway (another screen, a reload) while waiting, it is opened again. */
    private void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        tickLevelId();
        Minecraft mc = Minecraft.getInstance();
        if (!RespawnClientState.isChoosing() || mc.screen != null || mc.player == null || mc.level == null) return;
        if (mc.level.getGameTime() % 20 != 0) return;
        openForRespawn();
    }

    private void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof GuiMap)) return;
        RespawnPointsView view = RespawnClientState.view();
        if (!RespawnClientState.isDead() && view.points().isEmpty()) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = com.dod.unrealzaruba.UI.UiFont.get();
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (RespawnClientState.isDead()) {
            lines.add(Component.translatable("hud.unrealzaruba.respawn.choose_now").getString().toUpperCase());
            colors.add(HudStyle.ACCENT);
        } else {
            lines.add(Component.translatable("hud.unrealzaruba.respawn.point", describe(view.effective())).getString());
            colors.add(HudStyle.TEXT);
        }
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
