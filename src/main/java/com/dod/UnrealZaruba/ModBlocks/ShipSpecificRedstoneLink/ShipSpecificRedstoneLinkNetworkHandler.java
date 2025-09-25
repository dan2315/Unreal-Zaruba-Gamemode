package com.dod.unrealzaruba.ModBlocks.ShipSpecificRedstoneLink;
// package com.dod.unrealzaruba.ModBlocks.ShipSpecificRestoneLink;

// import java.util.HashMap;
// import java.util.IdentityHashMap;
// import java.util.Iterator;
// import java.util.LinkedHashSet;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.atomic.AtomicInteger;

// import com.simibubi.create.Create;
// import com.simibubi.create.content.redstone.link.LinkBehaviour;
// import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
// import com.simibubi.create.foundation.utility.Couple;
// import com.simibubi.create.foundation.utility.WorldHelper;
// import com.simibubi.create.infrastructure.config.AllConfigs;

// import net.minecraft.util.Tuple;
// import net.minecraft.world.level.LevelAccessor;
// import oshi.util.tuples.Triplet;

// public class ShipSpecificRedstoneLinkNetworkHandler {

// 	static final Map<LevelAccessor, Map<Triplet<Frequency, Frequency, Integer>, Set<IRedstoneLinkable>>> connections =
// 		new IdentityHashMap<>();

// 	public final AtomicInteger globalPowerVersion = new AtomicInteger();

// 	public void onLoadWorld(LevelAccessor world) {
// 		connections.put(world, new HashMap<>());
// 		Create.LOGGER.debug("Prepared Redstone Network Space for " + WorldHelper.getDimensionID(world));
// 	}

// 	public void onUnloadWorld(LevelAccessor world) {
// 		connections.remove(world);
// 		Create.LOGGER.debug("Removed Redstone Network Space for " + WorldHelper.getDimensionID(world));
// 	}

// 	public Set<IRedstoneLinkable> getNetworkOf(LevelAccessor world, IRedstoneLinkable actor) {
// 		Map<Triplet<Frequency, Frequency, Integer>, Set<IRedstoneLinkable>> networksInWorld = networksIn(world);
// 		Triplet<Frequency, Frequency, Integer> key = actor.getNetworkKey();
// 		if (!networksInWorld.containsKey(key))
// 			networksInWorld.put(key, new LinkedHashSet<>());
// 		return networksInWorld.get(key);
// 	}

// 	public void addToNetwork(LevelAccessor world, IRedstoneLinkable actor) {
// 		getNetworkOf(world, actor).add(actor);
// 		updateNetworkOf(world, actor);
// 	}

// 	public void removeFromNetwork(LevelAccessor world, IRedstoneLinkable actor) {
// 		Set<IRedstoneLinkable> network = getNetworkOf(world, actor);
// 		network.remove(actor);
// 		if (network.isEmpty()) {
// 			networksIn(world).remove(actor.getNetworkKey());
// 			return;
// 		}
// 		updateNetworkOf(world, actor);
// 	}

// 	public void updateNetworkOf(LevelAccessor world, IRedstoneLinkable actor) {
// 		Set<IRedstoneLinkable> network = getNetworkOf(world, actor);
// 		globalPowerVersion.incrementAndGet();
// 		int power = 0;

// 		for (Iterator<IRedstoneLinkable> iterator = network.iterator(); iterator.hasNext();) {
// 			IRedstoneLinkable other = iterator.next();
// 			if (!other.isAlive()) {
// 				iterator.remove();
// 				continue;
// 			}
			
// 			if (!withinRange(actor, other))
// 				continue;

// 			if (power < 15)
// 				power = Math.max(other.getTransmittedStrength(), power);
// 		}

// 		if (actor instanceof LinkBehaviour) {
// 			LinkBehaviour linkBehaviour = (LinkBehaviour) actor;
// 			// fix one-to-one loading order problem
// 			if (linkBehaviour.isListening()) {
// 				linkBehaviour.newPosition = true;
// 				linkBehaviour.setReceivedStrength(power);
// 			}
// 		}

// 		for (IRedstoneLinkable other : network) {
// 			if (other != actor && other.isListening() && withinRange(actor, other))
// 				other.setReceivedStrength(power);
// 		}
// 	}

// 	public static boolean withinRange(IRedstoneLinkable from, IRedstoneLinkable to) {
// 		if (from == to)
// 			return true;
// 		return from.getLocation()
// 			.closerThan(to.getLocation(), AllConfigs.server().logistics.linkRange.get());
// 	}

// 	public Map<Triplet<Frequency, Frequency, Integer>, Set<IRedstoneLinkable>> networksIn(LevelAccessor world) {
// 		if (!connections.containsKey(world)) {
// 			Create.LOGGER.warn("Tried to Access unprepared network space of " + WorldHelper.getDimensionID(world));
// 			return new HashMap<>();
// 		}
// 		return connections.get(world);
// 	}

// 	public boolean hasAnyLoadedPower(Triplet<Frequency, Frequency, Integer> frequency) {
// 		for (Map<Triplet<Frequency, Frequency, Integer>, Set<IRedstoneLinkable>> map : connections.values()) {
// 			Set<IRedstoneLinkable> set = map.get(frequency);
// 			if (set == null || set.isEmpty())
// 				continue;
// 			for (IRedstoneLinkable link : set)
// 				if (link.getTransmittedStrength() > 0)
// 					return true;
// 		}
// 		return false;
// 	}

// 	private Triplet<Frequency, Frequency, Integer> ConvertKeyToShipSpecificFormat(IRedstoneLinkable actor)
// 	{
// 		actor.getNetworkKey();
// 		return new Tr
// 	}
// }
