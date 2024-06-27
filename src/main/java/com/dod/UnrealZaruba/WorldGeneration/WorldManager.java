package com.dod.UnrealZaruba.WorldGeneration;

import com.dod.UnrealZaruba.unrealzaruba;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class WorldManager {
    public static void Initialize() {

    }

    private static final int WORLD_COUNT = 30;
    private List<ServerLevel> availableWorlds = new ArrayList<>();

    public void createFlatWorlds(MinecraftServer server) {
        for (int i = 0; i < WORLD_COUNT; i++) {
            ResourceKey<Level> worldKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(unrealzaruba.MOD_ID, "player_world_" + i));
            ServerLevel world = generateFlatWorld(server, worldKey);
            availableWorlds.add(world);
        }
    }

    private ServerLevel generateFlatWorld(MinecraftServer server, ResourceKey<Level> worldKey) {
        // Setup flat world generator settings
        FlatLevelGeneratorSettings settings = FlatLevelGeneratorSettings.getDefault(null, null);
        ChunkGenerator generator = new SuperFlatWorldChunkGenerator(null, null, null);
        
        // Create a new world
        ServerLevel newWorld = new ServerLevel(server, server, null, null, worldKey, null, null, generator, false, WORLD_COUNT, null, false);

        return newWorld;
    }

    public ServerLevel assignWorldToPlayer() {
        if (!availableWorlds.isEmpty()) {
            return availableWorlds.remove(0);
        }
        return null;  // Handle case where no worlds are available
    }

    public static void OnPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer().level.isClientSide())
            return;

        Player player = (Player) event.getPlayer();
        MinecraftServer server = player.getServer();

        
    }

}