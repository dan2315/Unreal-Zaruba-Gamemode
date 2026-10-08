package com.dod.unrealzaruba.BuildKit;

import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Applies build files written by the worldkit generators (tools/worldkit/generators/Canvas) to a level in one go, and
 * undoes them. A build file has one box per line, absolute coordinates, applied in order:
 * <pre>
 *   clear  x0 y0 z0 x1 y1 z1 reason...   empty the box (only when applied with clears)
 *   place  x0 y0 z0 x1 y1 z1 block       put the block where the world has air, water or replaceable plants
 *   ground x0 y0 z0 x1 y1 z1 block       same, and it may replace natural ground (dirt, grass block, sand, gravel)
 * </pre>
 * Placement is additive: anything else already there stays, and is listed in the report. Every change is journaled
 * (previous state and block entity) so undo puts back exactly what was there.
 * Files live in unrealzaruba/builds/: name.build, undo/name-time.nbt, reports/name.txt.
 */
public final class BuildKit {
    public static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Path DIR = Path.of("unrealzaruba", "builds");
    private static final int MAX_BLOCKS = 500_000;

    public record Result(int placed, int already, int skipped, int cleared, int clearsNotRun, Map<String, Integer> blockers,
                         Path report, Path journal) {}

    private BuildKit() {
    }

    public static List<String> list() throws IOException {
        if (!Files.isDirectory(DIR)) {
            return List.of();
        }
        try (var files = Files.list(DIR)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".build"))
                    .map(n -> n.substring(0, n.length() - 6)).sorted().toList();
        }
    }

    /** Applies (or with dryRun only counts) a build. */
    public static Result apply(ServerLevel level, String name, boolean runClears, boolean dryRun) throws IOException, CommandSyntaxException {
        List<String> lines = Files.readAllLines(DIR.resolve(name + ".build"), StandardCharsets.UTF_8);
        HolderLookup<Block> blocks = level.holderLookup(Registries.BLOCK);
        Map<String, BlockStateParser.BlockResult> parsed = new HashMap<>();

        ListTag journal = new ListTag();
        Set<BlockPos> journaled = new HashSet<>();
        List<BlockPos> changed = new ArrayList<>();
        List<String> skippedLines = new ArrayList<>();
        Map<String, Integer> blockers = new TreeMap<>();
        int placed = 0, already = 0, skipped = 0, cleared = 0, clearsNotRun = 0, total = 0;

        for (int n = 0; n < lines.size(); n++) {
            String line = lines.get(n).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(" ", 8);
            if (parts.length < 8) {
                throw new IOException("line " + (n + 1) + ": expected '<op> x0 y0 z0 x1 y1 z1 <block|reason>'");
            }
            String op = parts[0];
            int x0 = Integer.parseInt(parts[1]), y0 = Integer.parseInt(parts[2]), z0 = Integer.parseInt(parts[3]);
            int x1 = Integer.parseInt(parts[4]), y1 = Integer.parseInt(parts[5]), z1 = Integer.parseInt(parts[6]);
            total += (Math.abs(x1 - x0) + 1) * (Math.abs(y1 - y0) + 1) * (Math.abs(z1 - z0) + 1);
            if (total > MAX_BLOCKS) {
                throw new IOException("build is larger than " + MAX_BLOCKS + " blocks");
            }

            if (op.equals("clear")) {
                if (!runClears) {
                    clearsNotRun++;
                    continue;
                }
                for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
                    BlockState current = level.getBlockState(pos);
                    if (current.isAir()) {
                        continue;
                    }
                    cleared++;
                    if (!dryRun) {
                        BlockPos p = pos.immutable();
                        record(level, p, journal, journaled);
                        Clearable.tryClear(level.getBlockEntity(p));
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        changed.add(p);
                    }
                }
                continue;
            }

            boolean ground = op.equals("ground");
            if (!ground && !op.equals("place")) {
                throw new IOException("line " + (n + 1) + ": unknown op " + op);
            }
            BlockStateParser.BlockResult block = parsed.get(parts[7]);
            if (block == null) {
                block = BlockStateParser.parseForBlock(blocks, parts[7], true);
                parsed.put(parts[7], block);
            }
            BlockInput input = new BlockInput(block.blockState(), block.properties().keySet(), block.nbt());
            for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
                BlockState current = level.getBlockState(pos);
                if (sameBlock(current, block)) {
                    already++;
                    continue;
                }
                if (!canReplace(current, ground)) {
                    skipped++;
                    String id = current.getBlock().builtInRegistryHolder().key().location().toString();
                    blockers.merge(id, 1, Integer::sum);
                    skippedLines.add(pos.getX() + " " + pos.getY() + " " + pos.getZ() + "  " + id + "  (wanted " + parts[7] + ")");
                    continue;
                }
                placed++;
                if (!dryRun) {
                    BlockPos p = pos.immutable();
                    record(level, p, journal, journaled);
                    Clearable.tryClear(level.getBlockEntity(p));
                    if (input.place(level, p, Block.UPDATE_CLIENTS)) {
                        changed.add(p);
                    }
                }
            }
        }

        Path journalFile = null;
        if (!dryRun) {
            for (BlockPos pos : changed) {
                level.blockUpdated(pos, level.getBlockState(pos).getBlock());
            }
            if (!journal.isEmpty()) {
                CompoundTag root = new CompoundTag();
                root.putString("build", name);
                root.putString("dimension", level.dimension().location().toString());
                root.put("blocks", journal);
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                journalFile = DIR.resolve("undo").resolve(name + "-" + time + ".nbt");
                Files.createDirectories(journalFile.getParent());
                NbtIo.writeCompressed(root, journalFile);
            }
        }
        Path report = DIR.resolve("reports").resolve(name + (dryRun ? "-check" : "") + ".txt");
        Files.createDirectories(report.getParent());
        List<String> out = new ArrayList<>();
        out.add((dryRun ? "check " : "apply ") + name + ": placed " + placed + ", already there " + already + ", skipped " + skipped
                + ", cleared " + cleared + (clearsNotRun > 0 ? ", clears not run " + clearsNotRun : ""));
        out.add("skipped (x y z, block in the way):");
        out.addAll(skippedLines);
        Files.write(report, out, StandardCharsets.UTF_8);
        UnrealZaruba.LOGGER.info("[BuildKit] {}", out.get(0));
        return new Result(placed, already, skipped, cleared, clearsNotRun, blockers, report, journalFile);
    }

    /** Puts back what the latest apply of the build changed. Returns the number of restored blocks. */
    public static int undo(ServerLevel level, String name) throws IOException {
        Path journalFile = latestJournal(name);
        if (journalFile == null) {
            throw new IOException("nothing to undo for " + name);
        }
        CompoundTag root = NbtIo.readCompressed(journalFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        if (!root.getString("dimension").equals(level.dimension().location().toString())) {
            throw new IOException("the journal is for " + root.getString("dimension"));
        }
        HolderLookup<Block> blocks = level.holderLookup(Registries.BLOCK);
        ListTag list = root.getList("blocks", Tag.TAG_COMPOUND);
        List<BlockPos> restored = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entry, "pos").orElseThrow();
            BlockState state = NbtUtils.readBlockState(blocks, entry.getCompound("state"));
            Clearable.tryClear(level.getBlockEntity(pos));
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (entry.contains("be")) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntity.loadWithComponents(entry.getCompound("be"), level.registryAccess());
                    blockEntity.setChanged();
                }
            }
            restored.add(pos);
        }
        for (BlockPos pos : restored) {
            level.blockUpdated(pos, level.getBlockState(pos).getBlock());
        }
        Files.move(journalFile, journalFile.resolveSibling(journalFile.getFileName() + ".undone"));
        UnrealZaruba.LOGGER.info("[BuildKit] undo {}: restored {} blocks", name, restored.size());
        return restored.size();
    }

    private static Path latestJournal(String name) throws IOException {
        Path dir = DIR.resolve("undo");
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (var files = Files.list(dir)) {
            return files.filter(p -> {
                String file = p.getFileName().toString();
                return file.startsWith(name + "-") && file.endsWith(".nbt") && file.length() == name.length() + 1 + 15 + 4;
            }).max(Comparator.comparing(p -> p.getFileName().toString())).orElse(null);
        }
    }

    // same block with the properties the build file names (others, like fence connections, follow the neighbours)
    private static boolean sameBlock(BlockState current, BlockStateParser.BlockResult wanted) {
        if (current.getBlock() != wanted.blockState().getBlock()) {
            return false;
        }
        for (var property : wanted.properties().entrySet()) {
            if (!current.getValue(property.getKey()).equals(property.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean canReplace(BlockState current, boolean ground) {
        if (current.canBeReplaced() || current.is(BlockTags.SMALL_FLOWERS) || current.is(BlockTags.TALL_FLOWERS)) {
            return true;
        }
        return ground && (current.is(BlockTags.DIRT) || current.is(BlockTags.SAND) || current.is(Blocks.GRAVEL));
    }

    // first change of a position wins: that's the state before the build
    private static void record(ServerLevel level, BlockPos pos, ListTag journal, Set<BlockPos> journaled) {
        if (!journaled.add(pos)) {
            return;
        }
        CompoundTag entry = new CompoundTag();
        entry.put("pos", NbtUtils.writeBlockPos(pos));
        entry.put("state", NbtUtils.writeBlockState(level.getBlockState(pos)));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            entry.put("be", blockEntity.saveWithFullMetadata(level.registryAccess()));
        }
        journal.add(entry);
    }
}
