package com.dod.unrealzaruba.Commands.CommandHandlers;

import static com.dod.unrealzaruba.Commands.CommandHandlers.Suggestions.MapSuggestions.MAP_SUGGESTIONS;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /forcestart [map]: ends the vote now; with a map, the match is on that map whatever the votes say. */
public class ForceStartGameCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("forcestart")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GamemodeManager.instance.StartGame();
                    return 1;
                })
                .then(Commands.argument("map", StringArgumentType.word())
                        .suggests(MAP_SUGGESTIONS)
                        .executes(context -> {
                            String arg = StringArgumentType.getString(context, "map");
                            GameMaps.GameMap map = GameMaps.find(arg);
                            if (map == null) {
                                context.getSource().sendFailure(Component.literal("No map " + arg));
                                return 0;
                            }
                            GamemodeManager.instance.StartGame(map);
                            return 1;
                        })));
    }

    @Override
    public String getCommandName() {
        return "forcestart";
    }
}
