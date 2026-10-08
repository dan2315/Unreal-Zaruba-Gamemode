package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.BuildKit.BuildKit;
import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DEV mode map building in the open game map (see {@link BuildKit}):
 * /buildkit list | check <name> | apply <name> [clear] | undo <name>
 */
public class BuildKitCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("buildkit")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.literal("list").executes(BuildKitCommand::list))
                .then(Commands.literal("check").then(name().executes(context -> apply(context, false, true))))
                .then(Commands.literal("apply").then(name()
                        .executes(context -> apply(context, false, false))
                        .then(Commands.literal("clear").executes(context -> apply(context, true, false)))))
                .then(Commands.literal("undo").then(name().executes(BuildKitCommand::undo))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> name() {
        return Commands.argument("name", StringArgumentType.word()).suggests((context, builder) -> {
            try {
                return SharedSuggestionProvider.suggest(BuildKit.list(), builder);
            } catch (Exception e) {
                return builder.buildFuture();
            }
        });
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        try {
            List<String> names = BuildKit.list();
            context.getSource().sendSuccess(() -> Component.literal(names.isEmpty() ? "No builds in unrealzaruba/builds" : String.join(", ", names)), false);
            return names.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(e.toString()));
            return 0;
        }
    }

    private static int apply(CommandContext<CommandSourceStack> context, boolean clear, boolean dryRun) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = gameLevel(source);
        String name = validName(source, StringArgumentType.getString(context, "name"));
        if (level == null || name == null) {
            return 0;
        }
        try {
            BuildKit.Result r = BuildKit.apply(level, name, clear, dryRun);
            String blockers = r.blockers().entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue()).limit(6)
                    .map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
            String text = (dryRun ? "Would place " : "Placed ") + r.placed() + ", already there " + r.already()
                    + ", skipped " + r.skipped() + (r.cleared() > 0 ? ", cleared " + r.cleared() : "")
                    + (r.clearsNotRun() > 0 ? ", " + r.clearsNotRun() + " clears not run (add 'clear')" : "")
                    + (blockers.isEmpty() ? "" : ". In the way: " + blockers)
                    + ". Report: " + r.report();
            source.sendSuccess(() -> Component.literal(text), true);
            return r.placed();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Build " + name + " failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int undo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = gameLevel(source);
        String name = validName(source, StringArgumentType.getString(context, "name"));
        if (level == null || name == null) {
            return 0;
        }
        try {
            int restored = BuildKit.undo(level, name);
            source.sendSuccess(() -> Component.literal("Undid " + name + ": restored " + restored + " blocks"), true);
            return restored;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Undo " + name + " failed: " + e.getMessage()));
            return 0;
        }
    }

    private static ServerLevel gameLevel(CommandSourceStack source) {
        if (!ConfigManager.isDevMode()) {
            source.sendFailure(Component.literal("BuildKit works in DEV mode only (matches don't save)"));
            return null;
        }
        if (WorldManager.gameLevel == null) {
            source.sendFailure(Component.literal("The game map is not loaded (/openmap)"));
            return null;
        }
        return WorldManager.gameLevel;
    }

    private static String validName(CommandSourceStack source, String name) {
        if (!BuildKit.NAME.matcher(name).matches()) {
            source.sendFailure(Component.literal("Name may only contain letters, digits, _ and -"));
            return null;
        }
        return name;
    }

    @Override
    public String getCommandName() {
        return "buildkit";
    }
}
