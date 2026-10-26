package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.WorldManager.MapPacks;
import com.dod.unrealzaruba.WorldManager.MapScan;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /mapscan &lt;name&gt; [seconds per stop]: flies you over the loaded map so Xaero's World Map renders all of it, then
 * saves the result as the scan of that name, which becomes the open map's pre-rendered world map for every player.
 */
public class MapScanCommand implements ICommandHandler {
    private static final int DEFAULT_WAIT_SECONDS = 6;

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mapscan")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> run(context, DEFAULT_WAIT_SECONDS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(context -> run(context, IntegerArgumentType.getInteger(context, "seconds"))))));
    }

    private static int run(CommandContext<CommandSourceStack> context, int seconds) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        if (!MapPacks.NAME.matcher(name).matches()) {
            context.getSource().sendFailure(Component.literal("A scan name is up to 32 letters, digits, _ and -"));
            return 0;
        }
        String mapKey = com.dod.unrealzaruba.WorldManager.WorldManager.currentMapKey();
        if (mapKey == null) {
            context.getSource().sendFailure(Component.literal("Open the map first (/openmap or a match), the scan needs its world loaded"));
            return 0;
        }
        Component result = MapScan.start(player, name, mapKey, seconds);
        context.getSource().sendSuccess(() -> result, false);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "mapscan";
    }
}
