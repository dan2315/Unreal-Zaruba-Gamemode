package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.WorldManager.MapScan;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /mapscan [seconds per stop]: flies you over the loaded map so Xaero's World Map renders all of it, then uploads the
 * result as the map's pre-rendered world map for every player.
 */
public class MapScanCommand implements ICommandHandler {
    private static final int DEFAULT_WAIT_SECONDS = 6;

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mapscan")
                .requires(cs -> cs.hasPermission(2))
                .executes(context -> run(context, DEFAULT_WAIT_SECONDS))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                        .executes(context -> run(context, IntegerArgumentType.getInteger(context, "seconds")))));
    }

    private static int run(CommandContext<CommandSourceStack> context, int seconds) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var gamemode = GamemodeManager.instance.GetActiveGamemode();
        if (gamemode == null) {
            context.getSource().sendFailure(Component.literal("Start a match on the map first, the scan needs its world loaded"));
            return 0;
        }
        Component result = MapScan.start(player, gamemode.getClass().getSimpleName(), seconds);
        context.getSource().sendSuccess(() -> result, false);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "mapscan";
    }
}
