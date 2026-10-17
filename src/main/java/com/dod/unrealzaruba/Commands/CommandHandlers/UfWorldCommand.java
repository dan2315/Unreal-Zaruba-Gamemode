package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.WorldManager.WorldCommit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

/** /uf rules shows the backend schematic rules the mod holds; /uf world commit &lt;slot&gt; [comment...]: uploads the current map or lobby to the backend as a new world version. */
public class UfWorldCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("uf")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("rules").executes(context -> {
                    var rules = com.dod.unrealzaruba.Pricing.RulesService.get();
                    if (rules == null) {
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Schematic rules: not loaded yet (fetching in the background, run again in a moment)"), false);
                        return 0;
                    }
                    long age = (System.currentTimeMillis() - rules.fetchedAtMillis()) / 1000;
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Schematic rules " + rules.version()
                            + ": " + rules.catalog().size() + " price entries, max size " + rules.maxSize()[0] + "x" + rules.maxSize()[1] + "x" + rules.maxSize()[2]
                            + ", scanner " + rules.scanner() + ", checked " + age + " s ago"), false);
                    return 1;
                }))
                .then(Commands.literal("clearmobs").executes(context -> {
                    int cleared = com.dod.unrealzaruba.WorldManager.MapMobs.sweep(com.dod.unrealzaruba.WorldManager.WorldManager.gameLevel);
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Cleared " + cleared + " mobs from the game map"), false);
                    return cleared;
                }))
                .then(Commands.literal("leakcheck").executes(context -> {
                    int alive = com.dod.unrealzaruba.WorldManager.WorldManager.retiredLevelsAlive();
                    Runtime runtime = Runtime.getRuntime();
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Closed game levels still in memory: " + alive
                            + ", heap used " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + " MB of " + runtime.maxMemory() / (1024 * 1024) + " MB"), false);
                    return alive;
                }))
                .then(Commands.literal("world")
                        .then(Commands.literal("commit")
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(WorldCommit.SLOTS, builder))
                                        .executes(context -> WorldCommit.start(context.getSource(),
                                                StringArgumentType.getString(context, "slot"), ""))
                                        .then(Commands.argument("comment", StringArgumentType.greedyString())
                                                .executes(context -> WorldCommit.start(context.getSource(),
                                                        StringArgumentType.getString(context, "slot"),
                                                        StringArgumentType.getString(context, "comment"))))))));
    }

    @Override
    public String getCommandName() {
        return "uf";
    }
}
