package com.dod.UnrealZaruba.Commands.CommandHandlers.Suggestions;

import com.dod.UnrealZaruba.Gamemodes.GamemodeFactory;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class GamemodeSuggestions {
    public static final SuggestionProvider<CommandSourceStack> GAMEMODE_SUGGESTIONS =
            (context, builder) -> suggestGamemodes(builder);

    public static CompletableFuture<Suggestions> suggestGamemodes(SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        for (String gamemodeName : GamemodeFactory.gamemodes.keySet()) {
            String lowercaseGamemodeName = gamemodeName.toLowerCase();
            if (lowercaseGamemodeName.startsWith(input)) {
                builder.suggest(lowercaseGamemodeName);
            }
        }

        return builder.buildFuture();
    }
}
