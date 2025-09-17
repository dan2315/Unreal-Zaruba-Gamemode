package com.dod.UnrealZaruba.NetworkPackets;

import com.dod.UnrealZaruba.UnrealZaruba;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.dod.UnrealZaruba.NetworkPackets.VehiclePurchase.PurchaseVehiclePacket;
import com.dod.UnrealZaruba.NetworkPackets.VehiclePurchase.PurchaseResultPacket;
import com.dod.UnrealZaruba.NetworkPackets.CharacterClasses.SetClassPacket;
import com.dod.UnrealZaruba.NetworkPackets.CharacterClasses.AssignClassToPlayerPacket;

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

        CHANNEL.registerMessage(packetId++, UpdateDeathTimerPacket.class,
                UpdateDeathTimerPacket::encode,
                UpdateDeathTimerPacket::decode,
                UpdateDeathTimerPacket::handle);

        CHANNEL.registerMessage(packetId++, SelectRespawnPointPacket.class,
                SelectRespawnPointPacket::encode,
                SelectRespawnPointPacket::decode,
                SelectRespawnPointPacket::handle);

        CHANNEL.registerMessage(packetId++, PurchaseVehiclePacket.class,
                PurchaseVehiclePacket::encode,
                PurchaseVehiclePacket::decode,
                PurchaseVehiclePacket::handle);

        CHANNEL.registerMessage(packetId++, PurchaseResultPacket.class,
                PurchaseResultPacket::encode,
                PurchaseResultPacket::decode,
                PurchaseResultPacket::handle);

        CHANNEL.registerMessage(packetId++, SetClassPacket.class,
                SetClassPacket::encode,
                SetClassPacket::decode,
                SetClassPacket::handle);
                
        CHANNEL.registerMessage(packetId++, AssignClassToPlayerPacket.class,
                AssignClassToPlayerPacket::encode,
                AssignClassToPlayerPacket::decode,
                AssignClassToPlayerPacket::handle);

        CHANNEL.registerMessage(packetId++, StartHudTimerPacket.class,
                StartHudTimerPacket::encode,
                StartHudTimerPacket::decode,
                StartHudTimerPacket::handle);

        CHANNEL.registerMessage(packetId++, StopHudTimerPacket.class,
                StopHudTimerPacket::encode,
                StopHudTimerPacket::decode,
                StopHudTimerPacket::handle);

        CHANNEL.registerMessage(packetId++, RenderableZonesPacket.class,
                RenderableZonesPacket::encode,
                RenderableZonesPacket::decode,
                RenderableZonesPacket::handle);

        CHANNEL.registerMessage(packetId++, ClientboundObjectivesPacket.class,
                ClientboundObjectivesPacket::encode,
                ClientboundObjectivesPacket::decode,
                ClientboundObjectivesPacket::handle);

        CHANNEL.registerMessage(packetId++, ClientboundUpdateObjectivePacket.class,
                ClientboundUpdateObjectivePacket::encode,
                ClientboundUpdateObjectivePacket::decode,
                ClientboundUpdateObjectivePacket::handle);

        CHANNEL.registerMessage(packetId++, ClientboundUpdateObjectivesPacket.class,
                ClientboundUpdateObjectivesPacket::encode,
                ClientboundUpdateObjectivesPacket::decode,
                ClientboundUpdateObjectivesPacket::handle);

        CHANNEL.registerMessage(packetId++, GamemodeVotePacket.class,
                GamemodeVotePacket::encode,
                GamemodeVotePacket::decode,
                GamemodeVotePacket::handle);

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
        
        public static void openClassAssignerScreen(ServerPlayer player, BlockPos blockPos, String currentClassId) {
            boolean isAdmin = player.hasPermissions(2); // Check if player has OP level 2 or higher
            
            Map<String, Object> data = new HashMap<>();
            data.put("blockPos", blockPos);
            data.put("currentClassId", currentClassId);
            data.put("isAdmin", isAdmin);
            
            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenScreenPacket(3, data)
            );
        }
    }
}
