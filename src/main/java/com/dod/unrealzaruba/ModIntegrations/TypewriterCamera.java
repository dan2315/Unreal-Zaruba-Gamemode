package com.dod.unrealzaruba.ModIntegrations;

import java.lang.reflect.Method;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.glfw.GLFW;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Sitting down at a Drive By Wire Typewriter Hub switches the camera to Sable's sub-level third person (what a double
 * F5 gives on a contraption) and leaving it goes back to first person. Everything from the other mods is reached by
 * name, so the mod loads without them.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class TypewriterCamera {
    private static final ResourceLocation HUB = ResourceLocation.parse("drivebywiretypewriter:typewriter_hub");
    private static final String PLAYER_EXTENSION = "dev.simulated_team.simulated.mixin_interface.PlayerTypewriterExtension";

    private static boolean lookedUp;
    private static Method currentTypewriter;
    private static CameraType sableView;
    private static boolean wasAtHub;
    /** Escape was pressed at the hub: Simulated only drops the connection later, so the camera must not go back to it. */
    private static boolean leftWithEscape;

    private TypewriterCamera() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean atHub = player != null && isAtHub(minecraft, player);
        if (leftWithEscape) {
            if (atHub) return; // still reported as connected, wait until it lets go
            leftWithEscape = false;
            wasAtHub = false;
            return;
        }
        if (atHub == wasAtHub) return;
        wasAtHub = atHub;
        if (atHub) {
            if (sableView != null) minecraft.options.setCameraType(sableView);
        } else if (minecraft.options.getCameraType() == sableView) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_ESCAPE || event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!wasAtHub || minecraft.screen != null) return;
        leftWithEscape = true;
        wasAtHub = false;
        if (minecraft.options.getCameraType() == sableView) minecraft.options.setCameraType(CameraType.FIRST_PERSON);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        wasAtHub = false;
        leftWithEscape = false;
    }

    private static boolean isAtHub(Minecraft minecraft, LocalPlayer player) {
        lookUp(player);
        if (currentTypewriter == null || minecraft.level == null) return false;
        try {
            if (!(currentTypewriter.invoke(player) instanceof BlockPos pos)) return false;
            BlockEntity block = minecraft.level.getBlockEntity(pos);
            return block != null && HUB.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(block.getType()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static void lookUp(LocalPlayer player) {
        if (lookedUp) return;
        lookedUp = true;
        try {
            currentTypewriter = Class.forName(PLAYER_EXTENSION, false, player.getClass().getClassLoader())
                    .getMethod("simulated$getCurrentTypewriter");
            for (CameraType type : CameraType.values()) {
                if (type.name().equals("SUB_LEVEL_VIEW")) sableView = type;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            UnrealZaruba.LOGGER.info("[TypewriterCamera] Simulated or Sable is not there, hub camera is off: {}", e.toString());
        }
    }
}
