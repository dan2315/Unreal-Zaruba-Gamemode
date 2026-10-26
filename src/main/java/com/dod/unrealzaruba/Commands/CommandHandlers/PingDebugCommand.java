package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Radiopings.PingService;
import com.dod.unrealzaruba.Radiopings.RadioDebug;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Singleplayer testing bypass for radio and pings: {@code /pingdebug} forces the strips on anywhere
 * (no team, no match, any dimension) with delivery back to the sender, again turns it off;
 * {@code /pingdebug demo} places a ping riding a fake hull that orbits the player, so the whole
 * follow path can be watched with no ships around. Mirrors the {@code /compass} debug toggle.
 */
public class PingDebugCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pingdebug")
                .requires(source -> source.hasPermission(2))
                .executes(PingDebugCommand::toggle)
                .then(Commands.literal("demo").executes(PingDebugCommand::demo)));
    }

    private static int toggle(CommandContext<CommandSourceStack> context) {
        boolean on = RadioDebug.toggleDebug();
        context.getSource().sendSuccess(
                () -> Component.literal("Ping debug: " + (on ? "ON (radio and pings work anywhere, to yourself)" : "OFF")),
                false);
        return 1;
    }

    private static int demo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!RadioDebug.DEBUG_FORCE) {
            context.getSource().sendFailure(Component.literal("Turn /pingdebug on first"));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        PingService.placeDemo(player);
        context.getSource().sendSuccess(() -> Component.literal("Demo ping riding an orbit around you"), false);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "pingdebug";
    }
}
