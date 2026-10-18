package com.dod.unrealzaruba.Feed;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.Feed.ClientboundFeedPacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The event feed: what happens in the match, as lines the clients draw in a corner of the screen. A kill shows the weapon, the
 * names in the colour of their teams and what it paid; the rest (a point taken, a score mark, a supply drop, a vehicle bought, a
 * bounty) is a line of text with the colour of the team it is about.
 */
public final class Feed {
    private static final int NEUTRAL = 0xDDDDEE;

    private Feed() {}

    /** The colour (RGB) a team is drawn with in the feed. */
    public static int colorOf(TeamColor team) {
        if (team == null) return NEUTRAL;
        return switch (team) {
            case RED -> 0xFF3B30;
            case BLUE -> 0x3B82FF;
            case YELLOW -> 0xFFD60A;
            case PURPLE -> 0xB347EA;
            default -> NEUTRAL;
        };
    }

    /** The team of a player, or null when they are in none. */
    public static TeamColor teamOf(ServerPlayer player) {
        if (player == null) return null;
        return PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext context && context.Team() != null
                ? context.Team().Color() : null;
    }

    public static int colorOf(ServerPlayer player) {
        return colorOf(teamOf(player));
    }

    /**
     * A death. {@code killer} is null for deaths with no player behind them (a fall, fire, a mob). {@code reward} is what the killer was
     * paid, {@code bounty} the bounty they took, {@code streak} how many they have killed in a row now.
     */
    public static void kill(MinecraftServer server, ServerPlayer victim, ServerPlayer killer, DamageSource source, int reward, int bounty, int streak) {
        if (server == null) return;
        ItemStack icon = weaponIcon(source, killer);
        send(server, new ClientboundFeedPacket(FeedKind.KILL, colorOf(killer), colorOf(victim),
                killer == null ? "" : killer.getGameProfile().getName(), victim.getGameProfile().getName(),
                encode(server, icon), reward, bounty, streak, "", List.of()));
    }

    /** An event with a line of text (a translation key and its arguments) in the colour of the team it is about. */
    public static void event(MinecraftServer server, FeedKind kind, int color, String key, String... args) {
        if (server == null) return;
        send(server, new ClientboundFeedPacket(kind, color, NEUTRAL, "", "", null, 0, 0, 0, key, List.of(args)));
    }

    private static void send(MinecraftServer server, ClientboundFeedPacket packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    /** The weapon for the kill line: what the killer holds, or an icon for how the player died. */
    private static ItemStack weaponIcon(DamageSource source, ServerPlayer killer) {
        if (source != null && source.is(DamageTypeTags.IS_EXPLOSION)) return new ItemStack(Items.TNT);
        if (killer != null && !killer.getMainHandItem().isEmpty()) return killer.getMainHandItem().copyWithCount(1);
        if (source != null) {
            if (source.is(DamageTypeTags.IS_FALL)) return new ItemStack(Items.FEATHER);
            if (source.is(DamageTypeTags.IS_FIRE)) return new ItemStack(Items.BLAZE_POWDER);
            if (source.is(DamageTypeTags.IS_DROWNING)) return new ItemStack(Items.WATER_BUCKET);
            if (source.is(DamageTypeTags.IS_PROJECTILE)) return new ItemStack(Items.ARROW);
        }
        return new ItemStack(Items.SKELETON_SKULL);
    }

    /** The stack as NBT with the registries of the server, or null if it cannot be written. */
    private static CompoundTag encode(MinecraftServer server, ItemStack stack) {
        try {
            RegistryAccess access = server.registryAccess();
            Tag tag = ItemStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, access), stack).result().orElse(null);
            return tag instanceof CompoundTag compound ? compound : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Names for the voice lines and the like: every player of a team. */
    public static List<ServerPlayer> playersOf(MinecraftServer server, TeamColor team) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teamOf(player) == team) players.add(player);
        }
        return players;
    }
}
