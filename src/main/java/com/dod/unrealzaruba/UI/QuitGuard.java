package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.UI.TabList.TeamRosterState;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Leaving the server from the pause menu in the middle of a match asks first: outside the team's own zone the server counts
 * leaving as a death and what the player carries is lost, so the exit is not a way to keep it. In the zone it does not ask.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class QuitGuard {
    private QuitGuard() {}

    @SubscribeEvent
    public static void onPauseInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pause)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isLocalServer() || !ScoreOverlay.INSTANCE.isShown()
                || TeamRosterState.teamOf(mc.player.getUUID()) == null) return;
        // In the team's own zone leaving costs nothing (the server checks the same zone when the player leaves)
        if (ZoneHintsOverlay.INSTANCE.inOwnZone()) return;

        for (var listener : event.getListenersList()) {
            if (!(listener instanceof Button button)) continue;
            String key = button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
            if (!key.equals("menu.disconnect") && !key.equals("menu.returnToMenu")) continue;

            Button guarded = Button.builder(button.getMessage(), b -> mc.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) button.onPress();
                else mc.setScreen(pause);
            }, Component.translatable("hud.unrealzaruba.quit.title"), Component.translatable("hud.unrealzaruba.quit.text"),
                    button.getMessage().copy().withStyle(net.minecraft.ChatFormatting.RED), CommonComponents.GUI_CANCEL)))
                    .bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();
            event.removeListener(button);
            event.addListener(guarded);
            return;
        }
    }
}
