package com.dod.unrealzaruba.Pricing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * Counts what a saved blueprint (the file a player shares) is made of: the same reading as the backend
 * {@code SchematicScanner} v6 for the blocks and the stacks inside them, so the price charged for spawning one equals the
 * price shown in the browser. Two formats: the toolgun sublevel snapshot (chunks of sections with a packed block palette)
 * and a vanilla structure NBT (palette plus block list). The size is not computed here; the toolgun reports it.
 */
public final class BlueprintScanner {
    private static final long MAX_UNPACKED_BYTES = 32L * 1024 * 1024;
    private static final int SECTION_BLOCKS = 4096;
    private static final Set<String> IGNORED_BLOCKS =
            Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void");

    private BlueprintScanner() {}

    /** Reads the gzip NBT bytes; throws IOException if they are not a readable blueprint. */
    public static Composition scan(byte[] data) throws IOException {
        CompoundTag root;
        try {
            root = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.create(MAX_UNPACKED_BYTES));
        } catch (RuntimeException e) {
            throw new IOException("not NBT: " + e.getMessage(), e);
        }
        List<CompoundTag> snapshots = new ArrayList<>();
        findChunks(root, 0, snapshots);
        return snapshots.isEmpty() ? scanStructure(root) : scanSublevels(root, snapshots);
    }

    // ---- toolgun sublevel snapshot ----

    private static Composition scanSublevels(CompoundTag root, List<CompoundTag> loose) throws IOException {
        Composition composition = new Composition();
        List<CompoundTag> parts = new ArrayList<>();
        if (root.contains("sublevels", Tag.TAG_LIST)) {
            ListTag list = root.getList("sublevels", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag part = list.getCompound(i);
                if (part.contains("plot", Tag.TAG_COMPOUND) && part.getCompound("plot").contains("chunks", Tag.TAG_COMPOUND)) {
                    parts.add(part);
                }
            }
        }
        if (parts.isEmpty()) {
            // Some other wrapping of chunks: each group of chunks is taken as it is
            for (CompoundTag chunks : loose) scanPart(chunks, composition);
        } else {
            for (CompoundTag part : parts) scanPart(part.getCompound("plot").getCompound("chunks"), composition);
        }
        return composition;
    }

    private static void scanPart(CompoundTag chunks, Composition into) throws IOException {
        for (String key : chunks.getAllKeys()) {
            if (!chunks.contains(key, Tag.TAG_COMPOUND)) throw new IOException("Chunk is not a compound");
            CompoundTag chunk = chunks.getCompound(key);
            if (chunk.contains("sections", Tag.TAG_COMPOUND)) {
                CompoundTag sections = chunk.getCompound("sections");
                for (String sectionKey : sections.getAllKeys()) {
                    if (!sections.contains(sectionKey, Tag.TAG_COMPOUND)) continue;
                    CompoundTag section = sections.getCompound(sectionKey);
                    if (section.contains("block_states", Tag.TAG_COMPOUND)) countSection(section.getCompound("block_states"), into);
                }
            }
            if (chunk.contains("block_entities", Tag.TAG_LIST)) {
                ListTag blockEntities = chunk.getList("block_entities", Tag.TAG_COMPOUND);
                for (int i = 0; i < blockEntities.size(); i++) {
                    CompositionScanner.walkChildren(blockEntities.getCompound(i), 1, into);
                }
            }
        }
    }

    /** Palette indices are packed like in the chunk format: at least 4 bits each, none split across two longs. */
    private static void countSection(CompoundTag states, Composition into) throws IOException {
        if (!states.contains("palette", Tag.TAG_LIST)) throw new IOException("Section without a palette");
        ListTag palette = states.getList("palette", Tag.TAG_COMPOUND);
        String[] names = new String[palette.size()];
        for (int i = 0; i < names.length; i++) {
            CompoundTag entry = palette.getCompound(i);
            if (!entry.contains("Name", Tag.TAG_STRING)) throw new IOException("Palette entry without a name");
            names[i] = entry.getString("Name");
        }
        if (names.length == 0) throw new IOException("Empty section palette");

        if (names.length == 1) {
            if (!IGNORED_BLOCKS.contains(names[0])) Composition.add(into.blocks, names[0], SECTION_BLOCKS);
            return;
        }
        if (!(states.get("data") instanceof LongArrayTag array)) throw new IOException("Section without block data");
        long[] data = array.getAsLongArray();
        int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(names.length - 1));
        int perLong = 64 / bits;
        if (data.length < (SECTION_BLOCKS + perLong - 1) / perLong) throw new IOException("Section block data is too short");

        long mask = (1L << bits) - 1;
        long[] counts = new long[names.length];
        for (int i = 0; i < SECTION_BLOCKS; i++) {
            int index = (int) ((data[i / perLong] >>> (i % perLong * bits)) & mask);
            if (index >= names.length) throw new IOException("Block state is outside the palette");
            counts[index]++;
        }
        for (int i = 0; i < names.length; i++) {
            if (counts[i] > 0 && !IGNORED_BLOCKS.contains(names[i])) Composition.add(into.blocks, names[i], counts[i]);
        }
    }

    /** Finds {@code chunks} at any shallow place of a file, so a different wrapping of the same data still reads. */
    private static void findChunks(Tag node, int depth, List<CompoundTag> found) {
        if (depth > 6) return;
        if (node instanceof CompoundTag compound) {
            if (compound.contains("chunks", Tag.TAG_COMPOUND)) {
                found.add(compound.getCompound("chunks"));
                return;
            }
            for (String key : compound.getAllKeys()) findChunks(compound.get(key), depth + 1, found);
        } else if (node instanceof ListTag list) {
            for (Tag child : list) findChunks(child, depth + 1, found);
        }
    }

    // ---- vanilla structure NBT ----

    private static Composition scanStructure(CompoundTag root) throws IOException {
        ListTag palette = null;
        if (root.contains("palette", Tag.TAG_LIST)) {
            palette = root.getList("palette", Tag.TAG_COMPOUND);
        } else if (root.contains("palettes", Tag.TAG_LIST) && root.getList("palettes", Tag.TAG_LIST).size() > 0) {
            palette = root.getList("palettes", Tag.TAG_LIST).getList(0);
        }
        if (palette == null) throw new IOException("Not a structure: no block palette");
        if (!root.contains("blocks", Tag.TAG_LIST)) throw new IOException("Not a structure: no block list");

        String[] names = new String[palette.size()];
        for (int i = 0; i < names.length; i++) {
            CompoundTag entry = palette.getCompound(i);
            if (!entry.contains("Name", Tag.TAG_STRING)) throw new IOException("Palette entry without a name");
            names[i] = entry.getString("Name");
        }

        Composition composition = new Composition();
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i);
            if (!block.contains("state")) throw new IOException("Block without a state");
            int state = block.getInt("state");
            if (state < 0 || state >= names.length) throw new IOException("Block state is outside the palette");
            String name = names[state];
            if (IGNORED_BLOCKS.contains(name)) continue;
            Composition.add(composition.blocks, name, 1);
            if (block.contains("nbt", Tag.TAG_COMPOUND)) CompositionScanner.walkChildren(block.getCompound("nbt"), 1, composition);
        }
        // Entities (item frames, minecart chests, ...) hold stacks the same way
        if (root.contains("entities", Tag.TAG_LIST)) {
            ListTag entities = root.getList("entities", Tag.TAG_COMPOUND);
            for (int i = 0; i < entities.size(); i++) {
                CompoundTag entity = entities.getCompound(i);
                if (entity.contains("nbt", Tag.TAG_COMPOUND)) CompositionScanner.walkChildren(entity.getCompound("nbt"), 1, composition);
            }
        }
        return composition;
    }
}
