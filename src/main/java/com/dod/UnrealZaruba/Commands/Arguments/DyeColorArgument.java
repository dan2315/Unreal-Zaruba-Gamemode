package com.dod.UnrealZaruba.Commands.Arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.item.DyeColor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.FriendlyByteBuf;


public class DyeColorArgument implements ArgumentType<DyeColor> {
    private final DyeColor color;  // Store the dye color

    public DyeColorArgument(DyeColor color) {
        this.color = color;  // Constructor to set the dye color
    }

    // Getter method to retrieve the dye color
    public DyeColor getColor() {
        return color;
    }

    public static DyeColorArgument color()
    {
        return new DyeColorArgument(DyeColor.BLACK);
    }

    public static DyeColor getColor(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, DyeColor.class);
    }

    @Override
    public DyeColor parse(StringReader reader) throws CommandSyntaxException {
        String colorName = reader.readString();
        try {
            return DyeColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e);
        }
    }

    public static class Serializer implements ArgumentSerializer<DyeColorArgument> {
        @Override
        public void serializeToNetwork(DyeColorArgument argument, FriendlyByteBuf buffer) {
            buffer.writeInt(argument.getColor().ordinal());
        }

        @Override
        public DyeColorArgument deserializeFromNetwork(FriendlyByteBuf buffer) {
            int ordinal = buffer.readInt();
            return new DyeColorArgument(DyeColor.values()[ordinal]);
        }

        @Override
        public void serializeToJson(DyeColorArgument argument, JsonObject buffer) {

        }

        
    }

    static {
        ArgumentTypes.register("dye_color", DyeColorArgument.class, new Serializer());
    }

}