package com.dod.unrealzaruba.WorldManager;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * The game map has no mobs: the ones saved in the map never load, nothing spawns on its own, and {@link #sweep} clears
 * whatever got in. Only this mod's own entities (NPCs and the like) are allowed there.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID)
public final class MapMobs {
    private MapMobs() {}

    /** A mob that does not belong on the game map. */
    public static boolean unwanted(Entity entity) {
        return entity instanceof Mob
                && !UnrealZaruba.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
    }

    private static boolean inGameMap(net.minecraft.world.level.Level level) {
        return level != null && level.dimension().equals(WorldManager.GAME_DIMENSION);
    }

    /** Mobs saved in the map, mobs created by other mods: they never enter the game level. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (inGameMap(event.getLevel()) && unwanted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Natural spawns, spawners and structures: stopped before the mob is even set up. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (inGameMap(event.getLevel().getLevel()) && unwanted(event.getEntity())) {
            event.setSpawnCancelled(true);
        }
    }

    /** Removes the mobs that are in the level now. Returns how many. */
    public static int sweep(ServerLevel level) {
        if (level == null) return 0;
        List<Entity> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (unwanted(entity)) found.add(entity);
        }
        for (Entity entity : found) {
            entity.discard();
        }
        if (!found.isEmpty()) {
            UnrealZaruba.LOGGER.info("[MapMobs] Cleared {} mobs from the game map", found.size());
        }
        return found.size();
    }
}
