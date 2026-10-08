package com.dod.unrealzaruba.Camera;

import com.dod.unrealzaruba.NetworkPackets.Camera.CameraShotResultPacket;
import com.dod.unrealzaruba.NetworkPackets.Camera.ClientboundCameraShotPacket;
import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.SectionPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Camera client side of {@link CameraShots}: holds the view the server moved us to, waits until the chunks around it
 * have arrived and every section is compiled, then saves the frame without the HUD.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCamera {
    private static final int MIN_TICKS = 20;
    private static final int STABLE_TICKS = 10;
    private static final int TIMEOUT_TICKS = 20 * 30;
    private static final int CHUNK_RADIUS = 6;

    private static ClientboundCameraShotPacket shot;
    private static int ticks;
    private static int stableTicks;
    private static boolean savedHideGui;

    private ClientCamera() {
    }

    public static void start(ClientboundCameraShotPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (!CameraShots.NAME.matcher(packet.name).matches()) {
            return;
        }
        if (shot == null) {
            savedHideGui = mc.options.hideGui;
        }
        shot = packet;
        ticks = 0;
        stableTicks = 0;
        mc.options.hideGui = true;
        // An unfocused window would otherwise open the pause menu over the shot
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null) {
            mc.setScreen(null);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (shot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            shot = null;
            mc.options.hideGui = savedHideGui;
            return;
        }
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        mc.player.setYRot(shot.yaw);
        mc.player.setXRot(shot.pitch);
        mc.player.yRotO = shot.yaw;
        mc.player.xRotO = shot.pitch;
        mc.player.setYHeadRot(shot.yaw);

        ticks++;
        boolean arrived = mc.player.distanceToSqr(shot.x, shot.y, shot.z) < 4;
        boolean ready = ticks >= MIN_TICKS && arrived && chunksLoaded(mc) && mc.levelRenderer.hasRenderedAllSections();
        stableTicks = ready ? stableTicks + 1 : 0;
        if (stableTicks >= STABLE_TICKS || ticks >= TIMEOUT_TICKS) {
            capture(mc, stableTicks >= STABLE_TICKS);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (shot != null && shot.fov > 0) {
            event.setFOV(shot.fov);
        }
    }

    private static boolean chunksLoaded(Minecraft mc) {
        int cx = SectionPos.blockToSectionCoord(shot.x);
        int cz = SectionPos.blockToSectionCoord(shot.z);
        int radius = Math.min(CHUNK_RADIUS, mc.options.getEffectiveRenderDistance());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!mc.level.hasChunk(cx + dx, cz + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void capture(Minecraft mc, boolean complete) {
        ClientboundCameraShotPacket taken = shot;
        shot = null;
        // The main target still holds the last frame, which was rendered with the HUD hidden
        NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        mc.options.hideGui = savedHideGui;
        Path file = mc.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("claude").resolve(taken.name + ".png");
        String size = image.getWidth() + "x" + image.getHeight();
        Util.ioPool().execute(() -> {
            boolean success;
            String message;
            try (image) {
                Files.createDirectories(file.getParent());
                image.writeToFile(file);
                success = true;
                message = file.toAbsolutePath() + " " + size + (complete ? "" : " (timed out, some chunks may be missing)");
            } catch (Exception e) {
                UnrealZaruba.LOGGER.error("[Camera] Failed to save {}", file, e);
                success = false;
                message = e.toString();
            }
            boolean sent = success;
            String text = message;
            mc.execute(() -> {
                if (mc.getConnection() != null) {
                    PacketDistributor.sendToServer(new CameraShotResultPacket(sent, taken.name, text));
                }
            });
        });
    }
}
