package com.dod.unrealzaruba.Economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundMoneyPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Players' dollars for the current match. Balances live only on the server and start at the map's
 * startingMoney; every change is pushed to the player's HUD.
 */
public final class Wallets {
    private static final Map<UUID, Integer> BALANCES = new HashMap<>();

    private Wallets() {}

    public static int get(ServerPlayer player) {
        return BALANCES.computeIfAbsent(player.getUUID(), id -> EconomyData.current().startingMoney);
    }

    public static void add(ServerPlayer player, int amount) {
        BALANCES.put(player.getUUID(), Math.max(0, get(player) + amount));
        sync(player);
    }

    public static void set(ServerPlayer player, int amount) {
        BALANCES.put(player.getUUID(), Math.max(0, amount));
        sync(player);
    }

    /** Takes the amount if the player has it. */
    public static boolean trySpend(ServerPlayer player, int amount) {
        int balance = get(player);
        if (amount < 0 || balance < amount) {
            return false;
        }
        BALANCES.put(player.getUUID(), balance - amount);
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundMoneyPacket(true, get(player)));
    }

    /** New match: everyone starts again from the new map's startingMoney. */
    public static void reset(MinecraftServer server) {
        BALANCES.clear();
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(Wallets::sync);
        }
    }

    /** Match over: drop balances and hide the HUD. */
    public static void clear(MinecraftServer server) {
        BALANCES.clear();
        if (server != null) {
            PacketDistributor.sendToAllPlayers(new ClientboundMoneyPacket(false, 0));
        }
    }
}
