package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Mobs.ClickableHumanoidEntity;
import com.dod.UnrealZaruba.Mobs.ModMobs;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class CreateNPCCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("createnpc")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("npc_id", IntegerArgumentType.integer())
                        .executes(context -> {
                            ServerPlayer sender = context.getSource()
                                    .getPlayerOrException();
                            Level world = sender.getCommandSenderWorld();
                            ClickableHumanoidEntity mob = new ClickableHumanoidEntity(
                                    ModMobs.CLICKABLE_HUMANOID_ENTITY.get(), world);
                            mob.Initialize(IntegerArgumentType.getInteger(context, "npc_id"));
                            mob.setPos(sender.position().x, sender.position().y,
                                    sender.position().z);
                            world.addFreshEntity(mob);

                            return 1;
                        })));
    }

    @Override
    public String getCommandName() {
        return "createnpc";
    }
} 