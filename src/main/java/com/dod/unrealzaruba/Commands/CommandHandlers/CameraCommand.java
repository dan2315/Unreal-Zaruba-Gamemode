package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.Camera.CameraShots;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * /camera register|unregister <player>: mark a player as the screenshot camera (kept out of matches, spectator).
 * /camera shot <pos> <yaw> <pitch> <name> [fov] and /camera look <from> <target> <name> [fov]: move the camera in the
 * game map and save screenshots/claude/<name>.png on the camera client. /camera status: the last shot's result.
 */
public class CameraCommand implements ICommandHandler {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("camera")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.literal("register")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setCamera(context, true))))
                .then(Commands.literal("unregister")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setCamera(context, false))))
                .then(Commands.literal("status")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal(CameraShots.lastResult()), false);
                            return 1;
                        }))
                .then(Commands.literal("shot")
                        .then(Commands.argument("pos", Vec3Argument.vec3(false))
                                .then(Commands.argument("yaw", FloatArgumentType.floatArg(-360, 360))
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                                .then(withFov(Commands.argument("name", StringArgumentType.word()), false))))))
                .then(Commands.literal("look")
                        .then(Commands.argument("from", Vec3Argument.vec3(false))
                                .then(Commands.argument("target", Vec3Argument.vec3(false))
                                        .then(withFov(Commands.argument("name", StringArgumentType.word()), true))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> withFov(RequiredArgumentBuilder<CommandSourceStack, String> name, boolean look) {
        return name.executes(context -> shoot(context, look, 0))
                .then(Commands.argument("fov", FloatArgumentType.floatArg(10, 140))
                        .executes(context -> shoot(context, look, FloatArgumentType.getFloat(context, "fov"))));
    }

    private static int setCamera(CommandContext<CommandSourceStack> context, boolean camera) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        if (camera) {
            player.addTag(CameraShots.TAG);
        } else {
            player.removeTag(CameraShots.TAG);
        }
        context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + (camera ? " is now the camera" : " is no longer the camera")), true);
        return 1;
    }

    private static int shoot(CommandContext<CommandSourceStack> context, boolean look, float fov) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        if (!CameraShots.NAME.matcher(name).matches()) {
            source.sendFailure(Component.literal("Name may only contain letters, digits, _ and -"));
            return 0;
        }
        ServerPlayer camera = CameraShots.findCamera(source.getServer());
        if (camera == null) {
            source.sendFailure(Component.literal("No camera online. Join with the camera client and run /camera register <player>"));
            return 0;
        }
        ServerLevel level = WorldManager.gameLevel;
        if (level == null) {
            source.sendFailure(Component.literal("The game map is not loaded (/openmap or start a match)"));
            return 0;
        }
        float yaw, pitch;
        Vec3 pos;
        if (look) {
            pos = Vec3Argument.getVec3(context, "from");
            Vec3 target = Vec3Argument.getVec3(context, "target");
            float[] rotation = CameraShots.lookAt(pos.x, pos.y + camera.getEyeHeight(), pos.z, target.x, target.y, target.z);
            yaw = rotation[0];
            pitch = rotation[1];
        } else {
            pos = Vec3Argument.getVec3(context, "pos");
            yaw = FloatArgumentType.getFloat(context, "yaw");
            pitch = FloatArgumentType.getFloat(context, "pitch");
        }
        CameraShots.shoot(camera, level, pos.x, pos.y, pos.z, yaw, pitch, fov, name);
        source.sendSuccess(() -> Component.literal(String.format("Camera %s at %.1f %.1f %.1f yaw %.1f pitch %.1f -> %s.png",
                camera.getScoreboardName(), pos.x, pos.y, pos.z, yaw, pitch, name)), false);
        return 1;
    }

    @Override
    public String getCommandName() {
        return "camera";
    }
}
