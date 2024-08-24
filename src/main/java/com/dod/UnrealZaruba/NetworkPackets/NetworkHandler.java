package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;

public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UnrealZaruba.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static final Map<Integer, Long> sentPackets = new HashMap<>();
    private static final int TIMEOUT_MS = 5000; // Timeout period for resending packets
    private static final int RESEND_INTERVAL_MS = 1000; // Interval between resends

    public static void init() {
        CHANNEL.registerMessage(packetId++, LoginPacket.class,
                LoginPacket::encode,
                LoginPacket::decode,
                LoginPacket::handle);

        CHANNEL.registerMessage(packetId++, SaveTokensPacket.class,
                SaveTokensPacket::encode,
                SaveTokensPacket::decode,
                SaveTokensPacket::handle);

        CHANNEL.registerMessage(packetId++, OpenScreenPacket.class,
                OpenScreenPacket::encode,
                OpenScreenPacket::decode,
                OpenScreenPacket::handle);

        CHANNEL.registerMessage(packetId++, VotePlayerPacket.class,
                VotePlayerPacket::encode,
                VotePlayerPacket::decode,
                VotePlayerPacket::handle);

        CHANNEL.registerMessage(packetId++, AcknowledgePacket.class, 
                AcknowledgePacket::encode, 
                AcknowledgePacket::decode, 
                AcknowledgePacket::handle);

        MinecraftForge.EVENT_BUS.register(new NetworkHandler());
    }
    public static void sendToServerWithAck(Object packet, int packetId) {
        CHANNEL.sendToServer(packet);
        sentPackets.put(packetId, System.currentTimeMillis());
    }

    public static void sendToPlayerWithAck(Object packet, int packetId, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        sentPackets.put(packetId, System.currentTimeMillis());
    }

    public static void sendToAllWithAck(Object packet, int packetId) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        sentPackets.put(packetId, System.currentTimeMillis());
    }

    public static void markPacketAsAcknowledged(int packetId) {
        sentPackets.remove(packetId);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            checkForResends();
        }
    }

    

    private void checkForResends() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : sentPackets.entrySet()) {
            int packetId = entry.getKey();
            long sendTime = entry.getValue();

            if (currentTime - sendTime > TIMEOUT_MS) {
                // Resend the packet if it has timed out
                UnrealZaruba.LOGGER.warn("Resending packet ID: " + packetId);
                // Example of resending packet logic; replace with your actual packet resend logic
                resendPacket(packetId);
                sentPackets.put(packetId, currentTime + RESEND_INTERVAL_MS);
            }
        }
    }

    private void resendPacket(int packetId) {
        sentPackets.get(packetId).
        // Implement the logic to resend the specific packet by ID
        // Example: send the corresponding packet again
        // This method needs to know which packet to send, you can store additional packet info if necessary
    }
}
