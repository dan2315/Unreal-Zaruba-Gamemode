package com.dod.unrealzaruba.UI.Feed;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Feed.FeedKind;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * The lines of the event feed the client has, newest last. The corner of the screen ({@link FeedOverlay}) shows the fresh ones for a
 * while; the whole history stays here for the TAB panel, which scrolls through it.
 */
public final class FeedState {
    /** How many lines are kept. */
    public static final int HISTORY = 150;

    /** One line. {@code icon} is empty for lines that have none. Colours are RGB. */
    public record Line(FeedKind kind, int colorA, int colorB, String nameA, String nameB, ItemStack icon, int reward, int bounty,
                       int streak, String key, List<String> args, long createdAt) {}

    private static final List<Line> LINES = new ArrayList<>();
    /** How many lines the TAB panel is scrolled up from the newest one. */
    private static int scroll;

    private FeedState() {}

    public static synchronized List<Line> lines() {
        return new ArrayList<>(LINES);
    }

    public static synchronized void clear() {
        LINES.clear();
        scroll = 0;
    }

    public static synchronized int scroll() {
        return scroll;
    }

    /** Scrolls the TAB panel by lines (positive: toward older ones); returns the new position. */
    public static synchronized int scrollBy(int lines, int visible) {
        scroll = Math.max(0, Math.min(Math.max(0, LINES.size() - visible), scroll + lines));
        return scroll;
    }

    public static synchronized void resetScroll() {
        scroll = 0;
    }

    public static synchronized void add(FeedKind kind, int colorA, int colorB, String nameA, String nameB, CompoundTag iconTag,
                                        int reward, int bounty, int streak, String key, List<String> args) {
        LINES.add(new Line(kind, colorA, colorB, nameA, nameB, decode(iconTag), reward, bounty, streak, key, List.copyOf(args),
                System.currentTimeMillis()));
        // A panel that is scrolled up keeps showing the same lines when new ones come
        if (scroll > 0) scroll++;
        while (LINES.size() > HISTORY) LINES.remove(0);
    }

    private static ItemStack decode(CompoundTag tag) {
        if (tag == null) return ItemStack.EMPTY;
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) return ItemStack.EMPTY;
            return ItemStack.OPTIONAL_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, level.registryAccess()), tag).result().orElse(ItemStack.EMPTY);
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }
}
