package com.dod.UnrealZaruba.NetworkPackets;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.dod.UnrealZaruba.UI.CustomDeathScreen;
import com.dod.UnrealZaruba.UI.PlayerVoteScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class OpenScreenPacket {

    private final Integer screenID;
    private final Map<String, Object> data;

    public OpenScreenPacket(Integer screenID, Map<String, Object> data) {
        this.screenID = screenID;
        this.data = data != null ? data : new HashMap<>();
    }

    public Integer getScreenID() {
        return screenID;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public static void encode(OpenScreenPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.getScreenID());

        // Serialize data based on screenID
        switch (msg.getScreenID()) {
            case 1: // PlayerVoteScreen
                List<UUID> teammates = (List<UUID>) msg.getData().get("teammates");
                buffer.writeBoolean(teammates != null && !teammates.isEmpty());
                if (teammates != null && !teammates.isEmpty()) {
                    buffer.writeInt(teammates.size());
                    for (UUID teammate : teammates) {
                        buffer.writeUUID(teammate);
                    }
                }
                break;
            case 2: // CustomDeathScreen
                Boolean tentExist = (Boolean) msg.getData().get("tentExist");
                buffer.writeBoolean(tentExist != null && tentExist);
                break;
            default:
                break;
        }
    }

    public static OpenScreenPacket decode(FriendlyByteBuf buffer) {
        Integer screenID = buffer.readInt();
        Map<String, Object> data = new HashMap<>();

        // Deserialize data based on screenID
        switch (screenID) {
            case 1: // PlayerVoteScreen
                boolean hasTeammates = buffer.readBoolean();
                if (hasTeammates) {
                    int size = buffer.readInt();
                    List<UUID> teammates = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        teammates.add(buffer.readUUID());
                    }
                    data.put("teammates", teammates);
                }
                break;
            case 2: // CustomDeathScreen
                Boolean tentExist = buffer.readBoolean();
                data.put("tentExist", tentExist);
                break;
            default:
                break;
        }

        return new OpenScreenPacket(screenID, data);
    }

    public static void handle(OpenScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleOpenScreenPacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class ClientPacketHandler {
        public static void handleOpenScreenPacket(OpenScreenPacket msg) {
            Minecraft.getInstance().execute(() -> {
                if (msg.getScreenID() != null) {
                    switch (msg.getScreenID()) {
                        case 1:
                            List<UUID> teammates = (List<UUID>) msg.getData().get("teammates");
                            Minecraft.getInstance().setScreen(new PlayerVoteScreen(teammates));
                            break;
                        case 2:
                            Boolean tentExist = (Boolean) msg.getData().get("tentExist");
                            Minecraft.getInstance().setScreen(new CustomDeathScreen(tentExist));
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
}
