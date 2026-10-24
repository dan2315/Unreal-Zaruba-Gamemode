import java.nio.file.*;
import java.util.*;

/**
 * Copies a box of the map into a blueprint (see Blueprint) for the library in tools/worldkit/blueprints/.
 * Air is stored as "nothing". With --built-only, natural terrain and plants are left out too (grass, dirt, stone,
 * leaves of trees around, water), so what is kept is the building itself.
 * Usage: java Export <region dir> <out.bp> <name> x0 y0 z0 x1 y1 z1 <groundY> [--built-only] [--tags a,b]
 */
public class Export {
    static final Set<String> NATURAL = new HashSet<>(Arrays.asList(
            "grass_block", "dirt", "coarse_dirt", "stone", "moss_block", "moss_carpet", "lime_terracotta", "green_concrete_powder",
            "short_grass", "tall_grass", "fern", "large_fern", "water", "sand", "gravel", "andesite", "granite", "diorite", "deepslate",
            "tuff", "clay", "podzol", "rooted_dirt", "vine", "sugar_cane", "bedrock", "poppy", "dandelion", "cornflower", "oxeye_daisy",
            "azure_bluet", "allium", "lily_of_the_valley", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "snow",
            "sweet_berry_bush", "red_tulip", "white_tulip", "orange_tulip", "pink_tulip", "lilac", "rose_bush", "peony", "sunflower",
            "dead_bush", "lily_pad", "pink_petals", "bush"));

    public static void main(String[] a) throws Exception {
        Region region = new Region(Path.of(a[0]));
        Path out = Path.of(a[1]);
        String name = a[2];
        int x0 = Integer.parseInt(a[3]), y0 = Integer.parseInt(a[4]), z0 = Integer.parseInt(a[5]);
        int x1 = Integer.parseInt(a[6]), y1 = Integer.parseInt(a[7]), z1 = Integer.parseInt(a[8]);
        int groundY = a[9].equals("auto") ? autoGround(region, Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)) : Integer.parseInt(a[9]);
        List<String> rest = Arrays.asList(a).subList(10, a.length);
        boolean builtOnly = rest.contains("--built-only");
        int tagsAt = rest.indexOf("--tags");
        Blueprint bp = export(region, Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), builtOnly);
        bp.name = name;
        bp.ground = groundY - Math.min(y0, y1);
        if (tagsAt >= 0) bp.tags.addAll(Arrays.asList(rest.get(tagsAt + 1).split(",")));
        bp.save(out);
        System.out.print("ground " + groundY + "; "); System.out.println(out + ": " + bp.w + "x" + bp.h + "x" + bp.d + ", " + bp.count() + " blocks, " + bp.palette.size() + " states");
    }

    /** The most common height of the natural ground on the edge of the box (where a building stands on it). */
    static int autoGround(Region region, int x0, int z0, int x1, int z1) throws Exception {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (x != x0 && x != x1 && z != z0 && z != z1) continue;
            int g = region.top(x, z, s -> natural(s) && !Region.name(s).contains("grass") && !Region.name(s).contains("fern") && !Region.name(s).endsWith("leaves") && !Region.name(s).endsWith("water"));
            if (g != Integer.MIN_VALUE) counts.merge(g, 1, Integer::sum);
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(32);
    }

    static Blueprint export(Region region, int x0, int y0, int z0, int x1, int y1, int z1, boolean builtOnly) throws Exception {
        Blueprint bp = new Blueprint(x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1);
        bp.source = new int[] {x0, y0, z0};
        for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) {
            String s = region.state(x, y, z);
            if (Region.isAir(s)) continue;
            if (builtOnly && natural(s)) continue;
            Map<String, Object> be = region.blockEntity(x, y, z);
            if (be != null) s = s + Region.snbt(be);
            bp.cells[y - y0][z - z0][x - x0] = bp.index(s);
        }
        return bp;
    }

    static boolean natural(String state) {
        String n = Region.name(state).replace("minecraft:", "");
        return NATURAL.contains(n) || n.endsWith("_leaves") || n.endsWith("_sapling") || n.endsWith("mushroom");
    }
}
