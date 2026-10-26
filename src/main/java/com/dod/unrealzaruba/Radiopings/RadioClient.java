package com.dod.unrealzaruba.Radiopings;

import java.util.Optional;
import java.util.UUID;

import com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundPingPacket;
import com.dod.unrealzaruba.NetworkPackets.Radio.ServerboundRadioPacket;
import com.dod.unrealzaruba.SoundHandler.ModSounds;
import com.dod.unrealzaruba.UI.Respawn.RespawnClientState;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client side of radio: sending phrases and pings, showing what teammates sent. */
public final class RadioClient {
    private RadioClient() {}

    /** A phrase picked in the radio menu, with the auto-ping of the matching kind. */
    public static void sendPhrase(int phraseId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        RadioPhrase phrase = RadioPhrase.byId(phraseId);
        if (phrase == null) return;
        if (!PingClientState.canRadioNow()) {
            hint("radio.unrealzaruba.cooldown");
            return;
        }
        PingClientState.markRadio();
        PacketDistributor.sendToServer(new ServerboundRadioPacket((byte) phraseId));
        if (phrase.autoPing() != null) {
            // A rally marks the sender's own feet and never sticks to anything;
            // aimed phrases may snap onto a hull or an entity under the crosshair.
            boolean attach = phrase.autoPing() != PingType.RALLY;
            Optional<PingAim.Aim> target = !attach ? Optional.of(new PingAim.Aim(mc.player.blockPosition(), -1)) : PingAim.aim(mc);
            target.ifPresent(aim -> sendPing(phrase.autoPing(), aim.pos(), aim.entityId(), attach));
        }
    }

    /** A quick ping of the selected kind at this position. The dead cannot place pings. */
    public static void sendPing(PingType type, BlockPos pos) {
        sendPing(type, pos, -1, true);
    }

    /**
     * @param entityId runtime id of the aimed entity (-1 when the crosshair hit terrain):
     *                 the server may stick the ping onto it so it rides along.
     * @param attach when false the ping stays where it is put (a rally at the sender's feet);
     *               when true the server may snap it onto a vehicle hull or an entity under the crosshair.
     */
    public static void sendPing(PingType type, BlockPos pos, int entityId, boolean attach) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || type == null || pos == null) return;
        if (mc.player.isSpectator() || RespawnClientState.isDead()) {
            hint("ping.unrealzaruba.dead");
            return;
        }
        if (!PingClientState.canPingNow()) {
            hint("ping.unrealzaruba.cooldown");
            return;
        }
        PingClientState.markPing();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle().normalize();
        if (RadioDebug.DEBUG_FORCE) {
            com.dod.unrealzaruba.UnrealZaruba.LOGGER.info("[PingDebug] send ping type={} pos={} entity={}", type, pos.toShortString(), entityId);
        }
        PacketDistributor.sendToServer(new ServerboundPingPacket((byte) type.ordinal(), pos.immutable(), entityId, eye, look, attach));
    }

    public static void cycleType() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PingClientState.cycleSelected();
        PingType type = PingClientState.selected();
        mc.player.displayClientMessage(Component.translatable("ping.unrealzaruba.selected",
                Component.translatable(type.langKey)), true);
    }

    /** A teammate's radio phrase: a team-coloured chat line plus a placeholder blip. */
    public static void onRadio(UUID sender, String senderName, byte phraseId, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        RadioPhrase phrase = RadioPhrase.byId(phraseId);
        if (phrase == null) return;
        Style nameStyle = Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)).withBold(true);
        Component line = Component.translatable("radio.unrealzaruba.tag").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" " + senderName).setStyle(nameStyle))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(phrase.langKey).withStyle(ChatFormatting.WHITE));
        mc.player.sendSystemMessage(line);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SELECT1.get(), 1.0f));
    }

    /** A hint in the actionbar, not an error: the send was skipped locally. */
    private static void hint(String langKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.displayClientMessage(Component.translatable(langKey), true);
    }
}
