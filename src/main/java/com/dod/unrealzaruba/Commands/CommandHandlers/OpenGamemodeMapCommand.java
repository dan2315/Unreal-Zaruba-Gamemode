package com.dod.unrealzaruba.Commands.CommandHandlers;

import static com.dod.unrealzaruba.Commands.CommandHandlers.Suggestions.MapSuggestions.MAP_SUGGESTIONS;

import com.dod.unrealzaruba.Gamemodes.GamemodeFactory;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /openmap &lt;map&gt;: loads a map (in DEV mode its template itself, to edit it). With a mode set up (/setgamemode or a
 * match), that mode is made again for the map, so its settings commands write to this map. A mode id opens that
 * mode's first map.
 */
public class OpenGamemodeMapCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("openmap")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("map", StringArgumentType.word())
                        .suggests(MAP_SUGGESTIONS).executes(context -> {
                            String arg = StringArgumentType.getString(context, "map");
                            GameMaps.GameMap map = GameMaps.find(arg);
                            if (map == null) {
                                context.getSource().sendFailure(Component.literal("No map " + arg + ". Maps: "
                                        + String.join(", ", GameMaps.all().stream().map(GameMaps.GameMap::key).toList())));
                                return 0;
                            }
                            GameMaps.select(map.key());
                            if (GamemodeManager.instance.GetActiveGamemode() != null) {
                                GamemodeFactory.createGamemode(map.mode()); // its constructor loads the map
                            } else {
                                WorldManager.ReloadGameWorldDelayed(map.key());
                            }
                            context.getSource().sendSuccess(() -> Component.literal("Opening " + map.key() + " (" + map.mode() + ")"), true);
                            return 1;
                        })
                ));
    }

    @Override
    public String getCommandName() {
        return "openmap";
    }
}
