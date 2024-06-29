package com.dod.UnrealZaruba.Commands;

import com.dod.UnrealZaruba.Commands.Arguments.DyeColorArgument;
import com.dod.UnrealZaruba.Gamemodes.CaptureObjectivesMode;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class CommandRegistration {

    private static final Map<DyeColor, Item> itemMap = new HashMap<>();

    static {
        initializeItemMap();
    }

    private static void initializeItemMap() {
        // Adding entries for all wool colors
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

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("getwool")
                .requires(cs -> cs.hasPermission(0)).executes(context -> giveColoredWool(context)));

        dispatcher.register(Commands.literal("startgame")
                .requires(cs -> cs.hasPermission(3)).executes(context -> CaptureObjectivesMode.StartGame(context)));

        dispatcher.register(Commands.literal("startpreparation")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> CaptureObjectivesMode.StartPreparation(context)));

        dispatcher.register(Commands.literal("setteamspawn")
                .then(Commands.argument("color", DyeColorArgument.color())
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(CommandRegistration::SetTeamSpawnTo))))));

        dispatcher.register(Commands.literal("setteamspawn")
                .then(Commands.argument("color", DyeColorArgument.color())
                        .executes(CommandRegistration::SetTeamSpawn)));

        // dispatcher.register(Commands.literal("giveteamkits")
        // .requires(cs -> cs.hasPermission(3))
        // .executes(context ->
        // TeamManager.GiveKitToAll(context.getSource().getServer())));

    }

    private static int SetTeamSpawnTo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        BlockPos position = new BlockPos(x, y, z);
        DyeColor color = DyeColorArgument.getColor(context, "color");

        TeamManager.SetSpawn(color, position);
        context.getSource().sendSuccess(
                new TextComponent("Spawn of team " + color.toString().toUpperCase() + " was set to " + position),
                true);
        return 0;
    }

    private static int SetTeamSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos position = new BlockPos(context.getSource().getPlayerOrException().position());
        DyeColor color = DyeColorArgument.getColor(context, "color");

        TeamManager.SetSpawn(color, position);
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

        if (!player.isCreative()) {
            source.sendFailure(new TextComponent("This command can only be run by a player."));
            return 0;
        }

        player.sendMessage(new TextComponent("Прошёл проверку на креатив"), player.getUUID());

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
