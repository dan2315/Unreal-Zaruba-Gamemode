package com.dod.unrealzaruba.Schematics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket;

import net.minecraft.core.BlockPos;

/** The vehicle purchases going on near this player, as the server last described them. */
public final class PurchaseClientState {
    /** {@code deadlineMillis}: when it ends unless somebody adds money, on this machine's clock. */
    public record View(BlockPos pos, long schematicId, String name, int price, int paid, long deadlineMillis,
                       double centerX, double centerZ, double minimumBlockCenterY, String initiator,
                       Map<String, Integer> contributors) {
        public int remaining() {
            return Math.max(0, price - paid);
        }

        public float fraction() {
            return price <= 0 ? 0 : Math.min(1f, paid / (float) price);
        }

        public int secondsLeft() {
            return (int) Math.max(0, (deadlineMillis - System.currentTimeMillis() + 999) / 1000);
        }

        /** The smallest contribution: a hundredth of the price, or what is left when that is more. Same rule as the server. */
        public int minimumContribution() {
            return Math.min(Math.max(1, remaining()), Math.max(1, price / 100));
        }
    }

    private static final Map<BlockPos, View> VIEWS = new HashMap<>();

    private PurchaseClientState() {}

    public static void set(ClientboundPurchaseSessionPacket packet) {
        if (!packet.active) {
            VIEWS.remove(packet.pos);
            return;
        }
        VIEWS.put(packet.pos, new View(packet.pos, packet.schematicId, packet.name, packet.price, packet.paid,
                System.currentTimeMillis() + packet.ticksLeft * 50L, packet.centerX, packet.centerZ,
                packet.minimumBlockCenterY, packet.initiator, packet.contributors));
    }

    public static View get(BlockPos pos) {
        return VIEWS.get(pos);
    }

    public static Collection<View> all() {
        return VIEWS.values();
    }

    public static void reset() {
        VIEWS.clear();
    }
}
