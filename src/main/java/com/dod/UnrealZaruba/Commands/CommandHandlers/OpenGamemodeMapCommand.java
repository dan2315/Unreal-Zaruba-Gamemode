package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.GamemodeFactory;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static com.dod.UnrealZaruba.Commands.CommandHandlers.Suggestions.GamemodeSuggestions.GAMEMODE_SUGGESTIONS;

public class OpenGamemodeMapCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("openmap")
                .then(Commands.argument("gamemode", StringArgumentType.word())
                        .suggests(GAMEMODE_SUGGESTIONS).executes(context -> {
                            var gamemodeName = StringArgumentType.getString(context, "gamemode");
                            WorldManager.ReloadGameWorldDelayed(gamemodeName+"gamemode");
                            return 1;
                        })
                ));
    }

    @Override
    public String getCommandName() {
        return "openmap";
    }
}
