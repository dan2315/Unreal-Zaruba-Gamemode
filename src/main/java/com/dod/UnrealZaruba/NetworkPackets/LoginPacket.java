package com.dod.UnrealZaruba.NetworkPackets;

import java.util.UUID;
import java.util.function.Supplier;

import com.dod.UnrealZaruba.DiscordIntegration.DiscordAuth;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class LoginPacket {

    private final String state;
    private final UUID uuid;
    private final String minecraft_username;

    public LoginPacket(String state, UUID uuid, String minecraft_username) {
        this.state = state;
        this.uuid = uuid;
        this.minecraft_username = minecraft_username;
    }

    public String getState() {
        return state;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getMinecraftUsername() {
        return minecraft_username;
    }

    public static <MSG extends LoginPacket> void encode(MSG msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.getState()); 
        buffer.writeUUID(msg.getUuid());
        buffer.writeUtf(msg.getMinecraftUsername());
    }

    public static LoginPacket decode(FriendlyByteBuf buffer) {
        String state = buffer.readUtf(50); 
        UUID uuid = buffer.readUUID();
        String minecraft_username = buffer.readUtf(16); 
        return new LoginPacket(state, uuid, minecraft_username);
    }

    public static void handle(LoginPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleLoginPacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class ClientPacketHandler { 
        public static void handleLoginPacket(LoginPacket msg) {
            Minecraft.getInstance().execute(() -> {
                DiscordAuth.OpenAuthPage(msg.getState(), msg.getUuid(), msg.getMinecraftUsername());
            });
        }
    }
}
