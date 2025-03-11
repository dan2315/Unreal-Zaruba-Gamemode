package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UnrealZaruba.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static final Map<Integer, PacketMetadata> sentPackets = new HashMap<>();
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

        CHANNEL.registerMessage(packetId++,
                UpdateDeathTimerPacket.class,
                UpdateDeathTimerPacket::encode,
                UpdateDeathTimerPacket::decode,
                UpdateDeathTimerPacket::handle);

        CHANNEL.registerMessage(packetId++, SelectTentPacket.class,
                SelectTentPacket::encode,
                SelectTentPacket::decode,
                SelectTentPacket::handle);

        CHANNEL.registerMessage(packetId++, AcknowledgePacket.class,
                AcknowledgePacket::encode,
                AcknowledgePacket::decode,
                AcknowledgePacket::handle);

        MinecraftForge.EVENT_BUS.register(new NetworkHandler());
    }

    // TODO: Найху удалить систему акноледжмента, ТСП и так релаебл духоя, а чат ГПТ гандон
    // Воистину гандон👌

    public static void sendToServerWithAcknowledgement(Object packet, int packetId, UUID senderUUID) {
        CHANNEL.sendToServer(packet);
        // Store with PacketMetadata including the packet itself
        sentPackets.put(packetId, new PacketMetadata(System.currentTimeMillis(), senderUUID, false, packet));
    }

    public static void sendToPlayerWithAcknowledgement(Object packet, int packetId, ServerPlayer receiver) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), packet);
        // Store with PacketMetadata including the packet itself
        sentPackets.put(packetId, new PacketMetadata(System.currentTimeMillis(), receiver.getUUID(), true, packet));
    }

    public static void markPacketAsAcknowledged(int packetId) {
        sentPackets.remove(packetId);
    }

    // @SubscribeEvent
    // public void onServerTick(TickEvent.ServerTickEvent event) {
    // if (event.phase == TickEvent.Phase.END) {
    // checkForResends();
    // }
    // }

    // @SubscribeEvent
    // public void onClientTick(TickEvent.ClientTickEvent event) {
    // if (event.phase == TickEvent.Phase.END) {
    // checkForResends();
    // }
    // }

    private void checkForResends() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Integer, PacketMetadata> entry : sentPackets.entrySet()) {
            int packetId = entry.getKey();
            PacketMetadata metadata = entry.getValue();

            long sendTime = metadata.getSendTime();
            UUID targetUUID = metadata.getPlayerUUID();
            boolean fromServer = metadata.isFromServer();
            Object packet = metadata.getPacket();

            if (currentTime - sendTime > TIMEOUT_MS) {
                UnrealZaruba.LOGGER.warn("Resending packet ID: " + packetId);

                // Resend the packet using stored packet object
                resendPacket(packet, targetUUID, fromServer);

                // Update the resend time
                sentPackets.put(packetId,
                        new PacketMetadata(currentTime + RESEND_INTERVAL_MS, targetUUID, fromServer, packet));
            }
        }
    }

    private void resendPacket(Object packet, UUID targetUUID, boolean fromServer) {
        if (fromServer) {
            if (targetUUID != null) {
                ServerPlayer receiver = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetUUID);
                if (receiver != null) {
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), packet);
                }
            } else {
                CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
            }
        } else {
            CHANNEL.sendToServer(packet);
        }
    }
}
