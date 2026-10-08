package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Economy.EconomyConfig;
import com.dod.unrealzaruba.Economy.EconomyData;
import com.dod.unrealzaruba.Economy.SupplyDrops;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /supplydrop addspot | removespot | list | now. Spots are saved in the current map's economy.json; the crate is
 * placed where the player stands.
 */
public class SupplyDropCommand implements ICommandHandler {
    private static final int REMOVE_RANGE = 5;

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("supplydrop")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("addspot").executes(SupplyDropCommand::addSpot))
                .then(Commands.literal("removespot").executes(SupplyDropCommand::removeSpot))
                .then(Commands.literal("list").executes(SupplyDropCommand::list))
                .then(Commands.literal("now").executes(SupplyDropCommand::now)));
    }

    private static int addSpot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyData data = data(context);
        if (data == null) return 0;
        BlockPos pos = player.blockPosition();
        data.getData().supplyDrops.locations.add(pos);
        data.saveData();
        context.getSource().sendSuccess(() -> Component.literal("Added supply drop spot " + pos.toShortString()
                + " (" + data.getData().supplyDrops.locations.size() + " spots)"), true);
        return 1;
    }

    private static int removeSpot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyData data = data(context);
        if (data == null) return 0;
        var locations = data.getData().supplyDrops.locations;
        BlockPos here = player.blockPosition();
        BlockPos nearest = locations.stream()
                .filter(pos -> pos.distSqr(here) <= REMOVE_RANGE * REMOVE_RANGE)
                .min((a, b) -> Double.compare(a.distSqr(here), b.distSqr(here)))
                .orElse(null);
        if (nearest == null) {
            context.getSource().sendFailure(Component.literal("No supply drop spot within " + REMOVE_RANGE + " blocks"));
            return 0;
        }
        locations.remove(nearest);
        data.saveData();
        context.getSource().sendSuccess(() -> Component.literal("Removed supply drop spot " + nearest.toShortString()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        EconomyData data = data(context);
        if (data == null) return 0;
        var locations = data.getData().supplyDrops.locations;
        StringBuilder text = new StringBuilder("Supply drop spots (" + locations.size() + "):");
        locations.forEach(pos -> text.append("\n  ").append(pos.toShortString()));
        context.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
        return locations.size();
    }

    private static int now(CommandContext<CommandSourceStack> context) {
        EconomyConfig.SupplyDrops config = EconomyData.current().supplyDrops;
        if (!SupplyDrops.spawn(context.getSource().getServer(), config)) {
            context.getSource().sendFailure(Component.literal("No game map loaded or no supply drop spots"));
            return 0;
        }
        return 1;
    }

    private static EconomyData data(CommandContext<CommandSourceStack> context) {
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        EconomyData data = gamemode == null ? null : GamemodeDataManager.getHandler(gamemode.getClass(), EconomyData.class);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Start a match on the map first"));
        }
        return data;
    }

    @Override
    public String getCommandName() {
        return "supplydrop";
    }
}
