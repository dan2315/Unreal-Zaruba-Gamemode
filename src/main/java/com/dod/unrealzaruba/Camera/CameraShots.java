package com.dod.unrealzaruba.Camera;

import com.dod.unrealzaruba.NetworkPackets.Camera.ClientboundCameraShotPacket;
import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Screenshot camera: a player tagged as a camera (a second client left running in the background) is moved to a
 * position as a spectator and saves what it sees to screenshots/claude/<name>.png. The camera is kept out of the
 * game logic so it never joins a team.
 */
public final class CameraShots {
    public static final String TAG = "zaruba_camera";
    public static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static volatile String lastResult = "no shots yet";

    private CameraShots() {
    }

    public static boolean isCamera(ServerPlayer player) {
        return player.getTags().contains(TAG);
    }

    @Nullable
    public static ServerPlayer findCamera(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isCamera(player)) {
                return player;
            }
        }
        return null;
    }

    public static void shoot(ServerPlayer camera, ServerLevel level, double x, double y, double z, float yaw, float pitch,
                             float fov, String name) {
        camera.setGameMode(GameType.SPECTATOR);
        camera.teleportTo(level, x, y, z, yaw, pitch);
        lastResult = name + ": pending";
        PacketDistributor.sendToPlayer(camera, new ClientboundCameraShotPacket(x, y, z, yaw, pitch, fov, name));
    }

    /** Yaw and pitch of a camera at from looking at to, in Minecraft's convention. */
    public static float[] lookAt(double fx, double fy, double fz, double tx, double ty, double tz) {
        double dx = tx - fx, dy = ty - fy, dz = tz - fz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[]{yaw, pitch};
    }

    public static void onResult(ServerPlayer camera, boolean success, String name, String message) {
        if (!isCamera(camera)) {
            return;
        }
        lastResult = name + ": " + (success ? "saved " : "failed ") + message;
        UnrealZaruba.LOGGER.info("[Camera] {}", lastResult);
    }

    public static String lastResult() {
        return lastResult;
    }
}
