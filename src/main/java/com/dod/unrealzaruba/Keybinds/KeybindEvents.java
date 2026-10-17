package com.dod.unrealzaruba.Keybinds;

import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.brigadier.Command;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Applies the key binding preset when the game starts and offers /ufkeys to export, re-apply and force it. */
public final class KeybindEvents {
    private KeybindEvents() {}

    @EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                String report = KeybindManager.apply(false);
                UnrealZaruba.LOGGER.info("[Keybinds] {}", report);
            });
        }
    }

    @EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
    public static final class Joined {
        private static boolean done;

        /** Other mods have written their config files by the time the player joins, so settings that had to wait are set now. */
        @SubscribeEvent
        public static void onLoggingIn(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            if (done) return;
            done = true;
            UnrealZaruba.LOGGER.info("[Keybinds] on join: {}", KeybindManager.applyConfigsOnly());
        }
    }

    @EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
    public static final class Commands_ {
        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("ufkeys")
                    .then(Commands.literal("export").executes(context -> {
                        String report = KeybindManager.export();
                        context.getSource().sendSuccess(() -> Component.literal(report), false);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("apply").executes(context -> {
                        String report = KeybindManager.apply(false);
                        context.getSource().sendSuccess(() -> Component.literal(report), false);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("reset").executes(context -> {
                        // Everything of the preset, whatever the player changed
                        String report = KeybindManager.apply(true);
                        context.getSource().sendSuccess(() -> Component.literal(report), false);
                        return Command.SINGLE_SUCCESS;
                    })));
        }
    }
}
