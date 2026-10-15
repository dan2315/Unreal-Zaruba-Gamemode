package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyDropPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Supply drops come down by parachute ({@link SupplyParadrop}): during the battle one lands at a random spot of the
 * configured area every few minutes, with the supply case and random loot in its barrel. This class runs that schedule,
 * resets the drops with a new match and tells the clients where the newest one is, for the map waypoint.
 */
public final class SupplyDrops {
    private static final RandomSource RANDOM = RandomSource.create();

    private SupplyDrops() {}

    /** A new match. */
    public static void reset() {
        SupplyParadrop.clear();
        SupplyCases.reset();
    }

    /** Called every tick of the battle. */
    static void tick(MinecraftServer server, EconomyConfig.SupplyDrops config, int battleTicks) {
        if (config == null || !config.enabled || !config.paradrop.auto) return;
        SupplyParadrop.tickAuto(server, config, battleTicks);
    }

    /** {@code rolls} weighted picks from {@code loot}; entries that don't parse are skipped. */
    static List<ItemStack> rollLoot(MinecraftServer server, List<EconomyConfig.Loot> table, int rolls) {
        List<ItemStack> stacks = new ArrayList<>();
        List<EconomyConfig.Loot> loot = table == null ? List.of() : table;
        int totalWeight = loot.stream().mapToInt(entry -> Math.max(0, entry.weight)).sum();
        if (totalWeight <= 0) return stacks;
        for (int roll = 0; roll < rolls; roll++) {
            int pick = RANDOM.nextInt(totalWeight);
            for (EconomyConfig.Loot entry : loot) {
                pick -= Math.max(0, entry.weight);
                if (pick < 0) {
                    ItemStack stack = ItemSpecs.parse(server.registryAccess(), entry.item, entry.count);
                    if (!stack.isEmpty()) stacks.add(stack);
                    break;
                }
            }
        }
        return stacks;
    }

    // ---- clients (map waypoint) ----

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet());
    }

    /** A parachute drop appeared or went away: tell the clients where to put the map waypoint. */
    static void refreshWaypoint(MinecraftServer server) {
        if (server != null) PacketDistributor.sendToAllPlayers(packet());
    }

    private static ClientboundSupplyDropPacket packet() {
        return new ClientboundSupplyDropPacket(SupplyParadrop.waypoint());
    }
}
