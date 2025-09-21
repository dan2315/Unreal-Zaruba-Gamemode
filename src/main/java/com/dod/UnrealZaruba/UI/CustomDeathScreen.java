package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.SelectRespawnPointPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class CustomDeathScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(UnrealZaruba.MOD_ID,
            "textures/gui/death_screen.png");
    private static final int TIMER_DURATION = 10; // Example respawn timer in seconds
    private int respawnTimer = TIMER_DURATION * 20; // Convert seconds to ticks (20 ticks per second)
    private final Minecraft minecraft;
    private List<RespawnUiElement> respawnPoints;
    private byte selectedRespawnPointId;
    private RespawnPointList respawnPointList;

    public CustomDeathScreen(List<RespawnUiElement> respawnPoints) {
        super(Component.literal("You Died"));
        this.minecraft = Minecraft.getInstance();
        this.respawnPoints = respawnPoints;
    }

    @Override
    protected void init() {
        super.init();
        if (respawnPoints.size() <= 1) return;

        int listTop = this.height / 2;
        int listBottom = this.height - 150;

        respawnPointList = new RespawnPointList(minecraft, this.width, this.height, listTop, listBottom, 20);

        for (RespawnUiElement el : respawnPoints) {
            respawnPointList.addRespawnPoint(el, () -> {
                this.selectedRespawnPointId = el.runtimeId();
                sendSelectRespawnPoint(selectedRespawnPointId);
            });
        }

        this.addRenderableWidget(respawnPointList);
    }


    public void updateRespawnTimer(int newTime) {
        this.respawnTimer = newTime * 20;
        UnrealZaruba.LOGGER.warn(Integer.toString(respawnTimer));
        if (respawnTimer <= 0)
            minecraft.setScreen(null);
    }

    private void sendSelectRespawnPoint(byte respawnPointId) {
        var player = Minecraft.getInstance().player;
        if (player == null)
            UnrealZaruba.LOGGER.warn("[WARN] Local player is not found");
        NetworkHandler.CHANNEL.sendToServer(new SelectRespawnPointPacket(player.getUUID(), respawnPointId));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);

        guiGraphics.blit(BACKGROUND_TEXTURE,this.width / 2 - 128, this.height / 2 - 128, 0, 0, 256, 256, 256, 256); // Draw the background

        String timerText = "Respawn in: §4" + (respawnTimer / 20) + "§r seconds";

        guiGraphics.pose().pushPose();

        float scale = 1.5f;
        guiGraphics.pose().scale(scale, scale, scale);

        int scaledWidth = (int) (this.width / (2 * scale));
        int scaledHeight = (int) ((this.height / 2 - 40) / scale);

        guiGraphics.drawCenteredString(this.font, timerText, scaledWidth, scaledHeight, 0xFFFFFF);
        guiGraphics.pose().popPose();

        if (respawnPoints.size() > 1) {
            guiGraphics.drawCenteredString(this.font, "Выбери точку респавна:", scaledWidth, scaledHeight + 16, 0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public record RespawnUiElement(byte runtimeId, String name) {}

    public static class RespawnPointList extends AbstractSelectionList<RespawnPointList.Entry>{

        public RespawnPointList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            setRenderHeader(false, 0);
        }

        public void addRespawnPoint(CustomDeathScreen.RespawnUiElement element, Runnable onSelect) {
            this.addEntry(new Entry(element, onSelect));
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) { }

        public class Entry extends AbstractSelectionList.Entry<Entry> {
            private final CustomDeathScreen.RespawnUiElement element;
            private final Runnable onSelect;

            public Entry(CustomDeathScreen.RespawnUiElement element, Runnable onSelect) {
                this.element = element;
                this.onSelect = onSelect;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                int color = hovered ? 0xFFFFA0 : 0xFFFFFF;
                guiGraphics.drawString(minecraft.font, element.name(), left + 5, top + 5, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    onSelect.run();
                    return true;
                }
                return false;
            }
        }
    }
}
