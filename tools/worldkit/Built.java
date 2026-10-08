import java.nio.file.*;
import java.util.*;

/**
 * Non-natural blocks in an area (y >= minY): counts with bounding boxes, and a top-down map where each cell
 * (step x step) shows '#' if it holds built blocks, 'w' water, 't' trees, '.' open ground. Also ground height per cell.
 * Usage: java Built <region dir> <x0> <z0> <x1> <z1> <step> <minY>
 */
public class Built {
    static final Set<String> NATURAL = new HashSet<>(Arrays.asList(
            "grass_block", "dirt", "coarse_dirt", "stone", "moss_block", "lime_terracotta", "green_concrete_powder",
            "short_grass", "grass", "tall_grass", "fern", "large_fern", "water", "sand", "gravel", "andesite", "granite",
            "diorite", "deepslate", "tuff", "clay", "podzol", "rooted_dirt", "vine", "sugar_cane", "bedrock", "dirt_path",
            "poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "lily_of_the_valley", "seagrass",
            "tall_seagrass", "kelp", "kelp_plant", "moss_carpet", "mossy_cobblestone", "cobblestone", "snow", "snow_block",
            "calcite", "dripstone_block", "sweet_berry_bush", "red_tulip", "white_tulip", "orange_tulip", "pink_tulip",
            "lilac", "rose_bush", "peony", "sunflower", "dead_bush", "copper_ore", "coal_ore", "iron_ore", "lily_pad"));

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        int step = Integer.parseInt(a[5]), minY = Integer.parseInt(a[6]);
        Map<String, int[]> box = new TreeMap<>(); // name -> count, minx, maxx, miny, maxy, minz, maxz
        int cols = (x1 - x0) / step + 1, rows = (z1 - z0) / step + 1;
        char[][] grid = new char[rows][cols];
        int[][] ground = new int[rows][cols];
        for (char[] r : grid) Arrays.fill(r, '.');
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int gr = (z - z0) / step, gc = (x - x0) / step;
            TreeMap<Integer, String> col = Surface.column(dir, x, z);
            int g = Integer.MIN_VALUE;
            boolean built = false, wet = false, tree = false;
            for (var e : col.entrySet()) {
                String n = e.getValue().replace("minecraft:", "");
                int y = e.getKey();
                if (y < minY) { continue; }
                boolean natural = NATURAL.contains(n) || n.endsWith("_leaves") || n.endsWith("_log") || n.endsWith("_wood")
                        || n.endsWith("_sapling") || n.endsWith("mushroom");
                if (n.equals("water")) wet = true;
                if (n.endsWith("_leaves") || n.endsWith("_log")) tree = true;
                if (!natural) {
                    built = true;
                    int[] b = box.computeIfAbsent(e.getValue(), k -> new int[] {0, 1 << 30, -(1 << 30), 1 << 30, -(1 << 30), 1 << 30, -(1 << 30)});
                    b[0]++; b[1] = Math.min(b[1], x); b[2] = Math.max(b[2], x); b[3] = Math.min(b[3], y); b[4] = Math.max(b[4], y);
                    b[5] = Math.min(b[5], z); b[6] = Math.max(b[6], z);
                } else if (!tree && !n.equals("water") && !n.endsWith("grass") && !n.endsWith("fern")) {
                    g = Math.max(g, y);
                }
            }
            ground[gr][gc] = Math.max(ground[gr][gc], g);
            char c = built ? '#' : wet ? 'w' : tree ? 't' : '.';
            if (grid[gr][gc] == '.' || c == '#') grid[gr][gc] = c;
            Surface.cache.size();
        }
        System.out.println("BUILT BLOCKS (count  name  x  y  z ranges):");
        box.entrySet().stream().sorted((p, q) -> q.getValue()[0] - p.getValue()[0]).limit(40).forEach(e -> {
            int[] b = e.getValue();
            System.out.printf("  %6d  %-42s x %d..%d  y %d..%d  z %d..%d%n", b[0], e.getKey(), b[1], b[2], b[3], b[4], b[5], b[6]);
        });
        System.out.println("MAP (x " + x0 + ".." + x1 + " left to right, z " + z0 + ".." + z1 + " top to bottom, " + step + " blocks per cell; # built, w water, t trees):");
        for (char[] r : grid) System.out.println("  " + new String(r));
        System.out.println("GROUND HEIGHT per cell (last two digits):");
        for (int[] r : ground) {
            StringBuilder sb = new StringBuilder("  ");
            for (int g : r) sb.append(g == Integer.MIN_VALUE ? " --" : String.format("%3d", Math.floorMod(g, 100)));
            System.out.println(sb);
        }
    }
}
