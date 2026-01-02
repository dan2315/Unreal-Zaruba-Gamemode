package com.dod.unrealzaruba.NetworkPackets;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GamemodeVotePacket {
    private final String gamemodeId;

    public GamemodeVotePacket(String gamemodeId) {
        this.gamemodeId = gamemodeId;
    }

    public static void encode(GamemodeVotePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.gamemodeId);
    }

    public static GamemodeVotePacket decode(FriendlyByteBuf buf) {
        return new GamemodeVotePacket(buf.readUtf());
    }

    public static void handle(GamemodeVotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                int result = GamemodeManager.instance.Vote(player.getUUID(), msg.gamemodeId);
                if (result == 0) {
                    player.sendSystemMessage(Component.literal("Вы сейчас не можете проголосовать за игровой режим"));
                } else if (result == 1) {
                    player.sendSystemMessage(Component.literal("Ваш голос за " + msg.gamemodeId + " успешно записан"));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
