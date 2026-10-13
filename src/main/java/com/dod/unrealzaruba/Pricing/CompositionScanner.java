package com.dod.unrealzaruba.Pricing;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Counts blocks and the stacks inside them. The NBT walk is a port of the backend SchematicScanner (scanner version
 * {@link #VERSION}); it must keep giving the same answers, which the shared golden vectors check.
 *
 * <p>Containers are not special-cased. Every block entity tag is searched recursively for item stacks ({@code id} +
 * {@code count}, or {@code Count}) and fluid stacks ({@code id}/{@code FluidName}/{@code fluid}/{@code Fluid} +
 * {@code amount}/{@code Amount}). Stacks inside a stack (shulker box, bundle) are multiplied by the outer stack size.
 */
public final class CompositionScanner {
    /** The backend scanner version this port matches (SchematicComposition.CurrentScanner). */
    public static final int VERSION = 6;

    private static final Set<String> IGNORED_BLOCKS =
            Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void");
    private static final String[] FLUID_ID_KEYS = {"id", "FluidName", "fluid", "Fluid"};
    private static final String[] FLUID_AMOUNT_KEYS = {"amount", "Amount"};
    private static final String[] ITEM_COUNT_KEYS = {"count", "Count"};

    private CompositionScanner() {}

    /** Reads the blocks of a Sable sub-level plot (the box min..max, plot coordinates) as they are now. */
    public static Composition scanLive(ServerLevel level, BlockPos min, BlockPos max) {
        Composition composition = new Composition();
        int[] lo = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] hi = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    if (IGNORED_BLOCKS.contains(name)) continue;
                    Composition.add(composition.blocks, name, 1);
                    lo[0] = Math.min(lo[0], x); hi[0] = Math.max(hi[0], x);
                    lo[1] = Math.min(lo[1], y); hi[1] = Math.max(hi[1], y);
                    lo[2] = Math.min(lo[2], z); hi[2] = Math.max(hi[2], z);

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        walkChildren(blockEntity.saveWithFullMetadata(level.registryAccess()), 1, composition);
                    }
                }
            }
        }
        if (hi[0] >= lo[0]) composition.size = new int[]{hi[0] - lo[0] + 1, hi[1] - lo[1] + 1, hi[2] - lo[2] + 1};
        return composition;
    }

    public static void walkChildren(CompoundTag compound, long multiplier, Composition into) {
        for (String key : compound.getAllKeys()) {
            walk(compound.get(key), multiplier, into);
        }
    }

    public static void walk(Tag node, long multiplier, Composition into) {
        if (node instanceof ListTag list) {
            for (Tag element : list) walk(element, multiplier, into);
        } else if (node instanceof CompoundTag compound) {
            String itemId = tryItemId(compound);
            if (itemId != null) {
                long count = itemCount(compound);
                Composition.add(into.items, itemId, multiply(count, multiplier));
                // Stacks inside the stack (shulker box, bundle) are multiplied by this stack size.
                for (String key : new String[]{"components", "tag"}) {
                    if (compound.contains(key, Tag.TAG_COMPOUND)) {
                        walkChildren(compound.getCompound(key), multiply(count, multiplier), into);
                    }
                }
                return;
            }
            Fluid fluid = tryFluid(compound);
            if (fluid != null) {
                Composition.add(into.fluids, fluid.id(), multiply(fluid.amount(), multiplier));
            } else {
                walkChildren(compound, multiplier, into);
            }
        }
    }

    /** The stack item id if this compound is an item stack (an id and a count), else null. */
    private static String tryItemId(CompoundTag compound) {
        String name = string(compound, "id");
        if (name == null || !isResourceId(name)) return null;
        for (String key : ITEM_COUNT_KEYS) {
            if (integer(compound, key) != null) return name;
        }
        return null;
    }

    private static long itemCount(CompoundTag compound) {
        for (String key : ITEM_COUNT_KEYS) {
            Long value = integer(compound, key);
            if (value != null) return Math.max(0, Math.min(value, Composition.COUNT_CAP));
        }
        return 0;
    }

    private record Fluid(String id, long amount) {}

    private static Fluid tryFluid(CompoundTag compound) {
        Long found = null;
        for (String key : FLUID_AMOUNT_KEYS) {
            Long value = integer(compound, key);
            if (value != null) {
                found = value;
                break;
            }
        }
        if (found == null) return null;
        for (String key : FLUID_ID_KEYS) {
            String name = string(compound, key);
            if (name != null && isResourceId(name)) {
                if (name.equals("minecraft:empty")) return null;
                return new Fluid(name, Math.max(0, Math.min(found, Composition.COUNT_CAP)));
            }
        }
        return null;
    }

    private static String string(CompoundTag compound, String key) {
        return compound.contains(key, Tag.TAG_STRING) ? compound.getString(key) : null;
    }

    /** Only integer tags count as counts and amounts (a double 1.5 is skipped), like the backend reader. */
    private static Long integer(CompoundTag compound, String key) {
        Tag tag = compound.get(key);
        if (tag instanceof ByteTag || tag instanceof ShortTag || tag instanceof IntTag || tag instanceof LongTag) {
            return ((NumericTag) tag).getAsLong();
        }
        return null;
    }

    private static boolean isResourceId(String value) {
        return value.length() > 2 && value.length() <= 128 && value.contains(":");
    }

    private static long multiply(long a, long b) {
        return b != 0 && a > Composition.COUNT_CAP / b ? Composition.COUNT_CAP : a * b;
    }
}
