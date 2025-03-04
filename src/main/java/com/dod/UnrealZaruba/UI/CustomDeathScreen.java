package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.SelectTentPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
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

    public CustomDeathScreen(boolean tentExist) {
        super(new TextComponent("You Died"));
        this.minecraft = Minecraft.getInstance();
        this.tentExist = tentExist;
    }

    @Override
    protected void init() {
        super.init();
        if (tentExist) {
            baseButton = this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 2 - 10, 200, 20,
                    new TextComponent("Respawn on Base"), button -> selectBase()));
            tentButton = this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 2 + 20, 200, 20,
                    new TextComponent("Respawn in Tent"), button -> selectTent()));
            baseButton.active = false;
        }
    }

    private void selectBase() {
        baseButton.active = false;
        tentButton.active = true;

        respawnAtBase();
    }

    private void selectTent() {
        tentButton.active = false;
        baseButton.active = true;

        respawnInTent();
    }

    public void updateRespawnTimer(int newTime) {
        this.respawnTimer = newTime * 20; // Convert seconds to ticks
        UnrealZaruba.LOGGER.warn(Integer.toString(respawnTimer));
        if (respawnTimer <= 0)
            minecraft.setScreen(null);
    }

    private void respawnAtBase() {
        var playerID = Minecraft.getInstance().player.getUUID();
        if (playerID == null)
            UnrealZaruba.LOGGER.warn("[WARN] Local player is not found");
        NetworkHandler.CHANNEL.sendToServer(new SelectTentPacket(playerID, false));
    }

    private void respawnInTent() {
        var playerID = Minecraft.getInstance().player.getUUID();
        if (playerID == null)
            UnrealZaruba.LOGGER.warn("[WARN] Local player is not found");
        NetworkHandler.CHANNEL.sendToServer(new SelectTentPacket(playerID, true));
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Draw background
        UnrealZaruba.LOGGER.info("Loading background texture: " + BACKGROUND_TEXTURE);
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        blit(matrices, this.width / 2 - 128, this.height / 2 - 128, 0, 0, 256, 256, 256, 256); // Draw the background
        // texture

        super.render(matrices, mouseX, mouseY, delta); // Draw buttons

        String timerText = "Respawn in: §4" + (respawnTimer / 20) + "§r seconds";

        matrices.pushPose();

        float scale = 2.0f; // Example scale factor (2x bigger)
        matrices.scale(scale, scale, scale);

        int scaledWidth = (int) (this.width / (2 * scale)); // Adjust position based on scale
        int scaledHeight = (int) ((this.height / 2 - 40) / scale); // Adjust position based on scale

        drawCenteredString(matrices, this.font, timerText, scaledWidth, scaledHeight, 0xFFFFFF);

        matrices.popPose();
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
