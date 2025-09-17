package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.SelectRespawnPointPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CustomDeathScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(UnrealZaruba.MOD_ID,
            "textures/gui/death_screen.png");
    private static final int TIMER_DURATION = 10; // Example respawn timer in seconds
    private int respawnTimer = TIMER_DURATION * 20; // Convert seconds to ticks (20 ticks per second)
    private final Minecraft minecraft;
    private Boolean tentExist;
    private Button baseButton;
    private Button tentButton;
    private int selectedRespawnPointId;

    public CustomDeathScreen(boolean tentExist) {
        super(Component.literal("You Died"));
        this.minecraft = Minecraft.getInstance();
        this.tentExist = tentExist;
    }

    @Override
    protected void init() {
        super.init();
        if (tentExist) {

            baseButton = Button.builder(Component.literal("Respawn on Base"), button -> selectBase()).
                bounds(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build();
            this.addRenderableWidget(baseButton);

            tentButton = Button.builder(Component.literal("Respawn on Tent"), button -> selectTent()).
                    bounds(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build();
            this.addRenderableWidget(tentButton);

            baseButton.active = false;
        }
    }

    private void selectBase() {
        baseButton.active = false;
        tentButton.active = true;

        sendSelectRespawnPoint(0);
    }

    private void selectTent() {
        tentButton.active = false;
        baseButton.active = true;

        sendSelectRespawnPoint(1);
    }

    public void updateRespawnTimer(int newTime) {
        this.respawnTimer = newTime * 20; // Convert seconds to ticks
        UnrealZaruba.LOGGER.warn(Integer.toString(respawnTimer));
        if (respawnTimer <= 0)
            minecraft.setScreen(null);
    }

    private void sendSelectRespawnPoint(int respawnPointId) {
        var player = Minecraft.getInstance().player;
        if (player == null)
            UnrealZaruba.LOGGER.warn("[WARN] Local player is not found");
        NetworkHandler.CHANNEL.sendToServer(new SelectRespawnPointPacket(player.getUUID(), respawnPointId));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics); // Draw background

        guiGraphics.blit(BACKGROUND_TEXTURE,this.width / 2 - 128, this.height / 2 - 128, 0, 0, 256, 256, 256, 256); // Draw the background
        // texture

        super.render(guiGraphics, mouseX, mouseY, delta); // Draw buttons

        String timerText = "Respawn in: §4" + (respawnTimer / 20) + "§r seconds";

        guiGraphics.pose().pushPose();

        float scale = 2.0f; // Example scale factor (2x bigger)
        guiGraphics.pose().scale(scale, scale, scale);

        int scaledWidth = (int) (this.width / (2 * scale)); // Adjust position based on scale
        int scaledHeight = (int) ((this.height / 2 - 40) / scale); // Adjust position based on scale

        guiGraphics.drawCenteredString(this.font, timerText, scaledWidth, scaledHeight, 0xFFFFFF);

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
