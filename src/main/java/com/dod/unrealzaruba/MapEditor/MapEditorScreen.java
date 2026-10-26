package com.dod.unrealzaruba.MapEditor;

import com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Settings of a capture point (name, initial owner; its respawn is placed in the world) or of a target to destroy
 * (name, the share of its blocks to destroy; targets belong to the blue team). New ones are saved from here too.
 */
public class MapEditorScreen extends Screen {
    private static final int WIDTH = 240;
    private static final String[] OWNERS = {"", "RED", "BLUE"};

    private final boolean isPoint;
    private final EditorState.Point point;
    private final int index;
    private final BlockPos min;
    private final BlockPos max;
    private final String initialName;
    private String owner;
    private final float initialRequired;

    private EditBox nameBox;
    private EditBox requiredBox;
    private Button ownerButton;

    private MapEditorScreen(boolean isPoint, EditorState.Point point, int index, BlockPos min, BlockPos max, String name, String owner, float required) {
        super(Component.translatable(isPoint ? "editor.unrealzaruba.screen.point" : "editor.unrealzaruba.screen.target"));
        this.isPoint = isPoint;
        this.point = point;
        this.index = index;
        this.min = min;
        this.max = max;
        this.initialName = name;
        this.owner = owner;
        this.initialRequired = required;
    }

    static MapEditorScreen newPoint(BlockPos a, BlockPos b, int number) {
        return new MapEditorScreen(true, null, -1, a, b,
                Component.translatable("editor.unrealzaruba.default_point", number).getString(), "", 0);
    }

    static MapEditorScreen newTarget(BlockPos a, BlockPos b, int number) {
        return new MapEditorScreen(false, null, -1, a, b,
                Component.translatable("editor.unrealzaruba.default_target", number).getString(), "BLUE", 0.01f);
    }

    static MapEditorScreen point(EditorState.Point p) {
        return new MapEditorScreen(true, p, p.index(), p.min(), p.max(), p.name(), p.owner(), 0);
    }

    static MapEditorScreen target(EditorState.Target t) {
        return new MapEditorScreen(false, null, t.index(), t.min(), t.max(), t.name(), "BLUE", t.required());
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int y = height / 2 - 60;

        nameBox = new EditBox(font, left, y, WIDTH, 20, Component.translatable("editor.unrealzaruba.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(initialName == null ? "" : initialName);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);
        y += 28;

        if (isPoint) {
            ownerButton = Button.builder(ownerText(), b -> {
                int next = 0;
                for (int i = 0; i < OWNERS.length; i++) {
                    if (OWNERS[i].equals(owner)) next = (i + 1) % OWNERS.length;
                }
                owner = OWNERS[next];
                b.setMessage(ownerText());
            }).bounds(left, y, WIDTH, 20).build();
            addRenderableWidget(ownerButton);
        } else {
            requiredBox = new EditBox(font, left + WIDTH - 60, y, 60, 20, Component.translatable("editor.unrealzaruba.required"));
            requiredBox.setMaxLength(5);
            requiredBox.setFilter(text -> text.matches("[0-9]{0,3}([.,][0-9]{0,2})?"));
            requiredBox.setValue(format(initialRequired * 100));
            addRenderableWidget(requiredBox);
        }
        y += 28;

        if (point != null) {
            addRenderableWidget(Button.builder(Component.translatable("editor.unrealzaruba.place_respawn"), b -> {
                MapEditorClient.placeRespawn(point);
                onClose();
            }).bounds(left, y, WIDTH, 20).build());
            y += 24;
        }

        int half = (WIDTH - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("editor.unrealzaruba.save"), b -> save()).bounds(left, y, half, 20).build());
        if (index >= 0) {
            addRenderableWidget(Button.builder(Component.translatable("editor.unrealzaruba.delete"), b -> {
                MapEditorClient.send(new ServerboundMapEditorPacket(isPoint ? ServerboundMapEditorPacket.Action.DELETE_POINT
                        : ServerboundMapEditorPacket.Action.DELETE_TARGET, index, "", "", min, max, 0));
                onClose();
            }).bounds(left + half + 4, y, half, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + half + 4, y, half, 20).build());
        }
    }

    private Component ownerText() {
        return Component.translatable("editor.unrealzaruba.owner_button",
                Component.translatable("editor.unrealzaruba.owner." + MapEditorClient.ownerKey(owner)));
    }

    private static String format(float percent) {
        String text = String.valueOf(Math.round(percent * 100) / 100.0);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private void save() {
        String name = nameBox.getValue().strip();
        if (name.isEmpty()) name = initialName;
        if (isPoint) {
            MapEditorClient.send(new ServerboundMapEditorPacket(ServerboundMapEditorPacket.Action.SAVE_POINT, index, name, owner, min, max, 0));
        } else {
            float percent;
            try {
                percent = Float.parseFloat(requiredBox.getValue().replace(',', '.'));
            } catch (NumberFormatException e) {
                percent = initialRequired * 100;
            }
            MapEditorClient.send(new ServerboundMapEditorPacket(ServerboundMapEditorPacket.Action.SAVE_TARGET, index, name, "BLUE", min, max, percent / 100f));
        }
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = (width - WIDTH) / 2;
        int top = height / 2 - 60;
        String heading = title.getString() + (index < 0 ? " · " + Component.translatable("editor.unrealzaruba.new").getString() : "");
        g.drawCenteredString(font, heading, width / 2, top - 30, Brand.MINT);
        int sx = Math.abs(max.getX() - min.getX()) + 1;
        int sy = Math.abs(max.getY() - min.getY()) + 1;
        int sz = Math.abs(max.getZ() - min.getZ()) + 1;
        g.drawCenteredString(font, Component.translatable("editor.unrealzaruba.size", sx, sy, sz).getString(), width / 2, top - 16, Brand.TEXT_DIM);
        if (!isPoint) {
            g.drawString(font, Component.translatable("editor.unrealzaruba.required").getString(), left, top + 34, Brand.TEXT);
            g.drawString(font, Component.translatable("editor.unrealzaruba.target_owner").getString(), left, top + 46, Brand.TEXT_MUTE);
        } else if (point != null && point.respawn() == null) {
            g.drawString(font, Component.translatable("editor.unrealzaruba.no_respawn").getString(), left, top + 80 + 22, Brand.WARN);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, Brand.alpha(Brand.WINDOW, 0xC0));
        HudStyle.panel(g, (width - WIDTH) / 2 - 12, height / 2 - 100, WIDTH + 24, 200);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
