package com.dod.unrealzaruba.Commands.CommandHandlers;

import static com.dod.unrealzaruba.Commands.CommandHandlers.Suggestions.MapSuggestions.MAP_SUGGESTIONS;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeFactory;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Maps for the modes (see {@link GameMaps}):
 * /newmap &lt;key&gt; &lt;mode&gt; [from] — a new map: a copy of another one (its world, and its settings when it is of the
 * same mode) or an empty world; open it with /openmap to build it;
 * /mapinfo — the maps there are; /mapinfo &lt;map&gt; name|description &lt;text&gt; — what the lobby shows for it.
 */
public class NewMapCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("newmap")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("key", StringArgumentType.word())
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(GamemodeFactory.classes.keySet(), builder))
                                .executes(context -> create(context, null))
                                .then(Commands.argument("from", StringArgumentType.word())
                                        .suggests(MAP_SUGGESTIONS)
                                        .executes(context -> create(context, StringArgumentType.getString(context, "from")))))));

        dispatcher.register(Commands.literal("mapinfo")
                .requires(cs -> cs.hasPermission(3))
                .executes(NewMapCommand::list)
                .then(Commands.argument("map", StringArgumentType.word())
                        .suggests(MAP_SUGGESTIONS)
                        .then(Commands.literal("name").then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> describe(context, true))))
                        .then(Commands.literal("description").then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> describe(context, false))))));
    }

    private static int create(CommandContext<CommandSourceStack> context, String fromArg) {
        CommandSourceStack source = context.getSource();
        String key = StringArgumentType.getString(context, "key").toLowerCase();
        String mode = StringArgumentType.getString(context, "mode").toLowerCase();
        if (!GameMaps.KEY.matcher(key).matches()) {
            source.sendFailure(Component.literal("A map key is up to 32 lowercase letters, digits, _ and -"));
            return 0;
        }
        if (Files.exists(GameMaps.TEMPLATES.resolve(key)) || Files.exists(GameMaps.MAPS.resolve(key))) {
            source.sendFailure(Component.literal("Map " + key + " already exists"));
            return 0;
        }
        if (!GamemodeFactory.classes.containsKey(mode)) {
            source.sendFailure(Component.literal("No mode " + mode + ". Modes: " + String.join(", ", GamemodeFactory.classes.keySet())));
            return 0;
        }
        GameMaps.GameMap from = fromArg == null ? null : GameMaps.find(fromArg);
        if (fromArg != null && from == null) {
            source.sendFailure(Component.literal("No map " + fromArg + " to copy"));
            return 0;
        }
        if (from != null && WorldManager.isCopyingTemplate(from.key())) {
            source.sendFailure(Component.literal(from.key() + " is being copied for a match right now, try again in a moment"));
            return 0;
        }
        // The map being edited in DEV mode is the template itself: what is built in it must be on disk before the copy
        if (from != null && ConfigManager.isDevMode() && from.key().equals(WorldManager.currentMapKey()) && WorldManager.gameLevel != null) {
            WorldManager.gameLevel.save(null, true, false);
        }
        source.sendSuccess(() -> Component.literal("Making map " + key + (from == null ? " (empty)" : " from " + from.key()) + "..."), true);
        var server = source.getServer();
        CompletableFuture.supplyAsync(() -> {
            try {
                return GameMaps.create(key, mode, from);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((map, error) -> server.execute(() -> {
            if (error != null || map == null) {
                UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not make map {}", key, error);
                source.sendFailure(Component.literal("Could not make map " + key + " (see the server log)"));
                return;
            }
            source.sendSuccess(() -> Component.literal("Map " + key + " (" + mode + ") is ready: /openmap " + key
                    + ". Its settings are in " + map.dataDir() + "; name it with /mapinfo " + key + " name <text>"), true);
        }));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        var maps = GameMaps.all();
        StringBuilder text = new StringBuilder(maps.isEmpty() ? "No maps in game_worlds/" : "Maps:");
        for (var map : maps) {
            text.append("\n").append(map.key()).append(" — ").append(map.mode())
                    .append(map.name().isEmpty() ? "" : ", \"" + map.name() + "\"")
                    .append(", settings in ").append(map.dataDir());
        }
        context.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
        return maps.size();
    }

    private static int describe(CommandContext<CommandSourceStack> context, boolean name) {
        CommandSourceStack source = context.getSource();
        String arg = StringArgumentType.getString(context, "map");
        GameMaps.GameMap map = GameMaps.get(arg.toLowerCase());
        if (map == null) {
            source.sendFailure(Component.literal("No map " + arg));
            return 0;
        }
        String text = StringArgumentType.getString(context, "text").trim();
        GameMaps.Info info = new GameMaps.Info();
        info.mode = map.mode();
        info.name = name ? text : map.name();
        info.description = name ? map.description() : text;
        try {
            GameMaps.writeInfo(map.key(), info);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Could not save the map's map.json: " + e.getMessage()));
            return 0;
        }
        com.dod.unrealzaruba.Lobby.LobbyService.broadcast();
        source.sendSuccess(() -> Component.literal(map.key() + (name ? " is now called \"" : " description: \"") + text + "\""), true);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "newmap";
    }
}
