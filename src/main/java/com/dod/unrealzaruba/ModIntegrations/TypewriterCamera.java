package com.dod.unrealzaruba.ModIntegrations;

import java.lang.reflect.Method;
import java.util.UUID;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Sitting down at a Drive By Wire Typewriter Hub switches the camera to Sable's sub-level third person (what a double
 * F5 gives on a contraption) and leaving it goes back to first person. Everything from the other mods is reached by
 * name, so the mod loads without them.
 *
 * <p>The player is at the hub while Simulated's typewriter input is active for it on this client: every way of leaving
 * (Escape, a screen, walking away, clicking it again) turns that off at once, while the server lets go a moment later.
 *
 * <p>It also mends a desync of Simulated: when the server disconnects a user it sends the block to the clients before it
 * forgets the user, so this client keeps believing it is still the user. The next click then counts as standing up here
 * and as sitting down on the server, and it takes a second click (and the two stay out of step). A hub that names this
 * player while this client is at no typewriter is disconnected again here.
 */
@EventBusSubscriber(modid = UnrealZaruba.MOD_ID, value = Dist.CLIENT)
public final class TypewriterCamera {
    private static final ResourceLocation HUB = ResourceLocation.parse("drivebywiretypewriter:typewriter_hub");
    private static final String PLAYER_EXTENSION = "dev.simulated_team.simulated.mixin_interface.PlayerTypewriterExtension";
    private static final String INTERACTION = "dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler";
    private static final String TYPEWRITER = "dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity";
    private static final String HUB_DISCONNECT = "org.sparklezfish.drivebywire.typewriter.network.TypewriterHubDisconnectPacket";

    private static boolean lookedUp;
    private static Method currentTypewriter;
    private static Method setCurrentTypewriter;
    private static Method mode;
    private static Method checkUser;
    private static Method disconnectUser;
    private static java.lang.reflect.Constructor<?> hubDisconnect;
    private static CameraType sableView;
    private static boolean wasAtHub;
    /** The last hub this player sat at, watched for the stale user the server sends back. */
    private static BlockPos lastHub;
    private static int lastNudge = -20;

    private TypewriterCamera() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        lookUp(player);
        if (currentTypewriter == null) return;

        BlockPos at = hubAt(minecraft, player);
        boolean atHub = at != null;
        if (atHub) lastHub = at;
        else mendStaleUser(minecraft, player);

        if (atHub == wasAtHub) return;
        wasAtHub = atHub;
        if (atHub) {
            if (sableView != null) minecraft.options.setCameraType(sableView);
        } else if (minecraft.options.getCameraType() == sableView) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        wasAtHub = false;
        lastHub = null;
    }

    /** The hub this client is typing at, or null. */
    private static BlockPos hubAt(Minecraft minecraft, LocalPlayer player) {
        try {
            if (!(currentTypewriter.invoke(player) instanceof BlockPos pos) || !isHub(minecraft.level.getBlockEntity(pos))) return null;
            return "ACTIVE".equals(modeName()) ? pos : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void mendStaleUser(Minecraft minecraft, LocalPlayer player) {
        if (lastHub == null || checkUser == null || disconnectUser == null || !"IDLE".equals(modeName())) return;
        BlockEntity block = minecraft.level.getBlockEntity(lastHub);
        if (!isHub(block)) return;
        try {
            UUID me = player.getUUID();
            if (!Boolean.TRUE.equals(checkUser.invoke(block, me))) return;
            // Client side this only forgets the user (and the typewriter input, which is off already)
            disconnectUser.invoke(block);
            if (setCurrentTypewriter != null && lastHub.equals(currentTypewriter.invoke(player))) setCurrentTypewriter.invoke(player, (Object) null);
            // And the server lets go too, in case it still holds this player (it ignores it otherwise); at most once a second
            if (hubDisconnect != null && Math.abs(player.tickCount - lastNudge) >= 20) {
                lastNudge = player.tickCount;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        (net.minecraft.network.protocol.common.custom.CustomPacketPayload) hubDisconnect.newInstance(lastHub));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            UnrealZaruba.LOGGER.debug("[TypewriterCamera] Could not mend the hub's user: {}", e.toString());
        }
    }

    private static boolean isHub(BlockEntity block) {
        return block != null && HUB.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(block.getType()));
    }

    /** Simulated's typewriter input on this client: ACTIVE while typing at one, IDLE when at none. */
    private static String modeName() {
        if (mode == null) return null;
        try {
            Object value = mode.invoke(null);
            return value instanceof Enum<?> e ? e.name() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void lookUp(LocalPlayer player) {
        if (lookedUp) return;
        lookedUp = true;
        ClassLoader loader = player.getClass().getClassLoader();
        try {
            Class<?> extension = Class.forName(PLAYER_EXTENSION, false, loader);
            currentTypewriter = extension.getMethod("simulated$getCurrentTypewriter");
            setCurrentTypewriter = extension.getMethod("simulated$setCurrentTypewriter", BlockPos.class);
            mode = Class.forName(INTERACTION, false, loader).getMethod("getMode");
            Class<?> typewriter = Class.forName(TYPEWRITER, false, loader);
            checkUser = typewriter.getMethod("checkUser", UUID.class);
            disconnectUser = typewriter.getMethod("disconnectUser");
            try {
                hubDisconnect = Class.forName(HUB_DISCONNECT, false, loader).getConstructor(BlockPos.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                hubDisconnect = null; // an older Drive By Wire: the client side is still mended
            }
            for (CameraType type : CameraType.values()) {
                if (type.name().equals("SUB_LEVEL_VIEW")) sableView = type;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            currentTypewriter = null;
            UnrealZaruba.LOGGER.info("[TypewriterCamera] Simulated or Sable is not there, hub camera is off: {}", e.toString());
        }
    }
}
