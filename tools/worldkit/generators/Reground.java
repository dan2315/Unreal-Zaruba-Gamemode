import java.nio.file.*;
import java.util.*;

/**
 * Re-covers an artificial ground patch (e.g. the coarse dirt laid under a build) so it matches the natural surface
 * around it. Learns from a band of natural columns around the area: which top block lies where and which plants grow
 * on it. Each target cell gets a material picked from the nearby natural ground (closer samples weigh more), in small
 * irregular patches (jittered-grid Voronoi cells), and plants at the same density as on that material outside.
 * Only cells whose top block at groundY is the given block are changed; roads, floors and buildings stay.
 * Usage: java -cp <worldkit classes> Reground <region dir> <x0> <z0> <x1> <z1> <groundY> <block to replace> > ground.build
 */
public class Reground {
    static final Set<String> GROUND = Set.of("grass_block", "moss_block", "lime_terracotta", "green_concrete_powder", "gravel",
            "dirt", "tuff", "andesite", "cobblestone", "mossy_cobblestone", "stone", "sand", "podzol", "clay", "coarse_dirt");
    static final Set<String> TALL = Set.of("tall_grass", "large_fern", "lilac", "peony", "rose_bush", "sunflower");
    static final Set<String> PLANTS = Set.of("short_grass", "fern", "tall_grass", "large_fern", "lilac", "peony", "rose_bush",
            "dandelion", "poppy", "azure_bluet", "oxeye_daisy", "cornflower", "allium", "red_tulip", "orange_tulip",
            "white_tulip", "pink_tulip", "lily_of_the_valley", "blue_orchid");
    static final int BAND = 24, RADIUS = 16, PATCH = 3;

    record Natural(int x, int z, String ground, String plant) {}

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        int gy = Integer.parseInt(a[5]);
        String target = a[6].contains(":") ? a[6] : "minecraft:" + a[6];

        // natural samples around the area, and the target cells inside it
        List<Natural> natural = new ArrayList<>();
        List<int[]> cells = new ArrayList<>();
        for (int x = x0 - BAND; x <= x1 + BAND; x++) {
            for (int z = z0 - BAND; z <= z1 + BAND; z++) {
                boolean inside = x >= x0 && x <= x1 && z >= z0 && z <= z1;
                TreeMap<Integer, String> col = Surface.column(dir, x, z);
                if (inside) {
                    if (target.equals(col.get(gy)) && (col.get(gy + 1) == null || isPlant(col.get(gy + 1)))) cells.add(new int[] {x, z});
                    continue;
                }
                for (int y = gy + 3; y >= gy - 4; y--) {
                    String b = col.get(y);
                    if (b == null || isPlant(b)) continue;
                    String name = b.substring(b.indexOf(':') + 1);
                    if (GROUND.contains(name) && !name.equals("coarse_dirt")) {
                        String above = col.get(y + 1);
                        natural.add(new Natural(x, z, name, above != null && isPlant(above) ? above.substring(above.indexOf(':') + 1) : null));
                    }
                    break;   // first non-plant block from the top decides (leaves, logs, builds: not a sample)
                }
            }
        }

        // plant density per ground material, and which plants
        Map<String, int[]> perGround = new HashMap<>();   // ground -> {cells, with plant}
        Map<String, Map<String, Integer>> plants = new HashMap<>();
        for (Natural n : natural) {
            int[] c = perGround.computeIfAbsent(n.ground, k -> new int[2]);
            c[0]++;
            if (n.plant != null) {
                c[1]++;
                plants.computeIfAbsent(n.ground, k -> new TreeMap<>()).merge(n.plant, 1, Integer::sum);
            }
        }
        System.err.println("samples " + natural.size() + ", cells to re-cover " + cells.size());
        perGround.forEach((g, c) -> System.err.println("  " + g + ": " + c[0] + " cells, plants on " + (100 * c[1] / c[0]) + "% " + plants.getOrDefault(g, Map.of())));

        Canvas canvas = new Canvas(0, 0);
        Map<Long, String> seedMaterial = new HashMap<>();
        Map<String, Integer> result = new TreeMap<>();
        for (int[] cell : cells) {
            int x = cell[0], z = cell[1];
            // nearest jittered seed decides the patch
            long best = 0;
            double bestD = Double.MAX_VALUE;
            int gx = Math.floorDiv(x, PATCH), gz = Math.floorDiv(z, PATCH);
            for (int i = -1; i <= 1; i++)
                for (int j = -1; j <= 1; j++) {
                    int sx = gx + i, sz = gz + j;
                    double px = sx * PATCH + hash(sx, sz, 1) * PATCH, pz = sz * PATCH + hash(sx, sz, 2) * PATCH;
                    double d = (px - x) * (px - x) + (pz - z) * (pz - z);
                    if (d < bestD) {
                        bestD = d;
                        best = ((long) sx << 32) | (sz & 0xFFFFFFFFL);
                    }
                }
            long seed = best;
            String ground = seedMaterial.computeIfAbsent(seed, s -> pick(natural, (int) (s >> 32) * PATCH, (int) (long) s * PATCH, hash((int) (s >> 32), (int) (long) s, 3)));
            canvas.ground(x, gy, z, x, gy, z, ground);
            result.merge(ground, 1, Integer::sum);

            int[] c = perGround.get(ground);
            if (c != null && hash(x, z, 4) < (double) c[1] / c[0]) {
                String plant = weighted(plants.get(ground), hash(x, z, 5));
                if (plant != null) {
                    if (TALL.contains(plant)) {
                        canvas.set(x, gy + 1, z, plant + "[half=lower]");
                        canvas.set(x, gy + 2, z, plant + "[half=upper]");
                    } else {
                        canvas.set(x, gy + 1, z, plant);
                    }
                }
            }
        }
        System.err.println("new ground: " + result);
        canvas.print(new String[0]);
    }

    // a material from the natural samples near (x, z), closer ones weigh more
    static String pick(List<Natural> natural, int x, int z, double r) {
        Map<String, Double> weight = new TreeMap<>();
        double total = 0;
        for (Natural n : natural) {
            double d2 = (n.x - x) * (n.x - x) + (n.z - z) * (n.z - z);
            if (d2 > 4.0 * RADIUS * RADIUS) continue;
            double w = 1 / (1 + d2 / (RADIUS * RADIUS / 4.0));
            weight.merge(n.ground, w, Double::sum);
            total += w;
        }
        if (total == 0) return "grass_block";
        double t = r * total;
        for (var e : weight.entrySet()) {
            t -= e.getValue();
            if (t <= 0) return e.getKey();
        }
        return weight.keySet().iterator().next();
    }

    static String weighted(Map<String, Integer> counts, double r) {
        if (counts == null || counts.isEmpty()) return null;
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        double t = r * total;
        for (var e : counts.entrySet()) {
            t -= e.getValue();
            if (t <= 0) return e.getKey();
        }
        return null;
    }

    static boolean isPlant(String block) {
        return PLANTS.contains(block.substring(block.indexOf(':') + 1));
    }

    static double hash(int x, int z, int salt) {
        long h = x * 73856093L ^ z * 19349663L ^ salt * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }
}
