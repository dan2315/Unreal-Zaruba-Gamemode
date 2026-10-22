package com.dod.unrealzaruba.Vehicles;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.joml.Quaterniond;
import org.joml.Vector3d;

import com.dod.unrealzaruba.SableIntegration.SableShips;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Turns a vehicle that lies on its roof back onto its wheels. The player holds a key beside it for {@link #HOLD_TICKS}; the
 * vehicle is then lifted and turned until its up direction is the world's up, over {@link #TURN_TICKS}, and set down.
 */
public final class VehicleFlip {
    /** The key is held this long (10 seconds). */
    public static final int HOLD_TICKS = 200;
    private static final int TURN_TICKS = 60;
    /** The held key is reported every tick; this many ticks without a report cancel the hold. */
    private static final int GRACE_TICKS = 4;
    private static final double REACH = 8.0;
    /** A vehicle whose up direction points less than this high counts as overturned (1 = upright, -1 = on its roof). */
    private static final double OVERTURNED_BELOW = 0.35;
    private static final double LIFT = 3.0;
    private static final double SETTLE = 0.8;

    private static final class Hold {
        UUID vehicle;
        int held;
        int lastSeen;
    }

    private static final class Turn {
        final UUID vehicle;
        final ServerLevel level;
        final Quaterniond from;
        final Quaterniond to;
        final double baseY;
        int tick;

        Turn(UUID vehicle, ServerLevel level, Quaterniond from, Quaterniond to, double baseY) {
            this.vehicle = vehicle;
            this.level = level;
            this.from = from;
            this.to = to;
            this.baseY = baseY;
        }
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    private static final Map<UUID, Turn> TURNS = new HashMap<>();
    private static int now;

    private VehicleFlip() {}

    /** The player reports that the key is held. */
    public static void held(ServerPlayer player) {
        if (player.isSpectator() || player.isPassenger()) return;
        Hold hold = HOLDS.get(player.getUUID());
        if (hold == null) {
            ServerSubLevel target = overturnedNear(player);
            if (target == null) return;
            hold = new Hold();
            hold.vehicle = target.getUniqueId();
            hold.lastSeen = now;
            HOLDS.put(player.getUUID(), hold);
        }
        hold.lastSeen = now;
    }

    public static void tick(MinecraftServer server) {
        now++;
        for (Iterator<Map.Entry<UUID, Hold>> it = HOLDS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Hold hold = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ServerSubLevel vehicle = player == null ? null : SableShips.get(player.serverLevel(), hold.vehicle);
            if (player == null || vehicle == null || now - hold.lastSeen > GRACE_TICKS || !overturned(vehicle) || TURNS.containsKey(hold.vehicle)) {
                if (player != null && now - hold.lastSeen > GRACE_TICKS && hold.held < HOLD_TICKS) {
                    player.displayClientMessage(Component.translatable("vehicle.unrealzaruba.flip.cancelled"), true);
                }
                it.remove();
                continue;
            }
            hold.held++;
            if (hold.held % 10 == 0) {
                player.displayClientMessage(Component.translatable("vehicle.unrealzaruba.flip.progress", hold.held / 20, HOLD_TICKS / 20), true);
            }
            if (hold.held >= HOLD_TICKS) {
                begin(player.serverLevel(), vehicle);
                it.remove();
            }
        }

        for (Iterator<Turn> it = TURNS.values().iterator(); it.hasNext(); ) {
            Turn turn = it.next();
            ServerSubLevel vehicle = SableShips.get(turn.level, turn.vehicle);
            if (vehicle == null) {
                it.remove();
                continue;
            }
            turn.tick++;
            double t = Math.min(1.0, turn.tick / (double) TURN_TICKS);
            double eased = t * t * (3 - 2 * t);
            Quaterniond orientation = new Quaterniond(turn.from).slerp(turn.to, eased).normalize();
            Vector3d position = new Vector3d(vehicle.logicalPose().position());
            position.y = turn.baseY + LIFT * Math.sin(Math.PI * t) + SETTLE * t;
            RigidBodyHandle handle = RigidBodyHandle.of(vehicle);
            handle.teleport(position, orientation);
            // Nothing carries over from before the turn: it is lifted and turned by hand, not thrown
            handle.addLinearAndAngularVelocity(new Vector3d(handle.getLinearVelocity()).negate(), new Vector3d(handle.getAngularVelocity()).negate());
            if (turn.tick >= TURN_TICKS) it.remove();
        }
    }

    public static void forget(UUID player) {
        HOLDS.remove(player);
    }

    private static void begin(ServerLevel level, ServerSubLevel vehicle) {
        Quaterniond from = new Quaterniond(vehicle.logicalPose().orientation());
        Vector3d up = from.transform(new Vector3d(0, 1, 0));
        // The shortest turn that puts the up direction on the world's up; a turn about the vertical (the heading) is kept
        Quaterniond to = new Quaterniond().rotationTo(up, new Vector3d(0, 1, 0)).mul(from).normalize();
        TURNS.put(vehicle.getUniqueId(), new Turn(vehicle.getUniqueId(), level, from, to, vehicle.logicalPose().position().y()));
    }

    private static boolean overturned(ServerSubLevel vehicle) {
        Vector3d up = new Quaterniond(vehicle.logicalPose().orientation()).transform(new Vector3d(0, 1, 0));
        return up.y < OVERTURNED_BELOW;
    }

    /** The overturned vehicle nearest to the player within reach, or null. */
    private static ServerSubLevel overturnedNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var container = SubLevelContainer.getContainer(level);
        if (container == null) return null;
        ServerSubLevel best = null;
        double bestDistance = REACH * REACH;
        for (var sub : container.getAllSubLevels()) {
            if (!(sub instanceof ServerSubLevel vehicle) || vehicle.isRemoved() || TURNS.containsKey(vehicle.getUniqueId()) || !overturned(vehicle)) continue;
            var bounds = vehicle.boundingBox();
            // Distance from the player to the box of the vehicle in the world
            double dx = Math.max(Math.max(bounds.minX() - player.getX(), 0), player.getX() - bounds.maxX());
            double dy = Math.max(Math.max(bounds.minY() - player.getY(), 0), player.getY() - bounds.maxY());
            double dz = Math.max(Math.max(bounds.minZ() - player.getZ(), 0), player.getZ() - bounds.maxZ());
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = vehicle;
            }
        }
        return best;
    }
}
