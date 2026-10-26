package com.dod.unrealzaruba.Commands.CommandHandlers.Suggestions;

import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

/** The keys of the maps there are (folders in game_worlds/). */
public class MapSuggestions {
    public static final SuggestionProvider<CommandSourceStack> MAP_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(GameMaps.all().stream().map(GameMaps.GameMap::key), builder);
}
