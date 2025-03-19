package com.dod.UnrealZaruba.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;


@Deprecated
public class Explosions {

    /**
     * Метод вызывает краш предположительно с рекурсивным поведением
     *
     * @param world
     * @param sourceEntity
     * @param pos
     * @param strength
     */
    public void createExplosionNoBlockDamage(Level world, Entity sourceEntity, BlockPos pos, float strength) {
        // Create the explosion with custom parameters
        world.explode(
                sourceEntity,       // The entity that caused the explosion, can be null
                pos.getX() + 0.5,         // X coordinate of the explosion
                pos.getY()  + 0.5,         // Y coordinate of the explosion
                pos.getZ()  + 0.5,         // Z coordinate of the explosion
                strength,                     // Explosion strength (like TNT is 4.0F)
                Level.ExplosionInteraction.NONE // Block interaction type: NONE means no block damage
        );
    }

    /**
     * Метод вызывает краш предположительно с рекурсивным поведением
     *
     * @param world
     * @param sourceEntity
     * @param pos
     * @param strength
     */
    public static void createExplosionNoPlayerDamage(Level world, Entity sourceEntity, BlockPos pos, float strength) {
        Explosion explosion = new Explosion(world, sourceEntity, null, null, pos.getX(), pos.getY(), pos.getZ(), strength, false, Explosion.BlockInteraction.DESTROY);
        explosion.explode();

        AABB explosionArea = new AABB(
                pos.getX() - strength, pos.getY() - strength, pos.getZ() - strength,
                pos.getX() + strength, pos.getY() + strength, pos.getZ() + strength
        );

        List<Entity> entitiesInRange = world.getEntities(sourceEntity, explosionArea);

        for (Entity entity : entitiesInRange) {
            if (!(entity instanceof Player)) {
                entity.hurt(explosion.getDamageSource(), strength);
            }
        }

        explosion.finalizeExplosion(true);
    }
}
