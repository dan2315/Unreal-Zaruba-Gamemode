package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static com.dod.unrealzaruba.Commands.CommandHandlers.Suggestions.GamemodeSuggestions.GAMEMODE_SUGGESTIONS;

public class VoteForGamemodeCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voteForGamemode")
                .then(Commands.argument("gamemodeName", StringArgumentType.word())
                        .suggests(GAMEMODE_SUGGESTIONS).executes(context -> {
                            var player = context.getSource().getPlayer();
                            String gamemodeName = StringArgumentType.getString(context, "gamemodeName").toLowerCase();

                            int result = GamemodeManager.instance.Vote(player.getUUID(), gamemodeName);

                            if (result == 0) {
                                player.sendSystemMessage(Component.literal("Сейчас не стадия голосования"));
                            }

                            player.sendSystemMessage(Component.literal("Вы успешно проголосовали за " + gamemodeName));
                            return 1;
                })));
    }

    @Override
    public String getCommandName() {
        return "voteForGamemode";
    }
}
