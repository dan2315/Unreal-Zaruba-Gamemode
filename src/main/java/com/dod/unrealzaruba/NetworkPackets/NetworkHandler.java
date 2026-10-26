package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.Gamemodes.RespawnPoints.IRespawnPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.dod.unrealzaruba.NetworkPackets.VehiclePurchase.PurchaseVehiclePacket;
import com.dod.unrealzaruba.NetworkPackets.VehiclePurchase.PurchaseResultPacket;
import com.dod.unrealzaruba.NetworkPackets.CharacterClasses.SetClassPacket;
import com.dod.unrealzaruba.NetworkPackets.CharacterClasses.AssignClassToPlayerPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.BuyItemsPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.BuyResultPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundMoneyPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.OpenBuyMenuPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.RequestBuyMenuPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.RequestTeamStoragePacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenSchematicSpawnerPacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundSchematicSpawnResultPacket;
import com.dod.unrealzaruba.NetworkPackets.Schematics.SpawnSchematicPiecePacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyDropPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyCasePacket;
import com.dod.unrealzaruba.NetworkPackets.Feed.ClientboundFeedPacket;
import com.dod.unrealzaruba.NetworkPackets.TabList.ClientboundTeamRosterPacket;
import com.dod.unrealzaruba.NetworkPackets.Voice.ClientboundVoicePacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundContraptionReportPacket;
import com.dod.unrealzaruba.NetworkPackets.Camera.CameraShotResultPacket;
import com.dod.unrealzaruba.NetworkPackets.Camera.ClientboundCameraShotPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackDataPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackOfferPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.RequestMapPackPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapPackUploadRequestPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.ClientboundMapScanStartPacket;
import com.dod.unrealzaruba.NetworkPackets.MapPack.UploadMapPackPacket;
import com.dod.unrealzaruba.NetworkPackets.RespawnPoints.ClientboundDeathStatePacket;
import com.dod.unrealzaruba.NetworkPackets.RespawnPoints.ClientboundRespawnPointsPacket;

public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";

    private enum Direction { TO_CLIENT, TO_SERVER }

    private record Registration<T extends ModPacket>(
            Direction direction,
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Supplier<PacketContext>> handler) {

        void register(PayloadRegistrar registrar) {
            if (direction == Direction.TO_CLIENT) {
                registrar.playToClient(type, codec, (payload, context) -> handler.accept(payload, () -> new PacketContext(context)));
            } else {
                registrar.playToServer(type, codec, (payload, context) -> handler.accept(payload, () -> new PacketContext(context)));
            }
        }
    }

    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();
    private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new HashMap<>();

    public static void init() {
        toClient("open_screen", OpenScreenPacket.class, OpenScreenPacket::encode, OpenScreenPacket::decode, OpenScreenPacket::handle);
        toServer("vote_player", VotePlayerPacket.class, VotePlayerPacket::encode, VotePlayerPacket::decode, VotePlayerPacket::handle);
        toClient("update_death_timer", UpdateDeathTimerPacket.class, UpdateDeathTimerPacket::encode, UpdateDeathTimerPacket::decode, UpdateDeathTimerPacket::handle);
        toServer("select_respawn_point", SelectRespawnPointPacket.class, SelectRespawnPointPacket::encode, SelectRespawnPointPacket::decode, SelectRespawnPointPacket::handle);
        toServer("purchase_vehicle", PurchaseVehiclePacket.class, PurchaseVehiclePacket::encode, PurchaseVehiclePacket::decode, PurchaseVehiclePacket::handle);
        toClient("purchase_result", PurchaseResultPacket.class, PurchaseResultPacket::encode, PurchaseResultPacket::decode, PurchaseResultPacket::handle);
        toServer("set_class", SetClassPacket.class, SetClassPacket::encode, SetClassPacket::decode, SetClassPacket::handle);
        toServer("assign_class_to_player", AssignClassToPlayerPacket.class, AssignClassToPlayerPacket::encode, AssignClassToPlayerPacket::decode, AssignClassToPlayerPacket::handle);
        toClient("start_hud_timer", StartHudTimerPacket.class, StartHudTimerPacket::encode, StartHudTimerPacket::decode, StartHudTimerPacket::handle);
        toClient("stop_hud_timer", StopHudTimerPacket.class, StopHudTimerPacket::encode, StopHudTimerPacket::decode, StopHudTimerPacket::handle);
        toClient("renderable_zones", RenderableZonesPacket.class, RenderableZonesPacket::encode, RenderableZonesPacket::decode, RenderableZonesPacket::handle);
        toClient("delete_renderable_zones", DeleteRenderableZonesPacket.class, DeleteRenderableZonesPacket::encode, DeleteRenderableZonesPacket::decode, DeleteRenderableZonesPacket::handle);
        toClient("objectives", ClientboundObjectivesPacket.class, ClientboundObjectivesPacket::encode, ClientboundObjectivesPacket::decode, ClientboundObjectivesPacket::handle);
        toClient("update_objective", ClientboundUpdateObjectivePacket.class, ClientboundUpdateObjectivePacket::encode, ClientboundUpdateObjectivePacket::decode, ClientboundUpdateObjectivePacket::handle);
        toClient("update_objectives", ClientboundUpdateObjectivesPacket.class, ClientboundUpdateObjectivesPacket::encode, ClientboundUpdateObjectivesPacket::decode, ClientboundUpdateObjectivesPacket::handle);
        toClient("remove_objectives", ClientboundRemoveObjectivesPacket.class, ClientboundRemoveObjectivesPacket::encode, ClientboundRemoveObjectivesPacket::decode, ClientboundRemoveObjectivesPacket::handle);
        toServer("gamemode_vote", GamemodeVotePacket.class, GamemodeVotePacket::encode, GamemodeVotePacket::decode, GamemodeVotePacket::handle);
        toClient("update_top_score", ClientboundUpdateTopScorePacket.class, ClientboundUpdateTopScorePacket::encode, ClientboundUpdateTopScorePacket::decode, ClientboundUpdateTopScorePacket::handle);
        toClient("money", ClientboundMoneyPacket.class, ClientboundMoneyPacket::encode, ClientboundMoneyPacket::decode, ClientboundMoneyPacket::handle);
        toClient("zone_hints", com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundZoneHintsPacket.class,
                com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundZoneHintsPacket::encode,
                com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundZoneHintsPacket::decode,
                com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundZoneHintsPacket::handle);
        toServer("request_buy_menu", RequestBuyMenuPacket.class, RequestBuyMenuPacket::encode, RequestBuyMenuPacket::decode, RequestBuyMenuPacket::handle);
        toServer("request_team_storage", RequestTeamStoragePacket.class, RequestTeamStoragePacket::encode, RequestTeamStoragePacket::decode, RequestTeamStoragePacket::handle);
        toClient("open_schematic_spawner", ClientboundOpenSchematicSpawnerPacket.class, ClientboundOpenSchematicSpawnerPacket::encode, ClientboundOpenSchematicSpawnerPacket::decode, ClientboundOpenSchematicSpawnerPacket::handle);
        toServer("spawn_schematic_piece", SpawnSchematicPiecePacket.class, SpawnSchematicPiecePacket::encode, SpawnSchematicPiecePacket::decode, SpawnSchematicPiecePacket::handle);
        toClient("schematic_spawn_result", ClientboundSchematicSpawnResultPacket.class, ClientboundSchematicSpawnResultPacket::encode, ClientboundSchematicSpawnResultPacket::decode, ClientboundSchematicSpawnResultPacket::handle);
        toClient("open_buy_menu", OpenBuyMenuPacket.class, OpenBuyMenuPacket::encode, OpenBuyMenuPacket::decode, OpenBuyMenuPacket::handle);
        toServer("buy_items", BuyItemsPacket.class, BuyItemsPacket::encode, BuyItemsPacket::decode, BuyItemsPacket::handle);
        toClient("buy_result", BuyResultPacket.class, BuyResultPacket::encode, BuyResultPacket::decode, BuyResultPacket::handle);
        toClient("supply_drop", ClientboundSupplyDropPacket.class, ClientboundSupplyDropPacket::encode, ClientboundSupplyDropPacket::decode, ClientboundSupplyDropPacket::handle);
        toClient("supply_case", ClientboundSupplyCasePacket.class, ClientboundSupplyCasePacket::encode, ClientboundSupplyCasePacket::decode, ClientboundSupplyCasePacket::handle);
        toClient("feed", ClientboundFeedPacket.class, ClientboundFeedPacket::encode, ClientboundFeedPacket::decode, ClientboundFeedPacket::handle);
        toClient("voice", ClientboundVoicePacket.class, ClientboundVoicePacket::encode, ClientboundVoicePacket::decode, ClientboundVoicePacket::handle);
        toClient("team_roster", ClientboundTeamRosterPacket.class, ClientboundTeamRosterPacket::encode, ClientboundTeamRosterPacket::decode, ClientboundTeamRosterPacket::handle);
        toClient("contraption_report", ClientboundContraptionReportPacket.class, ClientboundContraptionReportPacket::encode, ClientboundContraptionReportPacket::decode, ClientboundContraptionReportPacket::handle);
        toClient("respawn_points", ClientboundRespawnPointsPacket.class, ClientboundRespawnPointsPacket::encode, ClientboundRespawnPointsPacket::decode, ClientboundRespawnPointsPacket::handle);
        toClient("death_state", ClientboundDeathStatePacket.class, ClientboundDeathStatePacket::encode, ClientboundDeathStatePacket::decode, ClientboundDeathStatePacket::handle);
        toClient("map_pack_offer", ClientboundMapPackOfferPacket.class, ClientboundMapPackOfferPacket::encode, ClientboundMapPackOfferPacket::decode, ClientboundMapPackOfferPacket::handle);
        toClient("map_pack_data", ClientboundMapPackDataPacket.class, ClientboundMapPackDataPacket::encode, ClientboundMapPackDataPacket::decode, ClientboundMapPackDataPacket::handle);
        toServer("request_map_pack", RequestMapPackPacket.class, RequestMapPackPacket::encode, RequestMapPackPacket::decode, RequestMapPackPacket::handle);
        toClient("map_pack_upload_request", ClientboundMapPackUploadRequestPacket.class, ClientboundMapPackUploadRequestPacket::encode, ClientboundMapPackUploadRequestPacket::decode, ClientboundMapPackUploadRequestPacket::handle);
        toClient("map_scan_start", ClientboundMapScanStartPacket.class, ClientboundMapScanStartPacket::encode, ClientboundMapScanStartPacket::decode, ClientboundMapScanStartPacket::handle);
        toServer("map_editor", com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket.class, com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket::encode, com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket::decode, com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket::handle);
        toClient("map_editor_state", com.dod.unrealzaruba.NetworkPackets.MapEditor.ClientboundMapEditorStatePacket.class, com.dod.unrealzaruba.NetworkPackets.MapEditor.ClientboundMapEditorStatePacket::encode, com.dod.unrealzaruba.NetworkPackets.MapEditor.ClientboundMapEditorStatePacket::decode, com.dod.unrealzaruba.NetworkPackets.MapEditor.ClientboundMapEditorStatePacket::handle);
        toServer("upload_map_pack", UploadMapPackPacket.class, UploadMapPackPacket::encode, UploadMapPackPacket::decode, UploadMapPackPacket::handle);
        toClient("lobby_state", com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket.class, com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket::encode, com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket::decode, com.dod.unrealzaruba.NetworkPackets.Lobby.ClientboundLobbyStatePacket::handle);
        toServer("flip_vehicle", com.dod.unrealzaruba.NetworkPackets.Vehicles.FlipVehiclePacket.class, com.dod.unrealzaruba.NetworkPackets.Vehicles.FlipVehiclePacket::encode, com.dod.unrealzaruba.NetworkPackets.Vehicles.FlipVehiclePacket::decode, com.dod.unrealzaruba.NetworkPackets.Vehicles.FlipVehiclePacket::handle);
        toServer("give_toolgun", com.dod.unrealzaruba.NetworkPackets.Schematics.GiveToolGunPacket.class, com.dod.unrealzaruba.NetworkPackets.Schematics.GiveToolGunPacket::encode, com.dod.unrealzaruba.NetworkPackets.Schematics.GiveToolGunPacket::decode, com.dod.unrealzaruba.NetworkPackets.Schematics.GiveToolGunPacket::handle);
        toServer("radio_command", com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundRadioPacket.class, com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundRadioPacket::encode, com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundRadioPacket::decode, com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundRadioPacket::handle);
        toClient("radio_broadcast", com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundRadioPacket.class, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundRadioPacket::encode, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundRadioPacket::decode, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundRadioPacket::handle);
        toServer("ping_place", com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundPingPacket.class, com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundPingPacket::encode, com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundPingPacket::decode, com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundPingPacket::handle);
        toClient("ping_broadcast", com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingPacket.class, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingPacket::encode, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingPacket::decode, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingPacket::handle);
        toClient("ping_vehicle", com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingVehiclePacket.class, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingVehiclePacket::encode, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingVehiclePacket::decode, com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingVehiclePacket::handle);
        toServer("toggle_ready", com.dod.unrealzaruba.NetworkPackets.Lobby.ToggleReadyPacket.class, com.dod.unrealzaruba.NetworkPackets.Lobby.ToggleReadyPacket::encode, com.dod.unrealzaruba.NetworkPackets.Lobby.ToggleReadyPacket::decode, com.dod.unrealzaruba.NetworkPackets.Lobby.ToggleReadyPacket::handle);
        toClient("purchase_session", com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket.class, com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket::encode, com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket::decode, com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundPurchaseSessionPacket::handle);
        toClient("open_purchase_session", com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenPurchaseSessionPacket.class, com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenPurchaseSessionPacket::encode, com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenPurchaseSessionPacket::decode, com.dod.unrealzaruba.NetworkPackets.Schematics.ClientboundOpenPurchaseSessionPacket::handle);
        toServer("contribute_purchase", com.dod.unrealzaruba.NetworkPackets.Schematics.ContributePurchasePacket.class, com.dod.unrealzaruba.NetworkPackets.Schematics.ContributePurchasePacket::encode, com.dod.unrealzaruba.NetworkPackets.Schematics.ContributePurchasePacket::decode, com.dod.unrealzaruba.NetworkPackets.Schematics.ContributePurchasePacket::handle);
        toClient("team_vehicles", com.dod.unrealzaruba.NetworkPackets.Vehicles.ClientboundTeamVehiclesPacket.class, com.dod.unrealzaruba.NetworkPackets.Vehicles.ClientboundTeamVehiclesPacket::encode, com.dod.unrealzaruba.NetworkPackets.Vehicles.ClientboundTeamVehiclesPacket::decode, com.dod.unrealzaruba.NetworkPackets.Vehicles.ClientboundTeamVehiclesPacket::handle);
        toClient("camera_shot", ClientboundCameraShotPacket.class, ClientboundCameraShotPacket::encode, ClientboundCameraShotPacket::decode, ClientboundCameraShotPacket::handle);
        toServer("camera_shot_result", CameraShotResultPacket.class, CameraShotResultPacket::encode, CameraShotResultPacket::decode, CameraShotResultPacket::handle);
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        for (Registration<?> registration : REGISTRATIONS) {
            registration.register(registrar);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends ModPacket> CustomPacketPayload.Type<T> typeOf(Class<?> packetClass) {
        CustomPacketPayload.Type<?> type = TYPES.get(packetClass);
        if (type == null) {
            throw new IllegalStateException("Packet not registered: " + packetClass.getName());
        }
        return (CustomPacketPayload.Type<T>) type;
    }

    private static <T extends ModPacket> void toClient(String name, Class<T> packetClass, BiConsumer<T, FriendlyByteBuf> encoder,
                                                       Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<PacketContext>> handler) {
        add(Direction.TO_CLIENT, name, packetClass, encoder, decoder, handler);
    }

    private static <T extends ModPacket> void toServer(String name, Class<T> packetClass, BiConsumer<T, FriendlyByteBuf> encoder,
                                                       Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<PacketContext>> handler) {
        add(Direction.TO_SERVER, name, packetClass, encoder, decoder, handler);
    }

    private static <T extends ModPacket> void add(Direction direction, String name, Class<T> packetClass, BiConsumer<T, FriendlyByteBuf> encoder,
                                                  Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<PacketContext>> handler) {
        var type = new CustomPacketPayload.Type<T>(ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, name));
        StreamCodec<RegistryFriendlyByteBuf, T> codec = StreamCodec.of((buf, packet) -> encoder.accept(packet, buf), decoder::apply);
        TYPES.put(packetClass, type);
        REGISTRATIONS.add(new Registration<>(direction, type, codec, handler));
    }

    public class Screens {
        public static void openDeathScreen(ServerPlayer player, List<IRespawnPoint> respawnPoints) {
            PacketDistributor.sendToPlayer(player,
                new OpenScreenPacket(OpenScreenPacket.ScreenType.DEATH_SCREEN.id(), new HashMap<>() {{
                    put("respawnPoints", respawnPoints);
                }})
            );
        }

        public static void openClassAssignerScreen(ServerPlayer player, BlockPos blockPos, String currentClassId) {
            boolean isAdmin = player.hasPermissions(2); // Check if player has OP level 2 or higher

            Map<String, Object> data = new HashMap<>();
            data.put("blockPos", blockPos);
            data.put("currentClassId", currentClassId);
            data.put("isAdmin", isAdmin);

            PacketDistributor.sendToPlayer(player, new OpenScreenPacket(3, data));
        }
    }
}
