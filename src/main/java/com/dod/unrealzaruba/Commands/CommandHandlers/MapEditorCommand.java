package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.ModItems.ModItems;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** /mapeditor: gives you the map editor (capture points, targets to destroy, team bases of the open map). */
public class MapEditorCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mapeditor")
                .requires(cs -> cs.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!player.getInventory().add(new ItemStack(ModItems.MAP_EDITOR.get()))) {
                        context.getSource().sendFailure(Component.literal("No room in your inventory"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.translatable("editor.unrealzaruba.given"), false);
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "mapeditor";
    }
}
