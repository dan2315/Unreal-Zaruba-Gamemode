package com.dod.unrealzaruba.Economy;

import java.util.List;

import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyCasePacket;

/** The supply cases the server last told this client about. Drawn by {@code SupplyCaseBeams}. */
public final class SupplyCaseClientState {
    private static volatile List<ClientboundSupplyCasePacket.Entry> entries = List.of();

    private SupplyCaseClientState() {}

    public static List<ClientboundSupplyCasePacket.Entry> entries() {
        return entries;
    }

    public static void set(List<ClientboundSupplyCasePacket.Entry> value) {
        entries = List.copyOf(value);
    }
}
