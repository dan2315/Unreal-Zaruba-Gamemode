package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.UI.Compass.CompassOverlay;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Debug toggle for the compass strip: {@code /compass} forces it on anywhere
 * (any dimension, no objectives) with demo markers, again turns it off.
 * Singleplayer-only workflow; on singleplayer the integrated server and the
 * client share the JVM, so the client overlay sees the flag immediately.
 */
public class CompassDebugCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("compass")
                .executes(context -> {
                    boolean on = CompassOverlay.toggleDebug();
                    context.getSource().sendSuccess(
                            () -> Component.literal("Compass debug: " + (on ? "ON (demo markers)" : "OFF")),
                            false);
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "compass";
    }
}
