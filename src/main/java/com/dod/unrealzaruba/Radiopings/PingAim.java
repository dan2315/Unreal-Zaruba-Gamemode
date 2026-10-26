package com.dod.unrealzaruba.Radiopings;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where a quick ping lands: the block or living entity under the crosshair, up to {@link #REACH} blocks away.
 * Falls back to the point at max reach (pinging the sky), so the key always does something.
 */
public final class PingAim {
    static final double REACH = 150.0;

    private PingAim() {}

    /** What the crosshair points at: the block and, if one was hit, the entity on it (-1 when none). */
    public record Aim(BlockPos pos, int entityId) {}

    public static Optional<Aim> aim(Minecraft mc) {
        if (mc.player == null || mc.level == null) return Optional.empty();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(REACH));

        double bestDist = REACH;
        BlockPos best = null;
        int bestEntity = -1;
        BlockHitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        if (block.getType() != HitResult.Type.MISS) {
            bestDist = eye.distanceTo(block.getLocation());
            best = block.getBlockPos();
        }
        AABB sweep = new AABB(eye, end).inflate(2.0);
        for (Entity entity : mc.level.getEntities(mc.player, sweep,
                target -> target instanceof LivingEntity living && living.isAlive() && target.isPickable())) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.5).clip(eye, end);
            if (hit.isEmpty()) continue;
            // An entity hit behind a wall does not count: the wall was already closer
            BlockHitResult wall = mc.level.clip(new ClipContext(eye, hit.get(), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            if (wall.getType() != HitResult.Type.MISS && eye.distanceTo(wall.getLocation()) < eye.distanceTo(hit.get()) - 0.5) continue;
            double dist = eye.distanceTo(hit.get());
            if (dist < bestDist) {
                bestDist = dist;
                best = BlockPos.containing(hit.get());
                bestEntity = entity.getId();
            }
        }
        if (best != null) return Optional.of(new Aim(sanitize(eye, best, end), bestEntity));
        return Optional.of(new Aim(BlockPos.containing(end).immutable(), -1));
    }

    /**
     * Whatever a raycast (or a modded clip override) returns, the mark must stay on the
     * eye's segment: anything farther is garbage, fall back to the max-reach point.
     */
    static BlockPos sanitize(Vec3 eye, BlockPos hit, Vec3 fallback) {
        Vec3 center = hit.getCenter();
        double dx = center.x - eye.x;
        double dy = center.y - eye.y;
        double dz = center.z - eye.z;
        if (!Double.isFinite(dx + dy + dz) || dx * dx + dy * dy + dz * dz > REACH * REACH + 1.0) {
            return BlockPos.containing(fallback);
        }
        return hit.immutable();
    }
}
