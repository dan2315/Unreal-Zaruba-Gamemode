package com.dod.unrealzaruba.Commands.CommandHandlers;

import java.util.Collection;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Economy.Wallets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /money give|take|set &lt;players&gt; &lt;amount&gt;, /money get &lt;player&gt;. Balances are per match, like kill rewards.
 */
public class MoneyCommand implements ICommandHandler {
    private enum Op { GIVE, TAKE, SET }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("money")
                .requires(cs -> cs.hasPermission(2))
                .then(change("give", Op.GIVE))
                .then(change("take", Op.TAKE))
                .then(change("set", Op.SET))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int balance = Wallets.get(player);
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            player.getName().getString() + ": " + Money.format(balance)), false);
                                    return balance;
                                }))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> change(String name, Op op) {
        return Commands.literal(name)
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> apply(context, op))));
    }

    private static int apply(CommandContext<CommandSourceStack> context, Op op) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        for (ServerPlayer player : players) {
            switch (op) {
                case GIVE -> Wallets.add(player, amount);
                case TAKE -> Wallets.add(player, -amount);
                case SET -> Wallets.set(player, amount);
            }
        }
        String verb = switch (op) {
            case GIVE -> "Gave " + Money.format(amount) + " to ";
            case TAKE -> "Took " + Money.format(amount) + " from ";
            case SET -> "Set balance to " + Money.format(amount) + " for ";
        };
        String targets = players.size() == 1 ? players.iterator().next().getName().getString() : players.size() + " players";
        context.getSource().sendSuccess(() -> Component.literal(verb + targets), true);
        return players.size();
    }

    @Override
    public String getCommandName() {
        return "money";
    }
}
