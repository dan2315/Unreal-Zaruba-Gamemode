package com.dod.UnrealZaruba.Commands.Arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.FriendlyByteBuf;


public class TeamColorArgument implements ArgumentType<TeamColor> {
    private final TeamColor color;  

    public TeamColorArgument(TeamColor color) {
        this.color = color; 
    }

    public TeamColor getColor() {
        return color;
    }

    public static TeamColorArgument color()
    {
        return new TeamColorArgument(TeamColor.Undefined);
    }

    public static TeamColor getColor(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, TeamColor.class);
    }

    @Override
    public TeamColor parse(StringReader reader) throws CommandSyntaxException {
        String colorName = reader.readString();
        try {
            return TeamColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e);
        }
    }

    public static class Serializer implements ArgumentSerializer<TeamColorArgument> {
        @Override
        public void serializeToNetwork(TeamColorArgument argument, FriendlyByteBuf buffer) {
            buffer.writeInt(argument.getColor().ordinal());
        }

        @Override
        public TeamColorArgument deserializeFromNetwork(FriendlyByteBuf buffer) {
            int ordinal = buffer.readInt();
            return new TeamColorArgument(TeamColor.values()[ordinal]);
        }

        @Override
        public void serializeToJson(TeamColorArgument argument, JsonObject buffer) {
            buffer.addProperty("team_color", argument.getColor().name());
        }

        
    }

    public static void RegisterArgument() {
        ArgumentTypes.register("team_color", TeamColorArgument.class, new Serializer());
    }
}