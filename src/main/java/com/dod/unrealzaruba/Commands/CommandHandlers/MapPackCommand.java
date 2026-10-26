package com.dod.unrealzaruba.Commands.CommandHandlers;

import java.util.List;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.WorldManager.MapPacks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Debugging the pre-rendered world maps (see {@link MapPacks}):
 * /mappack — which scan is loaded, which one the running mode uses, the scans there are;
 * /mappack load &lt;scan&gt; — everyone online gets that scan now (until the next mode starts);
 * /mappack bind &lt;scan&gt; — the running mode uses that scan from now on, and it is loaded.
 */
public class MapPackCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mappack")
                .requires(cs -> cs.hasPermission(2))
                .executes(MapPackCommand::status)
                .then(Commands.literal("load").then(scan().executes(context -> load(context, false))))
                .then(Commands.literal("bind").then(scan().executes(context -> load(context, true)))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> scan() {
        return Commands.argument("scan", StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(MapPacks.scans(), builder));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        var pack = MapPacks.current();
        String mode = MapPacks.currentMode();
        List<String> scans = MapPacks.scans();
        String loaded = pack == null
                ? "none" + (MapPacks.currentScan() == null ? "" : " (no scan " + MapPacks.currentScan() + ")")
                : pack.mapName() + ": " + pack.files().size() + " regions, " + pack.totalSize() / 1024 + " KB, hash " + pack.hash();
        String text = "Loaded: " + loaded
                + "\nMode: " + (mode == null ? "none started" : mode + " uses " + MapPacks.scanFor(mode))
                + "\nScans: " + (scans.isEmpty() ? "none" : String.join(", ", scans));
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return scans.size();
    }

    private static int load(CommandContext<CommandSourceStack> context, boolean bind) {
        CommandSourceStack source = context.getSource();
        String scan = StringArgumentType.getString(context, "scan");
        if (!MapPacks.scans().contains(scan)) {
            source.sendFailure(Component.literal("No scan " + scan + ". Scans: " + String.join(", ", MapPacks.scans())));
            return 0;
        }
        String mode = null;
        if (bind) {
            var gamemode = GamemodeManager.instance.GetActiveGamemode();
            if (gamemode == null) {
                source.sendFailure(Component.literal("No mode is running to bind the scan to"));
                return 0;
            }
            mode = gamemode.getClass().getSimpleName();
            MapPacks.bind(mode, scan);
        }
        var pack = MapPacks.load(source.getServer(), scan);
        if (pack == null) {
            source.sendFailure(Component.literal("Could not read scan " + scan + " (see the server log)"));
            return 0;
        }
        String text = "Loaded " + scan + " for " + source.getServer().getPlayerCount() + " players: " + pack.files().size()
                + " regions, " + pack.totalSize() / 1024 + " KB" + (mode == null ? "" : ". " + mode + " uses it from now on");
        source.sendSuccess(() -> Component.literal(text), true);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "mappack";
    }
}
