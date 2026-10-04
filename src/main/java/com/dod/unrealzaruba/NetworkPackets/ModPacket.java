package com.dod.unrealzaruba.NetworkPackets;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Marker for the mod's packets. The payload type is looked up from the registrations in {@link NetworkHandler},
 * so packet classes only need the encode/decode/handle methods they had under Forge's SimpleChannel.
 */
public interface ModPacket extends CustomPacketPayload {
    @Override
    default Type<? extends CustomPacketPayload> type() {
        return NetworkHandler.typeOf(getClass());
    }
}
