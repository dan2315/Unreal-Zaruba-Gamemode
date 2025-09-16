package com.dod.UnrealZaruba.UI;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GamemodeSelectionScreen extends Screen {
    private GamemodeList gamemodeList;

    public GamemodeSelectionScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        this.gamemodeList = new GamemodeList(this.minecraft, this.width, this.height, 32, this.height - 32, 20);
        this.gamemodeList.addGamemode(Component.literal("Выживание"), 0);
        this.gamemodeList.addGamemode(Component.literal("Творческий"), 1);
        this.addRenderableWidget(this.gamemodeList);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.gamemodeList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public class GamemodeList extends AbstractSelectionList<GamemodeList.GamemodeListEntry> {
        public GamemodeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return 200;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 110;
        }

        public void addGamemode(Component name, int id) {
            this.addEntry(new GamemodeListEntry(name, id));
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {

        }

        // элемент списка теперь вложенный класс
        public class GamemodeListEntry extends AbstractSelectionList.Entry<GamemodeListEntry> {
            private final Component name;
            private final int id;

            public GamemodeListEntry(Component name, int id) {
                this.name = name;
                this.id = id;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                int color = hovered ? 0xFFFFA0 : 0xFFFFFF;
                guiGraphics.drawString(font, name, left + 5, top + 5, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Выбран режим: " + name.getString() + " (id=" + id + ")"));
                return true;
            }
        }
    }
}
