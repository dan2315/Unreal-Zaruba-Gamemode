package com.dod.UnrealZaruba.NetworkPackets;

import java.io.IOException;
import java.util.function.Supplier;

import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.DiscordIntegration.Tokens;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SaveTokensPacket {

    private final String token;
    private final String refreshToken;

    public SaveTokensPacket(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public static void encode(SaveTokensPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.getToken());
        buffer.writeUtf(msg.getRefreshToken());
    }

    public static SaveTokensPacket decode(FriendlyByteBuf buffer) {
        String token = buffer.readUtf(256); // assuming token length up to 256 chars
        String refreshToken = buffer.readUtf(256); // assuming refresh token length up to 256 chars
        return new SaveTokensPacket(token, refreshToken);
    }

    public static void handle(SaveTokensPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleSaveTokensPacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class ClientPacketHandler {
        public static void handleSaveTokensPacket(SaveTokensPacket msg) {
            Minecraft.getInstance().execute(() -> {
                try {
                    ConfigManager.saveConfig(ConfigManager.Tokens, new Tokens(msg.getToken(), msg.getRefreshToken()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
