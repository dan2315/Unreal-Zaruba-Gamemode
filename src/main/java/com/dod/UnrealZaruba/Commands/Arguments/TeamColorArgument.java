package com.dod.UnrealZaruba.Commands.Arguments;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;


public class TeamColorArgument implements ArgumentType<TeamColor> {
    public static final String PropertyName = "team_color";
    private final TeamColor color;  

    public TeamColorArgument(TeamColor color) {
        this.color = color; 
    }

    public TeamColor getColor() {
        return color;
    }

    public static TeamColorArgument color()
    {
        return new TeamColorArgument(TeamColor.UNDEFINED);
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

    public static class Info implements ArgumentTypeInfo<TeamColorArgument, Info.Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
            buffer.writeEnum(template.color);
        }

        @Override
        public @NotNull Template deserializeFromNetwork(@Nonnull FriendlyByteBuf buffer) {
            return new Template(buffer.readEnum(TeamColor.class));
        }

        @Override
        public void serializeToJson(Template template, JsonObject jsonObject) {
            jsonObject.addProperty(PropertyName, template.color.name());
        }

        @Override
        public @NotNull Template unpack(TeamColorArgument argument) {
            return new Template(argument.getColor());
        }


        public static class Template implements ArgumentTypeInfo.Template<TeamColorArgument> {
            private final TeamColor color;

            public Template(TeamColor color) {
                this.color = color;
            }

            @Override
            public TeamColorArgument instantiate(CommandBuildContext commandBuildContext) {
                return new TeamColorArgument(color);
            }

            @Override
            public ArgumentTypeInfo<TeamColorArgument, ?> type() {
                return new Info();
            }
        }
    }

    public static void RegisterArgument() {
        ArgumentTypeInfos.registerByClass(TeamColorArgument.class, new Info());
    }
}