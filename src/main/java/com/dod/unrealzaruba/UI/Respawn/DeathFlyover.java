package com.dod.unrealzaruba.UI.Respawn;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * While a dead player waits to respawn, the camera rises over the place of the body and turns slowly. It is driven here, every
 * frame, not by the server (a teleport every tick is jerky), and the player can neither move nor turn it meanwhile.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class DeathFlyover {
    private static final double DURATION_SECONDS = 5.0;
    private static final double LIFT = 4.5;
    private static final double START_HEIGHT = 1.5;
    private static final float TURN_DEGREES = 60f;
    private static final float PITCH = 70f;

    private static double x, y, z;
    private static float yaw;
    private static long startedAt;

    private DeathFlyover() {}

    public static void setSpot(double spotX, double spotY, double spotZ, float spotYaw) {
        x = spotX;
        y = spotY;
        z = spotZ;
        yaw = spotYaw;
        startedAt = System.nanoTime();
    }

    private static boolean flying() {
        return RespawnClientState.isDead() && !RespawnClientState.isChoosing() && startedAt != 0
                && Minecraft.getInstance().player != null;
    }

    private static double progress() {
        return Math.min(1.0, (System.nanoTime() - startedAt) / (DURATION_SECONDS * 1e9));
    }

    private static double eased(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!flying()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        double t = progress();
        // The previous position is kept so that the frames between two ticks are interpolated
        player.xo = player.xOld = player.getX();
        player.yo = player.yOld = player.getY();
        player.zo = player.zOld = player.getZ();
        player.setPos(x, y + START_HEIGHT + LIFT * eased(t), z);
        player.setDeltaMovement(0, 0, 0);
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        player.setYRot(yaw + TURN_DEGREES * (float) t);
        player.setXRot(PITCH);
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (!flying()) return;
        // Whatever the mouse did, the camera looks where the flyover looks
        event.setYaw(yaw + TURN_DEGREES * (float) progress());
        event.setPitch(PITCH);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!flying()) return;
        var input = event.getInput();
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
