package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeFactory;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

import static com.dod.UnrealZaruba.Commands.CommandHandlers.Suggestions.GamemodeSuggestions.GAMEMODE_SUGGESTIONS;

public class SetGamemodeCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setgamemode")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("gamemodeName", StringArgumentType.word())
                        .suggests(GAMEMODE_SUGGESTIONS)
                        .executes(context -> {
                            // Always use lowercase for gamemode names to comply with resource location standards
                            String gamemodeName = StringArgumentType.getString(context, "gamemodeName").toLowerCase();
                            
                            if (!GamemodeFactory.gamemodes.containsKey(gamemodeName)) {
                                context.getSource().sendFailure(Component.literal("Unknown gamemode: " + gamemodeName + 
                                                                ". Available gamemodes: " + String.join(", ", GamemodeFactory.gamemodes.keySet())));
                                return 0;
                            }
                            
                            // Create the gamemode using the factory
                            BaseGamemode gamemode = GamemodeFactory.createGamemode(gamemodeName);
                            
                            // Set it as active using GamemodeManager
                            GamemodeManager.instance.SetActiveGamemode(gamemode);
                            
                            UnrealZaruba.LOGGER.info("[UnrealZaruba] Setting active gamemode to " + gamemodeName);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Active gamemode set to " + gamemodeName),
                                    true);
                            
                            return 1;
                        })));
    }

    @Override
    public String getCommandName() {
        return "setgamemode";
    }
} 