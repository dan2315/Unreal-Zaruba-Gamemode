// package com.dod.UnrealZaruba.Events;

// import java.io.IOException;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.logging.Level;

// import org.valkyrienskies.core.impl.collision.l;

// import com.dod.UnrealZaruba.UnrealZaruba;
// import com.dod.UnrealZaruba.Commands.CommandRegistration;
// import com.dod.UnrealZaruba.WorldManager.WorldManager;

// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.server.level.ServerPlayer;
// import net.minecraft.server.level.TicketType;
// import net.minecraft.world.level.ChunkPos;
// import net.minecraft.world.phys.Vec3;
// import net.minecraftforge.api.distmarker.Dist;
// import net.minecraftforge.event.RegisterCommandsEvent;
// import net.minecraftforge.event.server.ServerStartingEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;

// @Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
// public class ClientEvents {

//     public static ServerLevel serverLevel;

//     @SubscribeEvent
//     public static void onServerStarting(ServerStartingEvent event) {
//         WorldManager.SetupDimensionType();

//         Path worldPath = Paths.get(event.getServer().getServerDirectory().toString(), "saves", "game_world");

//         try (ServerLevel level = WorldManager.LoadWorld(event.getServer(), worldPath)) {
//             if (level != null) {
//                 UnrealZaruba.LOGGER.warn("Custom world loaded successfully!");
//                 serverLevel = level;
//             } else {
//                 UnrealZaruba.LOGGER.error("Failed to load the custom world.");
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

    
//     public static void tpPlayerToNewDim(ServerPlayer player) {
//         // Ensure chunks around the spawn position are loaded
//         ChunkPos pos = new ChunkPos(serverLevel.getSharedSpawnPos());
//         serverLevel.getChunkSource().addRegionTicket(TicketType.FORCED, pos, player.getId(), pos);
//         // Set the spawn position or any other specific position
//         Vec3 spawnPosition = new Vec3(serverLevel.getSharedSpawnPos().getX(), serverLevel.getSharedSpawnPos().getY(), serverLevel.getSharedSpawnPos().getZ());

//         player.changeDimension(serverLevel);
//     }

//     @SubscribeEvent
//     public static void onRegisterCommands(RegisterCommandsEvent event) {
//         UnrealZaruba.LOGGER.info("COMMANDS Registered");
//         CommandRegistration.onCommandRegister(event);
//     }

// }
