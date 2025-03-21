package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;


public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UnrealZaruba.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;


    public static void init() {
        CHANNEL.registerMessage(packetId++, OpenScreenPacket.class,
                OpenScreenPacket::encode,
                OpenScreenPacket::decode,
                OpenScreenPacket::handle);

        CHANNEL.registerMessage(packetId++, VotePlayerPacket.class,
                VotePlayerPacket::encode,
                VotePlayerPacket::decode,
                VotePlayerPacket::handle);

        CHANNEL.registerMessage(packetId++,
                UpdateDeathTimerPacket.class,
                UpdateDeathTimerPacket::encode,
                UpdateDeathTimerPacket::decode,
                UpdateDeathTimerPacket::handle);

        CHANNEL.registerMessage(packetId++, SelectTentPacket.class,
                SelectTentPacket::encode,
                SelectTentPacket::decode,
                SelectTentPacket::handle);

        MinecraftForge.EVENT_BUS.register(new NetworkHandler());
    }
}
