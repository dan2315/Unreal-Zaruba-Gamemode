package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UnrealZaruba;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UnrealZaruba.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static final ResourceLocation VELOCITY_CHANNEL_LOCATION = new ResourceLocation("velocity", "main");
    public static final SimpleChannel VELOCITY_CHANNEL = NetworkRegistry.newSimpleChannel(
        VELOCITY_CHANNEL_LOCATION,
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

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

    }

    public class Screens {
        public static void openDeathScreen(ServerPlayer player, boolean tentExist) {
            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenScreenPacket(2, new HashMap<String, Object>() {{
                    put("tentExist", tentExist);
                }})
            );
        }
    }
}
