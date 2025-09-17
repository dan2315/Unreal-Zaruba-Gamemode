package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.NetworkPackets.GamemodeVotePacket;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GamemodeSelectionScreen extends Screen {
    private GamemodeList gamemodeList;
    private String selectedGamemodeId;

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private final List<GamemodeDefinition> gamemodes = new ArrayList<>();

    public GamemodeSelectionScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resources = resourceManager.listResources("gamemodes", path -> path.getPath().endsWith(".json"));
            for (var entry : resources.entrySet()) {
                try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                    GamemodeDefinition def = gson.fromJson(reader, GamemodeDefinition.class);
                    if (def != null) {
                        gamemodes.add(def);
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }


        this.gamemodeList = new GamemodeList(this.minecraft, this.width, this.height, 32, this.height - 32, 144);
        gamemodes.forEach(gamemodeDefinition -> {
            this.gamemodeList.addGamemode(
                    gamemodeDefinition.id,
                    Component.literal(gamemodeDefinition.name),
                    Component.literal(gamemodeDefinition.description));
        });
        this.addRenderableWidget(this.gamemodeList);

        this.addRenderableWidget(Button.builder(Component.literal("Проголосовать"),
                btn -> {
                    if (selectedGamemodeId != null) {
                        NetworkHandler.CHANNEL.sendToServer(new GamemodeVotePacket(selectedGamemodeId));
                        Minecraft.getInstance().setScreen(null);
                    }
                }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.gamemodeList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        var mc = Minecraft.getInstance();

        // pick block face texture (particle = usually side)
        var sprite = mc.getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(Blocks.STONE.defaultBlockState());

        // block atlas
        var atlas = TextureAtlas.LOCATION_BLOCKS;

        int tileSize = 32; // how big each block texture will look on screen

        for (int x = 0; x < this.width; x += tileSize) {
            for (int y = 0; y < this.height; y += tileSize) {
                guiGraphics.blit(
                        atlas,
                        x, y, tileSize, tileSize, // where & how big on screen
                        sprite.getU0() * 16,      // source U start (px)
                        sprite.getV0() * 16,      // source V start (px)
                        16, 16,                   // how many px from atlas to copy (block face = 16x16)
                        256, 256                  // atlas size (usually 256x256)
                );
            }
        }
    }

    public record GamemodeDefinition(
            String id,
            String name,
            String description
    ) {}

    public class GamemodeList extends AbstractSelectionList<GamemodeList.GamemodeListEntry> {
        public GamemodeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return (int) (this.width / 2.5f);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + getRowWidth() / 2;
        }

        public void addGamemode(String id, Component name, Component description) {
            this.addEntry(new GamemodeListEntry(id, name, description));
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

        public class GamemodeListEntry extends AbstractSelectionList.Entry<GamemodeListEntry> {
            private final String id;
            private final Component name;
            private final Component description;
            private final ResourceLocation icon;

            public GamemodeListEntry(String id, Component name, Component description) {
                this.name = name;
                this.description = description;
                this.id = id;
                this.icon = new ResourceLocation("unrealzaruba", "textures/gui/gamemodethumbnails/default.png");
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                int color = hovered ? 0xFFFFA0 : 0xFFFFFF;
                guiGraphics.renderOutline(left, top, width, height, 0x555555);

                int iconSize = 112;
                int iconX = left + 5;
                int iconY = top + 5;

                // Draw the icon
                guiGraphics.blit(
                        icon,
                        iconX, iconY,
                        0, 0,
                        iconSize, iconSize,
                        iconSize, iconSize
                );

                // Draw name under the icon
                int nameX = iconX;
                int nameY = iconY + iconSize + 2;
                guiGraphics.drawCenteredString(font, name, nameX + (iconSize / 2), nameY, color);

                // Draw description on the right
                int descX = iconX + iconSize + 10;
                int descY = top + 5;
                int maxWidth = width - (iconSize + 20);

                // Wrap description text if it's too long
                var descLines = font.split(description, maxWidth);
                for (int i = 0; i < descLines.size(); i++) {
                    guiGraphics.drawString(font, descLines.get(i), descX, descY + i * font.lineHeight, 0xB5B5B5);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectedGamemodeId = id;
                return true;
            }
        }
    }
}
