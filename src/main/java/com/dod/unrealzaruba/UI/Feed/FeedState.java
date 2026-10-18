package com.dod.unrealzaruba.UI.Feed;

import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Feed.FeedKind;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/** The lines of the event feed the client has, newest last. Lines go away on their own (see {@link FeedOverlay}). */
public final class FeedState {
    public static final int MAX_LINES = 7;

    /** One line. {@code icon} is empty for lines that have none. Colours are RGB. */
    public record Line(FeedKind kind, int colorA, int colorB, String nameA, String nameB, ItemStack icon, int reward, int bounty,
                       int streak, String key, List<String> args, long createdAt) {}

    private static final List<Line> LINES = new ArrayList<>();

    private FeedState() {}

    public static synchronized List<Line> lines() {
        return new ArrayList<>(LINES);
    }

    public static synchronized void clear() {
        LINES.clear();
    }

    public static synchronized void add(FeedKind kind, int colorA, int colorB, String nameA, String nameB, CompoundTag iconTag,
                                        int reward, int bounty, int streak, String key, List<String> args) {
        LINES.add(new Line(kind, colorA, colorB, nameA, nameB, decode(iconTag), reward, bounty, streak, key, List.copyOf(args),
                System.currentTimeMillis()));
        while (LINES.size() > MAX_LINES) LINES.remove(0);
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
