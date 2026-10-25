package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * What the client gets to draw the buy menu: the map's config with item strings already resolved to stacks for the
 * buyer's team. Indices match the server config, purchases refer to entries by them.
 */
public record BuyMenuView(List<Tab> tabs) {
    public record Tab(String name, String layout, List<Group> groups) {
        public boolean isSets() {
            return EconomyConfig.Layout.SETS.equals(layout);
        }
    }

    public record Group(String name, List<Category> categories) {}

    public record Category(String name, String hint, String note, Map<String, Integer> stats, List<Entry> entries) {}

    /** {@code index} is the entry's place in the server config (purchases use it), {@code compatible} is false for
     *  attachments and ammo that fit none of the guns the buyer carries. {@code stack} is empty when the config's item string didn't resolve; such entries can't be bought. */
    public record Entry(String name, String note, String slot, int price, ItemStack stack,
                        Map<String, Integer> stats, Map<String, String> info,
                        int index, boolean compatible) {
        public boolean available() {
            return !stack.isEmpty();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        RegistryFriendlyByteBuf registryBuf = (RegistryFriendlyByteBuf) buf;
        buf.writeCollection(tabs, (b, tab) -> {
            b.writeUtf(tab.name);
            b.writeUtf(tab.layout);
            b.writeCollection(tab.groups, (b2, group) -> {
                b2.writeUtf(group.name);
                b2.writeCollection(group.categories, (b3, category) -> {
                    b3.writeUtf(category.name);
                    b3.writeUtf(category.hint);
                    b3.writeUtf(category.note);
                    b3.writeMap(category.stats, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                    b3.writeCollection(category.entries, (b4, entry) -> {
                        b4.writeUtf(entry.name);
                        b4.writeUtf(entry.note);
                        b4.writeUtf(entry.slot);
                        b4.writeVarInt(entry.price);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf, entry.stack);
                        b4.writeMap(entry.stats, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                        b4.writeMap(entry.info, FriendlyByteBuf::writeUtf, (b5, value) -> b5.writeUtf(value));
                        b4.writeVarInt(entry.index);
                        b4.writeBoolean(entry.compatible);
                    });
                });
            });
        });
    }

    public static BuyMenuView decode(FriendlyByteBuf buf) {
        RegistryFriendlyByteBuf registryBuf = (RegistryFriendlyByteBuf) buf;
        List<Tab> tabs = buf.readList(b -> new Tab(b.readUtf(), b.readUtf(),
                b.readList(b2 -> new Group(b2.readUtf(),
                        b2.readList(b3 -> new Category(b3.readUtf(), b3.readUtf(), b3.readUtf(), readStats(b3),
                                b3.readList(b4 -> new Entry(b4.readUtf(), b4.readUtf(), b4.readUtf(), b4.readVarInt(),
                                        ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuf), readStats(b4),
                                        b4.readMap(LinkedHashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf),
                                        b4.readVarInt(), b4.readBoolean()))))))));
        return new BuyMenuView(tabs);
    }

    private static Map<String, Integer> readStats(FriendlyByteBuf buf) {
        return buf.readMap(LinkedHashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt);
    }

    /** Builds the view for one player's team. Missing config fields become empty strings and maps. */
    public static BuyMenuView of(EconomyConfig config, java.util.function.Function<EconomyConfig.Item, ItemStack> resolver,
                                 java.util.function.Predicate<ItemStack> fits) {
        List<Tab> tabs = new ArrayList<>();
        for (EconomyConfig.Tab tab : list(config.buyMenu)) {
            List<Group> groups = new ArrayList<>();
            for (EconomyConfig.Group group : list(tab.groups)) {
                List<Category> categories = new ArrayList<>();
                for (EconomyConfig.Category category : list(group.categories)) {
                    List<Entry> entries = new ArrayList<>();
                    int index = 0;
                    for (EconomyConfig.Item item : list(category.items)) {
                        ItemStack stack = resolver.apply(item);
                        entries.add(new Entry(str(item.name), str(item.note), str(item.slot), item.price,
                                stack, map(item.stats), map(item.info), index++, stack.isEmpty() || fits.test(stack)));
                    }
                    // What does not fit the buyer's guns goes last; the weapons tab goes by price inside each part, the other
                    // tabs keep the config's order (the sort is stable)
                    if (!EconomyConfig.Layout.SETS.equals(tab.layout)) {
                        java.util.Comparator<Entry> order = java.util.Comparator.comparing(entry -> !entry.compatible());
                        if (DefaultBuyMenu.WEAPONS_TAB.equals(tab.name)) order = order.thenComparingInt(Entry::price);
                        entries.sort(order);
                    }
                    categories.add(new Category(str(category.name), str(category.hint), str(category.note),
                            map(category.stats), entries));
                }
                groups.add(new Group(str(group.name), categories));
            }
            tabs.add(new Tab(str(tab.name), tab.layout != null ? tab.layout : EconomyConfig.Layout.GRID, groups));
        }
        return new BuyMenuView(tabs);
    }

    private static String str(String value) {
        return value != null ? value : "";
    }

    private static <T> List<T> list(List<T> value) {
        return value != null ? value : List.of();
    }

    private static <K, V> Map<K, V> map(Map<K, V> value) {
        return value != null ? value : Map.of();
    }
}
