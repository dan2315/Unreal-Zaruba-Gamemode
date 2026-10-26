package com.dod.unrealzaruba.MapEditor;

import com.dod.unrealzaruba.ModItems.ModItems;
import com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.UiFont;
import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpecialTextures;

import org.joml.Matrix4f;

import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The map editor in an op's hand. Everything of the open map is drawn: capture areas and target volumes as blue boxes
 * with Create's glue texture (framed in the owner's colour), respawns and team bases as player-sized frames, names
 * above them. Ctrl + scroll picks what the right mouse button edits:
 * <ul>
 * <li>capture points / targets: right-click two blocks for the corners of a new one (as with super glue), right-click
 * a box for its settings, shift + right-click twice to delete it; a point's respawn is placed from its settings;</li>
 * <li>red / blue base: right-click the block the team spawns on.</li>
 * </ul>
 * Left click drops the corner or respawn being placed. The server keeps the files; edits are taken in DEV mode only.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class MapEditorClient {
    enum Mode {
        POINTS("points"), TARGETS("targets"), RED_BASE("red_base"), BLUE_BASE("blue_base");

        final String key;

        Mode(String key) {
            this.key = key;
        }
    }

    private static final double REACH = 160;
    private static final double LABEL_DISTANCE = 256;
    private static final int GLUE_BLUE = 0x3D7BFF;
    private static final int RED = 0xE5484D;
    private static final int BLUE = 0x4C8DFF;
    private static final int NEUTRAL = 0xD9D9D9;
    private static final int PREVIEW = 0x8FDCFF;
    private static final int DELETE = 0xFF4040;
    private static final int DELETE_CONFIRM_TICKS = 60;

    private static EditorState state = EditorState.EMPTY;
    private static Mode mode = Mode.POINTS;
    private static BlockPos pendingCorner;
    private static EditorState.Point respawnFor;
    private static Object deleteArmed;
    private static long deleteArmedAt;
    private static boolean wasHolding;
    private static long requestedAt;

    private MapEditorClient() {}

    public static void setState(EditorState newState) {
        state = newState;
        if (respawnFor != null) {
            respawnFor = newState.points().stream().filter(p -> p.index() == respawnFor.index()).findFirst().orElse(null);
        }
        // The state comes again every few seconds: a box armed for deletion stays armed while it is unchanged
        if (deleteArmed != null && !newState.points().contains(deleteArmed) && !newState.targets().contains(deleteArmed)) {
            deleteArmed = null;
        }
    }

    static void send(ServerboundMapEditorPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    /** From a point's settings: the next right-clicked block is where its team respawns. */
    static void placeRespawn(EditorState.Point point) {
        respawnFor = point;
        pendingCorner = null;
    }

    private static boolean holding() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.player.hasPermissions(2)
                && mc.player.getMainHandItem().is(ModItems.MAP_EDITOR.get());
    }

    // ---- input ----

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!holding() || !Screen.hasControlDown() || Minecraft.getInstance().screen != null) return;
        int step = event.getScrollDeltaY() > 0 ? -1 : 1;
        Mode[] modes = Mode.values();
        mode = modes[Math.floorMod(mode.ordinal() + step, modes.length)];
        pendingCorner = null;
        respawnFor = null;
        deleteArmed = null;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!holding() || event.isPickBlock()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack()) {
            pendingCorner = null;
            respawnFor = null;
            deleteArmed = null;
            return;
        }
        if (event.isUseItem()) use();
    }

    private static void use() {
        Minecraft mc = Minecraft.getInstance();
        Look look = look();
        switch (mode) {
            case RED_BASE, BLUE_BASE -> {
                if (look.block != null) {
                    send(new ServerboundMapEditorPacket(ServerboundMapEditorPacket.Action.SET_BASE, -1, "",
                            mode == Mode.RED_BASE ? "RED" : "BLUE", look.block, look.block, 0));
                }
            }
            case POINTS, TARGETS -> {
                if (respawnFor != null) {
                    if (look.block != null) {
                        send(new ServerboundMapEditorPacket(ServerboundMapEditorPacket.Action.SET_POINT_RESPAWN, respawnFor.index(),
                                respawnFor.name(), respawnFor.owner(), look.block.above(), look.block.above(), 0));
                        respawnFor = null;
                    }
                    return;
                }
                if (pendingCorner != null) {
                    if (look.block != null) {
                        BlockPos first = pendingCorner;
                        pendingCorner = null;
                        mc.setScreen(mode == Mode.POINTS
                                ? MapEditorScreen.newPoint(first, look.block, state.points().size() + 1)
                                : MapEditorScreen.newTarget(first, look.block, state.targets().size() + 1));
                    }
                    return;
                }
                if (look.box != null) {
                    if (Screen.hasShiftDown()) {
                        long now = mc.level.getGameTime();
                        if (look.box.equals(deleteArmed) && now - deleteArmedAt <= DELETE_CONFIRM_TICKS) {
                            deleteArmed = null;
                            if (look.box instanceof EditorState.Point p) {
                                send(new ServerboundMapEditorPacket(ServerboundMapEditorPacket.Action.DELETE_POINT, p.index(), "", "", p.min(), p.max(), 0));
                            } else if (look.box instanceof EditorState.Target t) {
                                send(new ServerboundMapEditorPacket(ServerboundMapEditorPacket.Action.DELETE_TARGET, t.index(), "", "", t.min(), t.max(), 0));
                            }
                        } else {
                            deleteArmed = look.box;
                            deleteArmedAt = now;
                            mc.player.displayClientMessage(Component.translatable("editor.unrealzaruba.delete_confirm"), true);
                        }
                        return;
                    }
                    mc.setScreen(look.box instanceof EditorState.Point p ? MapEditorScreen.point(p) : MapEditorScreen.target((EditorState.Target) look.box));
                    return;
                }
                if (look.block != null) pendingCorner = look.block;
            }
        }
    }

    /** What the player looks at: a box of the current kind (when it is nearer than the blocks) or a block. */
    private record Look(BlockPos block, Object box) {}

    private static Look look() {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition(1);
        Vec3 end = eye.add(mc.player.getViewVector(1).scale(REACH));
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        double blockDistance = hit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : hit.getLocation().distanceTo(eye);
        BlockPos block = hit.getType() == HitResult.Type.MISS ? null : hit.getBlockPos();

        // While a corner or a respawn is being placed, and in the base modes, only blocks count
        if (pendingCorner != null || respawnFor != null || mode == Mode.RED_BASE || mode == Mode.BLUE_BASE) {
            return new Look(block, null);
        }
        Object nearest = null;
        double nearestDistance = blockDistance;
        Iterable<?> boxes = mode == Mode.POINTS ? state.points() : state.targets();
        for (Object box : boxes) {
            AABB aabb = box instanceof EditorState.Point p ? box(p.min(), p.max()) : box(((EditorState.Target) box).min(), ((EditorState.Target) box).max());
            if (aabb.contains(eye)) continue; // from inside a zone, the blocks in it are what is clicked
            var clip = aabb.clip(eye, end);
            if (clip.isPresent() && clip.get().distanceTo(eye) < nearestDistance) {
                nearest = box;
                nearestDistance = clip.get().distanceTo(eye);
            }
        }
        return nearest != null ? new Look(null, nearest) : new Look(block, null);
    }

    private static AABB box(BlockPos a, BlockPos b) {
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** A player-sized frame standing on {@code feet}. */
    private static AABB standing(BlockPos feet) {
        return new AABB(feet.getX() + 0.2, feet.getY(), feet.getZ() + 0.2, feet.getX() + 0.8, feet.getY() + 1.8, feet.getZ() + 0.8);
    }

    static int ownerColor(String owner) {
        return switch (owner) {
            case "RED" -> RED;
            case "BLUE" -> BLUE;
            default -> NEUTRAL;
        };
    }

    // ---- drawing ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!holding()) {
            if (wasHolding) {
                pendingCorner = null;
                respawnFor = null;
                deleteArmed = null;
            }
            wasHolding = false;
            return;
        }
        long now = mc.level.getGameTime();
        // On picking the editor up, and now and then for changes made by others or by commands
        if (!wasHolding || now - requestedAt > 100 || now < requestedAt) {
            requestedAt = now;
            send(ServerboundMapEditorPacket.request());
        }
        wasHolding = true;
        if (deleteArmed != null && now - deleteArmedAt > DELETE_CONFIRM_TICKS) deleteArmed = null;

        Outliner outliner = Outliner.getInstance();
        Look look = look();
        for (EditorState.Point p : state.points()) {
            AABB area = box(p.min(), p.max());
            boolean hovered = p.equals(look.box);
            glueBox(outliner, "uz_editor_point_" + p.index(), area, p.equals(deleteArmed) ? DELETE : GLUE_BLUE, hovered);
            outliner.showAABB("uz_editor_point_owner_" + p.index(), area.inflate(0.04)).colored(ownerColor(p.owner())).lineWidth(1 / 16f);
            if (p.respawn() != null) {
                AABB respawn = standing(p.respawn());
                outliner.showAABB("uz_editor_respawn_" + p.index(), respawn).colored(ownerColor(p.owner())).lineWidth(1 / 16f);
                outliner.showLine("uz_editor_respawn_line_" + p.index(), respawn.getCenter(), new Vec3(area.getCenter().x, area.minY, area.getCenter().z))
                        .colored(ownerColor(p.owner())).lineWidth(1 / 32f);
            }
        }
        for (EditorState.Target t : state.targets()) {
            AABB volume = box(t.min(), t.max());
            glueBox(outliner, "uz_editor_target_" + t.index(), volume, t.equals(deleteArmed) ? DELETE : GLUE_BLUE, t.equals(look.box));
            outliner.showAABB("uz_editor_target_owner_" + t.index(), volume.inflate(0.04)).colored(BLUE).lineWidth(1 / 16f);
        }
        for (var base : state.bases().entrySet()) {
            outliner.showAABB("uz_editor_base_" + base.getKey(), standing(base.getValue().above())).colored(ownerColor(base.getKey())).lineWidth(1 / 12f);
        }
        if (pendingCorner != null) {
            BlockPos other = look.block != null ? look.block : pendingCorner;
            glueBox(outliner, "uz_editor_pending", box(pendingCorner, other), PREVIEW, true);
        } else if (respawnFor != null && look.block != null) {
            outliner.showAABB("uz_editor_respawn_preview", standing(look.block.above())).colored(PREVIEW).lineWidth(1 / 16f);
        } else if ((mode == Mode.RED_BASE || mode == Mode.BLUE_BASE) && look.block != null) {
            outliner.showAABB("uz_editor_base_preview", standing(look.block.above())).colored(mode == Mode.RED_BASE ? RED : BLUE).lineWidth(1 / 16f);
        }
    }

    private static void glueBox(Outliner outliner, String slot, AABB box, int color, boolean hovered) {
        outliner.showAABB(slot, box)
                .colored(color)
                .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
                .disableLineNormals()
                .lineWidth(hovered ? 1 / 8f : 1 / 16f);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !holding()) return;
        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Font font = UiFont.get();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();

        for (EditorState.Point p : state.points()) {
            AABB area = box(p.min(), p.max());
            label(pose, camera, font, buffers, new Vec3(area.getCenter().x, area.maxY + 0.6, area.getCenter().z),
                    p.name(), Component.translatable("editor.unrealzaruba.owner." + ownerKey(p.owner())).getString(), ownerColor(p.owner()));
            if (p.respawn() != null) {
                label(pose, camera, font, buffers, Vec3.atBottomCenterOf(p.respawn()).add(0, 2.2, 0),
                        Component.translatable("editor.unrealzaruba.respawn_of", p.name()).getString(), null, ownerColor(p.owner()));
            }
        }
        for (EditorState.Target t : state.targets()) {
            AABB volume = box(t.min(), t.max());
            label(pose, camera, font, buffers, new Vec3(volume.getCenter().x, volume.maxY + 0.6, volume.getCenter().z),
                    t.name(), Component.translatable("editor.unrealzaruba.target_label", Math.round(t.required() * 1000) / 10.0).getString(), BLUE);
        }
        for (var base : state.bases().entrySet()) {
            label(pose, camera, font, buffers, Vec3.atBottomCenterOf(base.getValue()).add(0, 3.2, 0),
                    Component.translatable("editor.unrealzaruba.base." + ownerKey(base.getKey())).getString(), null, ownerColor(base.getKey()));
        }
        buffers.endBatch();
    }

    static String ownerKey(String owner) {
        return switch (owner) {
            case "RED" -> "red";
            case "BLUE" -> "blue";
            default -> "neutral";
        };
    }

    private static void label(PoseStack pose, Camera camera, Font font, MultiBufferSource buffers, Vec3 at,
                              String title, String detail, int color) {
        Vec3 eye = camera.getPosition();
        double dx = at.x - eye.x, dy = at.y - eye.y, dz = at.z - eye.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > LABEL_DISTANCE) return;
        float scale = 0.03f * Mth.clamp((float) (distance / 10.0), 1f, 6f);
        pose.pushPose();
        pose.translate(dx, dy, dz);
        pose.mulPose(camera.rotation());
        pose.scale(-scale, -scale, scale);
        // The level is drawn with the camera rotation already on the model-view stack: the pose is all that is needed
        Matrix4f matrix = pose.last().pose();
        Component name = Component.literal(title).withStyle(UiFont.bold());
        font.drawInBatch(name, -font.width(name) / 2f, 0, 0xFF000000 | color, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
        if (detail != null) {
            font.drawInBatch(detail, -font.width(detail) / 2f, 11, Brand.TEXT_DIM, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, 0xF000F0);
        }
        pose.popPose();
    }

    /** Over the hotbar: the editor's mode and what the mouse does now. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!holding() || mc.screen != null) return;
        GuiGraphics g = event.getGuiGraphics();
        Font font = UiFont.get();
        String title = Component.translatable("editor.unrealzaruba.title").getString() + " · "
                + Component.translatable("editor.unrealzaruba.mode." + mode.key).getString()
                + (state.mapKey().isEmpty() ? "" : "  [" + state.mapKey() + "]");
        String hint;
        if (state.mapKey().isEmpty()) {
            hint = Component.translatable("editor.unrealzaruba.no_map").getString();
        } else if (respawnFor != null) {
            hint = Component.translatable("editor.unrealzaruba.hint.respawn", respawnFor.name()).getString();
        } else if (pendingCorner != null) {
            hint = Component.translatable("editor.unrealzaruba.hint.second_corner").getString();
        } else {
            hint = Component.translatable("editor.unrealzaruba.hint." + mode.key).getString();
        }
        if (!state.mapKey().isEmpty() && !state.editable()) {
            hint = Component.translatable("editor.unrealzaruba.view_only").getString();
        }
        int width = g.guiWidth();
        int y = g.guiHeight() - 82;
        int boxWidth = Math.max(font.width(title), font.width(hint)) + 16;
        HudStyle.panel(g, (width - boxWidth) / 2, y - 5, boxWidth, 29);
        g.drawCenteredString(font, title, width / 2, y, Brand.MINT);
        g.drawCenteredString(font, hint, width / 2, y + 12, Brand.TEXT_DIM);
    }
}
