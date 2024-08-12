//package net.tardis.mod.helper;
//
//import java.util.Map;
//import java.util.concurrent.Executor;
//import java.util.function.BiFunction;
//
//import com.google.common.collect.ImmutableList;
//import com.mojang.serialization.Lifecycle;
//
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.util.RegistryKey;
//import net.minecraft.util.registry.Registry;
//import net.minecraft.world.Dimension;
//import net.minecraft.world.World;
//import net.minecraft.world.biome.BiomeManager;
//import net.minecraft.world.border.IBorderListener;
//import net.minecraft.world.chunk.listener.IChunkStatusListener;
//import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
//import net.minecraft.world.server.ServerWorld;
//import net.minecraft.world.storage.DerivedWorldInfo;
//import net.minecraft.world.storage.IServerConfiguration;
//import net.minecraft.world.storage.SaveFormat.LevelSave;
//import net.minecraft.world.level.storage.*;;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.event.world.WorldEvent;
//// import net.tardis.mod.network.Network;
//// import net.tardis.mod.network.packets.SyncDimensionListMessage;
///** A class that contains helper functions to register code based dimensions
// * <br> Not recommended for dimensions that do not need to be created dynamically (during runtime), as it uses vanilla internals, causes a temp server performance spike when initially called and requires extra syncing packets
// * <br> For non dynamic dimensions, make a json dimension file as specified in the MC wiki
// * <p> Created by 50ap5ud5 on 20th December 2020*/
//public class DimensionHelper
//{
//
//	/**
//	 * Gets a world, dynamically creating and registering one if it doesn't exist.<br>
//	 * The dimension registry is stored in the server's level file, all previously registered dimensions are loaded
//	 * and recreated and reregistered whenever the server starts.<br>
//	 * Static, singular dimensions can be registered via the getOrCreateWorldStatic method
//	 * in the FMLServerStartingEvent, which runs immediately after existing dimensions are loaded and registered.<br>
//	 * Dynamic dimensions seem to be able to be registered at runtime with no repercussions aside from
//	 * lagging the server for a couple seconds while the world initializes.
//	 * @param server a MinecraftServer instance (you can get this from a ServerPlayerEntity or ServerWorld)
//	 * @param worldKey A RegistryKey for your world, you can make one via RegistryKey.getOrCreateKey(Registry.WORLD_KEY, yourWorldResourceLocation);
//	 * @param dimensionFactory A function that produces a new Dimension instance if necessary, given the server and dimension id<br>
//	 * (dimension ID will be the same as the world ID from worldKey)<br>
//	 * It should be assumed that intended dimension has not been created or registered yet,
//	 * so making the factory attempt to get this dimension from the server's dimension registry will fail
//	 * @return Returns a ServerWorld, creating and registering a world and dimension for it if the world does not already exist
//	 */
//	public static ServerWorld getOrCreateWorld(MinecraftServer server, RegistryKey<World> worldKey, BiFunction<MinecraftServer, RegistryKey<Dimension>, Dimension> dimensionFactory)
//	{
//
//		// this is marked as deprecated but it's not called from anywhere and I'm not sure how old it is,
//		// it's probably left over from forge's previous dimension api
//		// in any case we need to get at the server's world field, and if we didn't use this getter,
//		// then we'd just end up making a private-field-getter for it ourselves anyway
//		@SuppressWarnings("deprecation")
//		Map<RegistryKey<World>, ServerWorld> map = server.forgeGetWorldMap();
//
//		// if the world already exists, return it
//		if (map.containsKey(worldKey))
//		{
//			return map.get(worldKey);
//		}
//		else
//		{
//			// for vanilla worlds, forge fires the world load event *after* the world is put into the map
//			// we'll do the same for consistency
//			// (this is why we're not just using map::computeIfAbsent)
//			ServerWorld newWorld = createAndRegisterDynamicWorldAndDimension(server, map, worldKey, dimensionFactory);
//
//			return newWorld;
//		}
//	}
//	/**
//	 * Gets or creates a ServerWorld for use in statically initilised dimensions (Non dynamic)
//	 * <br> After registering the world, sends a packet to all online players to refresh their local dimension list
//	 * @param server
//	 * @param worldKey
//	 * @param dimensionFactory
//	 * @return
//	 */
//	public static ServerWorld getOrCreateWorldStatic(MinecraftServer server, RegistryKey<World> worldKey, BiFunction<MinecraftServer, RegistryKey<Dimension>, Dimension> dimensionFactory)
//	{
//		// this is marked as deprecated but it's not called from anywhere and I'm not sure how old it is,
//		// it's probably left over from forge's previous dimension api
//		// in any case we need to get at the server's world field, and if we didn't use this getter,
//		// then we'd just end up making a private-field-getter for it ourselves anyway
//		@SuppressWarnings("deprecation")
//		Map<RegistryKey<World>, ServerWorld> map = server.forgeGetWorldMap();
//
//		// if the world already exists, return it
//		if (map.containsKey(worldKey))
//		{
//			return map.get(worldKey);
//		}
//		else
//		{
//			ServerWorld newWorld = createAndRegisterWorldAndDimension(server, map, worldKey, dimensionFactory);
//			return newWorld;
//		}
//	}
//
//	/**
//	 * Create and register a dimension dynamically
//	 * <br> This method adds a syncing packet for the client's /execute command suggestion list
//	 * <br> DO NOT use this for statically created dimensions as it will crash dedicated servers.
//	 * @param server
//	 * @param map
//	 * @param worldKey
//	 * @param dimensionFactory
//	 * @return
//	 */
//	private static ServerWorld createAndRegisterDynamicWorldAndDimension(MinecraftServer server, Map<RegistryKey<World>, ServerWorld> map, RegistryKey<World> worldKey, BiFunction<MinecraftServer, RegistryKey<Dimension>, Dimension> dimensionFactory)
//	{
//		ServerWorld newWorld = createAndRegisterWorldAndDimension(server, map, worldKey, dimensionFactory);
//		Network.sendPacketToAll(new SyncDimensionListMessage(worldKey, true)); //Sync dimension list to client for use in /execute command
//		return newWorld;
//	}
//
//	@SuppressWarnings("deprecation") // markWorldsDirty is deprecated, see below
//	private static ServerWorld createAndRegisterWorldAndDimension(MinecraftServer server, Map<RegistryKey<World>, ServerWorld> map, RegistryKey<World> worldKey, BiFunction<MinecraftServer, RegistryKey<Dimension>, Dimension> dimensionFactory)
//	{
//		ServerWorld overworld = server.getWorld(World.OVERWORLD);
//		RegistryKey<Dimension> dimensionKey = RegistryKey.getOrCreateKey(Registry.DIMENSION_KEY, worldKey.getLocation());
//		Dimension dimension = dimensionFactory.apply(server, dimensionKey);
//
//		// we need to get some private fields from MinecraftServer here
//			// chunkStatusListenerFactory
//			// backgroundExecutor
//			// anvilConverterForAnvilFile
//		// the int in create() here is radius of chunks to watch, 11 is what the server uses when it initializes worlds
//		IChunkStatusListener chunkListener = server.chunkStatusListenerFactory.create(11);
//		Executor executor = server.backgroundExecutor;
//		LevelSave levelSave = server.anvilConverterForAnvilFile;
//
//		// this is the same order server init creates these worlds:
//		// instantiate world, add border listener, add to map, fire world load event
//		// (in server init, the dimension is already in the dimension registry,
//			// that'll get registered here before the world is instantiated as well)
//
//		IServerConfiguration serverConfig = server.getServerConfiguration();
//		DimensionGeneratorSettings dimensionGeneratorSettings = serverConfig.getDimensionGeneratorSettings();
//		// this next line registers the Dimension
//		dimensionGeneratorSettings.func_236224_e_().register(dimensionKey, dimension, Lifecycle.stable()); //set to stable to reduce issues that come with the experimental marker
//		DerivedWorldInfo derivedWorldInfo = new DerivedWorldInfo(serverConfig, serverConfig.getServerWorldInfo());
//		// now we have everything we need to create the world instance
//		ServerWorld newWorld = new ServerWorld(
//			server,
//			executor,
//			levelSave,
//			derivedWorldInfo,
//			worldKey,
//			dimension.getDimensionType(),
//			chunkListener,
//			dimension.getChunkGenerator(),
//			dimensionGeneratorSettings.hasDebugChunkGenerator(), // boolean: is-debug-world
//			BiomeManager.getHashedSeed(dimensionGeneratorSettings.getSeed()),
//			ImmutableList.of(), // "special spawn list"
//				// phantoms, raiders, travelling traders, cats are overworld special spawns
//				// the dimension loader is hardcoded to initialize preexisting non-overworld worlds with no special spawn lists
//				// so this can probably be left empty for best results and spawns should be handled via other means
//			false); // "tick time", true for overworld, always false for everything else
//
//		// add world border listener
//		overworld.getWorldBorder().addListener(new IBorderListener.Impl(newWorld.getWorldBorder()));
//
//		// register world
//		map.put(worldKey, newWorld);
//
//		// update forge's world cache (very important, if we don't do this then the new world won't tick!)
//		server.markWorldsDirty();
//
//		// fire world load event
//		MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(newWorld)); // event isn't cancellable
//		System.out.println("Registered new dimension of: " + dimensionKey.getLocation().toString());
//		return newWorld;
//	}
//}
