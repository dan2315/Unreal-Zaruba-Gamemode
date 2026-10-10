import java.nio.file.*;
import java.util.*;

/**
 * Natural dirt trail along a Catmull-Rom curve through waypoints, in the style of the trail at 0,0 of the capture
 * points map: a soft-edged band where about half of the ground in the middle is coarse dirt / podzol / rooted dirt,
 * thinning out to the edges, and the painted "grass" (lime terracotta, green concrete powder) near the middle turns
 * into moss and grass block. The edge wobbles along the way. Reads the region files to find each column's surface and
 * emits swaps, so only the exact block it saw is replaced (farmland, roads, builds and anything changed since stay).
 * Path blocks come in small clumps (jittered 2-block grid). Optionally a loose line of oak log stumps along one edge,
 * like the posts beside the 0,0 trail.
 * Usage: java -cp <worldkit classes> Pathway <region dir> <half width> x z x z ... [--stumps=1|-1] > path.build
 */
public class Pathway {
    static final Set<String> SURFACE = Set.of("grass_block", "moss_block", "lime_terracotta", "green_concrete_powder",
            "dirt", "coarse_dirt", "podzol", "rooted_dirt");
    static final Set<String> PAINTED = Set.of("lime_terracotta", "green_concrete_powder");
    static final String[] PATH = {"coarse_dirt", "podzol", "rooted_dirt"};
    static final Set<String> PLANTS = Set.of("short_grass", "fern", "tall_grass", "large_fern", "dandelion", "poppy",
            "azure_bluet", "oxeye_daisy", "cornflower", "allium", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip");

    public static void main(String[] args) throws Exception {
        // --stumps=1 / --stumps=-1: a line of log stumps on the right / left of the direction of travel (1 = north when heading west)
        // --density=0.68: share of path blocks in the middle; --clear-trees: remove whole trees standing in the way
        int stumpSide = 0;
        double density = 0.68;
        boolean clearTrees = false;
        List<String> rest = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--stumps=")) stumpSide = Integer.parseInt(arg.substring(9));
            else if (arg.startsWith("--density=")) density = Double.parseDouble(arg.substring(10));
            else if (arg.equals("--clear-trees")) clearTrees = true;
            else rest.add(arg);
        }
        String[] a = rest.toArray(new String[0]);
        Path dir = Path.of(a[0]);
        double halfWidth = Double.parseDouble(a[1]);
        double[][] points = new double[(a.length - 2) / 2][];
        for (int i = 0; i < points.length; i++) points[i] = new double[] {Double.parseDouble(a[2 + 2 * i]), Double.parseDouble(a[3 + 2 * i])};

        // samples along the curve with their arc length
        List<double[]> curve = new ArrayList<>();   // x, z, s
        double s = 0;
        for (int i = 0; i < points.length - 1; i++) {
            double[] p0 = points[Math.max(0, i - 1)], p1 = points[i], p2 = points[i + 1], p3 = points[Math.min(points.length - 1, i + 2)];
            int n = (int) Math.ceil(Math.hypot(p2[0] - p1[0], p2[1] - p1[1]) / 0.5);
            for (int k = 0; k < n; k++) {
                double t = (double) k / n;
                double x = cr(p0[0], p1[0], p2[0], p3[0], t), z = cr(p0[1], p1[1], p2[1], p3[1], t);
                if (!curve.isEmpty()) s += Math.hypot(x - curve.get(curve.size() - 1)[0], z - curve.get(curve.size() - 1)[1]);
                curve.add(new double[] {x, z, s});
            }
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (double[] c : curve) {
            minX = Math.min(minX, (int) Math.floor(c[0] - 2 * halfWidth)); maxX = Math.max(maxX, (int) Math.ceil(c[0] + 2 * halfWidth));
            minZ = Math.min(minZ, (int) Math.floor(c[1] - 2 * halfWidth)); maxZ = Math.max(maxZ, (int) Math.ceil(c[1] + 2 * halfWidth));
        }

        Canvas canvas = new Canvas(0, 0);
        Set<Long> removed = clearTrees ? clearTrees(dir, canvas, curve, halfWidth, minX, minZ, maxX, maxZ) : Set.of();
        Map<String, Integer> made = new TreeMap<>();
        int columns = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double best = Double.MAX_VALUE, at = 0;
                for (double[] c : curve) {
                    double d = (c[0] - x) * (c[0] - x) + (c[1] - z) * (c[1] - z);
                    if (d < best) { best = d; at = c[2]; }
                }
                double width = halfWidth * (1 + 0.1 * Math.sin(at / 13) + 0.06 * Math.sin(at / 6.1 + 1));
                double u = Math.sqrt(best) / width;
                if (u > 1.05) continue;

                TreeMap<Integer, String> col = Surface.column(dir, x, z);
                int y = surface(col, removed, x, z);
                if (y == Integer.MIN_VALUE) continue;
                columns++;
                String current = col.get(y).substring(col.get(y).indexOf(':') + 1);
                // dense middle, thinning out over the outer half; decided per 2-block clump with some per-cell noise
                double pathChance = u < 0.45 ? density : density * (1.05 - u) / 0.6;
                long clump = clump(x, z);
                int cx = (int) (clump >> 32), cz = (int) clump;
                String block = null;
                if (0.7 * hash(cx, cz, 1) + 0.3 * hash(x, z, 1) < pathChance) {
                    int pick = (int) ((hash(x, z, 6) < 0.75 ? hash(cx, cz, 2) : hash(x, z, 2)) * PATH.length);
                    block = PATH[pick];
                } else if (PAINTED.contains(current) && u < 0.7) {
                    block = hash(cx, cz, 3) < 0.6 ? "moss_block" : "grass_block";
                }
                if (block == null || block.equals(current)) continue;
                canvas.swap(x, y, z, current, block);
                made.merge(block, 1, Integer::sum);
            }
        }
        int stumps = stumps(dir, canvas, curve, halfWidth, stumpSide);
        System.err.println("trail length " + Math.round(s) + ", columns " + columns + ", changes " + made + ", stumps " + stumps);
        canvas.print(new String[0]);
    }

    // oak log stumps (2-4 high, mostly 2-3) every ~4 blocks along one edge, 0.9-1.5 half widths from the middle
    static int stumps(Path dir, Canvas canvas, List<double[]> curve, double halfWidth, int side) throws Exception {
        if (side == 0) return 0;
        int count = 0, i = 0;
        List<int[]> placed = new ArrayList<>();
        double next = 2;
        while (i < curve.size() - 1) {
            double[] c = curve.get(i);
            if (c[2] < next) { i++; continue; }
            double[] d = curve.get(Math.min(i + 2, curve.size() - 1));
            double dx = d[0] - c[0], dz = d[1] - c[1], len = Math.hypot(dx, dz);
            int k = (int) c[2];
            next = c[2] + 3 + 2.5 * hash(k, 0, 7);
            if (len == 0) continue;
            double off = halfWidth * (0.9 + 0.6 * hash(k, 0, 8)) * side;
            int x = (int) Math.round(c[0] - dz / len * off), z = (int) Math.round(c[1] + dx / len * off);
            boolean near = false;
            for (int[] p : placed) near |= Math.abs(p[0] - x) + Math.abs(p[1] - z) < 3;
            TreeMap<Integer, String> col = Surface.column(dir, x, z);
            int y = surface(col);
            if (near || y == Integer.MIN_VALUE) continue;
            double r = hash(k, 0, 9);
            int h = r < 0.55 ? 2 : r < 0.92 ? 3 : 4;
            canvas.fill(x, y + 1, z, x, y + h, z, "oak_log[axis=y]");
            placed.add(new int[] {x, z});
            count++;
        }
        return count;
    }

    // nearest seed of a jittered 2-block grid: blocks of the same clump share a decision
    static long clump(int x, int z) {
        int gx = Math.floorDiv(x, 2), gz = Math.floorDiv(z, 2);
        long best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) {
                int sx = gx + i, sz = gz + j;
                double px = sx * 2 + hash(sx, sz, 4) * 2, pz = sz * 2 + hash(sx, sz, 5) * 2;
                double d = (px - x) * (px - x) + (pz - z) * (pz - z);
                if (d < bestD) { bestD = d; best = ((long) sx << 32) | (sz & 0xFFFFFFFFL); }
            }
        return best;
    }

    /**
     * Whole trees in the way: wood blocks (26-connected) form one tree; a tree goes if any of its wood up to 8 above the
     * ground lies within the corridor plus a block of clearance. Its wood and the leaves closest to it (within 6) are
     * cleared, so trees beside the route keep their full crowns. Exposed root wood at ground level becomes moss.
     * Returns the cleared positions (Canvas keys).
     */
    static Set<Long> clearTrees(Path dir, Canvas canvas, List<double[]> curve, double halfWidth,
                                int minX, int minZ, int maxX, int maxZ) throws Exception {
        int pad = 10, ground = 32;
        Map<Long, Integer> wood = new HashMap<>();          // position -> tree id
        Map<Long, String> groundWood = new HashMap<>();     // root wood on the ground layer: covered, not cleared
        List<long[]> leaves = new ArrayList<>();
        for (int x = minX - pad; x <= maxX + pad; x++) {
            for (int z = minZ - pad; z <= maxZ + pad; z++) {
                TreeMap<Integer, String> col = Surface.column(dir, x, z);
                for (var e : col.entrySet()) {
                    int y = e.getKey();
                    if (y < ground || y > 90) continue;
                    String name = e.getValue().substring(e.getValue().indexOf(':') + 1);
                    boolean isWood = (name.endsWith("_log") || name.endsWith("_wood")) && !name.startsWith("stripped");
                    if (isWood) {
                        wood.put(Canvas.key(x, y, z), -1);
                        if (y == ground) groundWood.put(Canvas.key(x, y, z), name);
                    } else if (name.endsWith("leaves") && !name.contains("azalea")) leaves.add(new long[] {x, y, z});
                }
            }
        }
        // trees = connected wood
        int trees = 0;
        Map<Integer, Boolean> inTheWay = new HashMap<>();
        for (long start : wood.keySet()) {
            if (wood.get(start) != -1) continue;
            int id = trees++;
            boolean hit = false;
            ArrayDeque<Long> queue = new ArrayDeque<>(List.of(start));
            wood.put(start, id);
            while (!queue.isEmpty()) {
                long k = queue.poll();
                int x = Canvas.x(k), y = Canvas.y(k), z = Canvas.z(k);
                if (y <= ground + 8 && distance(curve, x, z) <= halfWidth * 1.15 + 1) hit = true;
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    long n = Canvas.key(x + dx, y + dy, z + dz);
                    Integer t = wood.get(n);
                    if (t != null && t == -1) { wood.put(n, id); queue.add(n); }
                }
            }
            inTheWay.put(id, hit);
        }
        Set<Long> removed = new HashSet<>();
        for (var e : wood.entrySet()) if (inTheWay.get(e.getValue())) removed.add(e.getKey());
        // leaves belong to the nearest wood within 6 blocks
        int leafCount = 0;
        for (long[] l : leaves) {
            int lx = (int) l[0], ly = (int) l[1], lz = (int) l[2];
            double best = 37;
            int owner = -1;
            for (int dx = -6; dx <= 6; dx++) for (int dy = -6; dy <= 6; dy++) for (int dz = -6; dz <= 6; dz++) {
                double d = dx * dx + dy * dy + dz * dz;
                if (d >= best) continue;
                Integer t = wood.get(Canvas.key(lx + dx, ly + dy, lz + dz));
                if (t != null) { best = d; owner = t; }
            }
            if (owner >= 0 && inTheWay.get(owner)) { removed.add(Canvas.key(lx, ly, lz)); leafCount++; }
        }
        // clear column runs
        Map<Long, TreeSet<Integer>> columns = new HashMap<>();
        for (long k : removed) if (Canvas.y(k) > ground) columns.computeIfAbsent(((long) Canvas.x(k) << 32) | (Canvas.z(k) & 0xFFFFFFFFL), c -> new TreeSet<>()).add(Canvas.y(k));
        for (var e : columns.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey();
            Integer runStart = null, prev = null;
            for (int y : e.getValue()) {
                if (prev != null && y != prev + 1) { canvas.clear(x, runStart, z, x, prev, z, "tree in the way"); runStart = null; }
                if (runStart == null) runStart = y;
                prev = y;
            }
            canvas.clear(x, runStart, z, x, prev, z, "tree in the way");
        }
        for (long k : removed) {
            if (Canvas.y(k) == ground) canvas.swap(Canvas.x(k), ground, Canvas.z(k), groundWood.get(k), "moss_block");
        }
        removed.removeIf(k -> Canvas.y(k) == ground);   // covered with moss, the ground layer stays
        int removedTrees = (int) inTheWay.values().stream().filter(b -> b).count();
        System.err.println("trees: " + trees + " near the route, removing " + removedTrees + " (" + (removed.size() - leafCount)
                + " wood, " + leafCount + " leaves)");
        return removed;
    }

    static double distance(List<double[]> curve, int x, int z) {
        double best = Double.MAX_VALUE;
        for (double[] c : curve) best = Math.min(best, (c[0] - x) * (c[0] - x) + (c[1] - z) * (c[1] - z));
        return Math.sqrt(best);
    }

    // the top natural ground block with air or a plant above, near ground level (ignoring what the build removes)
    static int surface(TreeMap<Integer, String> col) {
        return surface(col, Set.of(), 0, 0);
    }

    static int surface(TreeMap<Integer, String> col, Set<Long> removed, int x, int z) {
        for (int y = 40; y >= 26; y--) {
            String b = col.get(y);
            if (b == null || removed.contains(Canvas.key(x, y, z))) continue;
            String name = b.substring(b.indexOf(':') + 1);
            if (PLANTS.contains(name) || name.endsWith("leaves") || name.equals("vine")) continue;   // a trail runs under canopies
            return SURFACE.contains(name) ? y : Integer.MIN_VALUE;   // leaves, logs, builds, roads: not a trail cell
        }
        return Integer.MIN_VALUE;
    }

    static double cr(double p0, double p1, double p2, double p3, double t) {
        return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    static double hash(int x, int z, int salt) {
        long h = x * 73856093L ^ z * 19349663L ^ salt * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }
}
