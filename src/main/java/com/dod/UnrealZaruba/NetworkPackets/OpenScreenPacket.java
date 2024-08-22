package com.dod.UnrealZaruba.NetworkPackets;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Supplier;

import com.dod.UnrealZaruba.UI.PlayerVoteScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class OpenScreenPacket {

    private final Integer screenID;
    private final List<UUID> teammates;

    public OpenScreenPacket(Integer screenID, List<UUID> teammates) {
        this.screenID = screenID;
        this.teammates = teammates != null ? teammates : new ArrayList<>(); // Handle null teammates list
    }

    public Integer getScreenID() {
        return screenID;
    }

    public List<UUID> getTeammates() {
        return teammates;
    }

    public static void encode(OpenScreenPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.getScreenID());

        // Check if teammates list is present and not empty
        buffer.writeBoolean(!msg.getTeammates().isEmpty());
        if (!msg.getTeammates().isEmpty()) {
            buffer.writeInt(msg.getTeammates().size());
            for (UUID teammate : msg.getTeammates()) {
                buffer.writeUUID(teammate);
            }
        }
    }

    public static OpenScreenPacket decode(FriendlyByteBuf buffer) {
        Integer screenID = buffer.readInt();

        // Read whether the teammates list exists
        boolean hasTeammates = buffer.readBoolean();
        List<UUID> teammates = new ArrayList<>();
        if (hasTeammates) {
            int size = buffer.readInt();
            for (int i = 0; i < size; i++) {
                teammates.add(buffer.readUUID());
            }
        }

        return new OpenScreenPacket(screenID, teammates);
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
                            // Pass the teammates list, which could be empty, to the PlayerVoteScreen
                            Minecraft.getInstance().setScreen(new PlayerVoteScreen(msg.getTeammates()));
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
}
