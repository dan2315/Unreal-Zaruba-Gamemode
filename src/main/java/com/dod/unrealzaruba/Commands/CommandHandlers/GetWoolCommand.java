package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class GetWoolCommand implements ICommandHandler {
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

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("getwool")
                .requires(cs -> cs.hasPermission(3))
                .executes(this::giveColoredWool));
    }

    private int giveColoredWool(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("Нашёл тебя"));

        DyeColor[] colors = DyeColor.values(); // Get all wool colors.
        for (DyeColor color : colors) {
            player.sendSystemMessage(Component.literal("Даю цвет" + color));

            String woolName = player.getName().getString() + " " + color.getName();
            ItemStack itemStack = new ItemStack((itemMap.get(color)), 1);

            if (!itemStack.hasTag()) {
                itemStack.setTag(new CompoundTag());
            }
            CompoundTag nbtData = itemStack.getTag();
            nbtData.putString("Owner", player.getName().getString());

            itemStack.setTag(nbtData);

            itemStack.setHoverName(Component.literal(woolName));
            player.getInventory().add(itemStack);
        }
        source.sendSuccess(() -> Component.literal("Given all colored wool to " + player.getName().getString()),
                true);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "getwool";
    }
} 