package com.dod.UnrealZaruba.Commands;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistration {
    private static final Map<DyeColor, Item> itemMap = new HashMap<>();

    static {
        initializeItemMap();
    }

    private static void initializeItemMap() {
        itemMap.put(DyeColor.WHITE, Items.WHITE_WOOL);
        itemMap.put(DyeColor.ORANGE, Items.ORANGE_WOOL);
        itemMap.put(DyeColor.MAGENTA, Items.MAGENTA_WOOL);
        itemMap.put(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL);
        itemMap.put(DyeColor.YELLOW, Items.YELLOW_WOOL);
        itemMap.put(DyeColor.LIME, Items.LIME_WOOL);
        itemMap.put(DyeColor.PINK, Items.PINK_WOOL);
        itemMap.put(DyeColor.GRAY, Items.GRAY_WOOL);
        itemMap.put(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL);
        itemMap.put(DyeColor.CYAN, Items.CYAN_WOOL);
        itemMap.put(DyeColor.PURPLE, Items.PURPLE_WOOL);
        itemMap.put(DyeColor.BLUE, Items.BLUE_WOOL);
        itemMap.put(DyeColor.BROWN, Items.BROWN_WOOL);
        itemMap.put(DyeColor.GREEN, Items.GREEN_WOOL);
        itemMap.put(DyeColor.RED, Items.RED_WOOL);
        itemMap.put(DyeColor.BLACK, Items.BLACK_WOOL);
    }

    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // АХАХХАХ, прикиньте сделать команду get_RPG, написать в чат не писать её, а
        // она будет тупо убивать

        dispatcher.register(Commands.literal("getwool")
                .requires(cs -> cs.hasPermission(0)).executes(context -> giveColoredWool(context)));

        dispatcher.register(Commands.literal("setprefix")
                .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("prefix", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    String prefix = StringArgumentType.getString(context, "prefix");

                                    ServerPlayer player = context.getSource().getServer().getPlayerList()
                                            .getPlayerByName(playerName);
                                    if (player != null) {
                                        Utils.SetPrefixTo(player, prefix);
                                        context.getSource().sendSuccess(
                                                new TextComponent("Set prefix for " + playerName + " to " + prefix),
                                                true);
                                    } else {
                                        context.getSource()
                                                .sendFailure(new TextComponent("Player " + playerName + " not found."));
                                    }

                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("startbattle")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> BaseGamemode.currentGamemode.StartBattle(context)));

        dispatcher.register(Commands.literal("crtobj")
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1", IntegerArgumentType.integer())
                                        .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                        .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                .then(Commands
                                                                        .argument("z2", IntegerArgumentType.integer())
                                                                        .executes(context -> {
                                                                            String name = StringArgumentType
                                                                                    .getString(context, "name");
                                                                            int x1 = IntegerArgumentType
                                                                                    .getInteger(context, "x1");
                                                                            int y1 = IntegerArgumentType
                                                                                    .getInteger(context, "y1");
                                                                            int z1 = IntegerArgumentType
                                                                                    .getInteger(context, "z1");
                                                                            int x2 = IntegerArgumentType
                                                                                    .getInteger(context, "x2");
                                                                            int y2 = IntegerArgumentType
                                                                                    .getInteger(context, "y2");
                                                                            int z2 = IntegerArgumentType
                                                                                    .getInteger(context, "z2");

                                                                            BlockVolume volume = new BlockVolume(
                                                                                    new BlockPos(x1, y1, z1),
                                                                                    new BlockPos(x2, y2, z2), true);
                                                                        

                                                                            DestructibleObjective objective = new DestructibleObjective(
                                                                                    volume, name);
                                                                            DestructibleObjectivesHandler
                                                                                    .Add(objective);

                                                                            context.getSource()
                                                                                    .sendSuccess(new TextComponent(
                                                                                            "Created objective: "
                                                                                                    + objective),
                                                                                            true);
                                                                            return 1;
                                                                        })))))))));

        // Pray to Dod this code works
        // Бермудский треугольник, не раскоменчивать
        // Ошибка происходит после .requires(cs -> cs.hasPermission(3))
        // dispatcher.register(Commands.literal("setteamspawn")
        // .requires(cs -> cs.hasPermission(3))
        // .then(Commands.argument("team_color", TeamColorArgument.color())
        // .executes(CommandRegistration::SetTeamSpawn)
        // .then(Commands.argument("x", IntegerArgumentType.integer())
        // .then(Commands.argument("y", IntegerArgumentType.integer())
        // .then(Commands.argument("z", IntegerArgumentType.integer())
        // .executes(CommandRegistration::SetTeamSpawnTo))))));

    }

    private static int SetTeamSpawnTo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        BlockPos position = new BlockPos(x, y, z);
        TeamColor color = TeamColorArgument.getColor(context, TeamColorArgument.PropertyName);

        BaseGamemode.currentGamemode.TeamManager.SetSpawn(color, position);
        context.getSource().sendSuccess(
                new TextComponent("Спавн команды " + color.toString().toUpperCase() + " поставлен в " + position),
                true);
        return 0;
    }

    private static int SetTeamSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos position = new BlockPos(player.position());
        TeamColor color = TeamColorArgument.getColor(context, TeamColorArgument.PropertyName);

        BaseGamemode.currentGamemode.TeamManager.SetSpawn(color, position);
        context.getSource().sendSuccess(
                new TextComponent("Спавн команды " + color.toString().toUpperCase() + " поставлен в " + position),
                true);
        return 0;
    }

    private static int giveColoredWool(CommandContext<CommandSourceStack> context) {

        CommandSourceStack source = context.getSource();
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(new TextComponent("This command can only be run by a player."));
            return 0;
        }

        player.sendMessage(new TextComponent("Нашёл тебя"), player.getUUID());

        DyeColor[] colors = DyeColor.values(); // Get all wool colors.
        for (DyeColor color : colors) {
            player.sendMessage(new TextComponent("Даю цвет" + color), player.getUUID());

            String woolName = player.getName().getString() + " " + color.getName();
            ItemStack itemStack = new ItemStack((itemMap.get(color)), 1);

            if (!itemStack.hasTag()) {
                itemStack.setTag(new CompoundTag());
            }
            CompoundTag nbtData = itemStack.getTag();
            nbtData.putString("Owner", player.getName().getString());

            itemStack.setTag(nbtData);

            itemStack.setHoverName(new TextComponent(woolName));
            player.getInventory().add(itemStack);
        }
        source.sendSuccess(new TextComponent("Given all colored wool to " + player.getName().getString()), true);
        return 1;
    }

}
