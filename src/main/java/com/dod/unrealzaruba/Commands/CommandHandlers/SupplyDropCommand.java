package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Economy.EconomyConfig;
import com.dod.unrealzaruba.Economy.EconomyData;
import com.dod.unrealzaruba.Economy.SupplyParadrop;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * /supplydrop now [ground x y z] | area pos1 | pos2 | floor | show | auto on | off. Supply drops come down by parachute
 * during the battle at a random spot of the area; {@code now} sends one right away. The settings are saved in the
 * current map's economy.json.
 */
public class SupplyDropCommand implements ICommandHandler {
    private static final RandomSource RANDOM = RandomSource.create();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("supplydrop")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("now")
                        .executes(context -> now(context, null))
                        .then(Commands.argument("ground", BlockPosArgument.blockPos())
                                .executes(context -> now(context, BlockPosArgument.getBlockPos(context, "ground")))))
                // Where automatic drops may land: stand in a corner of the area and run pos1, then in the opposite
                // corner for pos2, then stand on the ground and run floor
                .then(Commands.literal("area")
                        .then(Commands.literal("pos1").executes(context -> setCorner(context, 1)))
                        .then(Commands.literal("pos2").executes(context -> setCorner(context, 2)))
                        .then(Commands.literal("floor").executes(SupplyDropCommand::setFloor))
                        .then(Commands.literal("show").executes(SupplyDropCommand::showArea)))
                .then(Commands.literal("auto")
                        .then(Commands.literal("on").executes(context -> setAuto(context, true)))
                        .then(Commands.literal("off").executes(context -> setAuto(context, false)))));
    }

    /** Sends a drop now: at the given ground position, else a random spot of the area, else above the player. */
    private static int now(CommandContext<CommandSourceStack> context, BlockPos given) {
        EconomyConfig.SupplyDrops config = EconomyData.current().supplyDrops;
        BlockPos target;
        if (given != null) {
            target = given;
        } else if (config.paradrop.area.isValid()) {
            target = config.paradrop.area.randomSpot(RANDOM);
        } else {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("The drop area is not set (/supplydrop area pos1, pos2, floor): give the ground position, /supplydrop now <x y z>"));
                return 0;
            }
            target = player.blockPosition();
        }
        if (!SupplyParadrop.spawn(context.getSource().getServer(), config, target)) {
            context.getSource().sendFailure(Component.literal("No game map loaded, or the sky above the spot is not free"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Supply drop sent to " + target.getX() + ", " + target.getZ()), true);
        return 1;
    }

    private static int setCorner(CommandContext<CommandSourceStack> context, int corner) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyData data = data(context);
        if (data == null) return 0;
        EconomyConfig.Area area = data.getData().supplyDrops.paradrop.area;
        BlockPos pos = player.blockPosition();
        if (corner == 1) {
            area.x1 = pos.getX();
            area.z1 = pos.getZ();
        } else {
            area.x2 = pos.getX();
            area.z2 = pos.getZ();
        }
        data.saveData();
        context.getSource().sendSuccess(() -> Component.literal("Supply drop area corner " + corner + " set to x=" + pos.getX() + " z=" + pos.getZ()
                + (area.isValid() ? ". " + describe(area) : ". Set the other corner too.")), true);
        return 1;
    }

    /** The block you stand on is the floor: drops start high above it and settle on whatever ground is below. */
    private static int setFloor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyData data = data(context);
        if (data == null) return 0;
        EconomyConfig.Area area = data.getData().supplyDrops.paradrop.area;
        area.floorY = player.blockPosition().getY() - 1;
        data.saveData();
        context.getSource().sendSuccess(() -> Component.literal("Supply drop floor y set to " + area.floorY), true);
        return 1;
    }

    private static int showArea(CommandContext<CommandSourceStack> context) {
        EconomyData data = data(context);
        if (data == null) return 0;
        EconomyConfig.Paradrop paradrop = data.getData().supplyDrops.paradrop;
        context.getSource().sendSuccess(() -> Component.literal("Automatic supply drops are " + (paradrop.auto ? "ON" : "OFF")
                + ": first after " + paradrop.firstDropSeconds + " s, then every " + paradrop.intervalSeconds + " s, at most "
                + paradrop.maxActive + " at once. " + (paradrop.area.isValid() ? describe(paradrop.area) : "The area is not set (use /supplydrop area pos1 and pos2).")), false);
        return 1;
    }

    private static int setAuto(CommandContext<CommandSourceStack> context, boolean on) {
        EconomyData data = data(context);
        if (data == null) return 0;
        EconomyConfig.Paradrop paradrop = data.getData().supplyDrops.paradrop;
        paradrop.auto = on;
        data.saveData();
        context.getSource().sendSuccess(() -> Component.literal("Automatic supply drops " + (on ? "ON" : "OFF")
                + (on && !paradrop.area.isValid() ? ", but the area is not set yet" : "")), true);
        return 1;
    }

    private static String describe(EconomyConfig.Area area) {
        return "Area x " + Math.min(area.x1, area.x2) + ".." + Math.max(area.x1, area.x2) + ", z " + Math.min(area.z1, area.z2) + ".."
                + Math.max(area.z1, area.z2) + ", floor y " + area.floorY;
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
