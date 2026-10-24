import java.nio.file.*;
import java.util.*;

/**
 * A forest with its own relief in a box of open land: low hills, small swamps, clear corridors for vehicle tracks, trees
 * from a blueprint folder, and a forest floor. Additive like every build: earth is piled onto the ground, swamps and the
 * forest floor replace only natural surface blocks (region-aware swaps), trees go only into air and plants; the only
 * clears are plants that would float over swamp water.
 *   - Everything keeps away from what is not nature: built blocks, roads (columns where many of the 5x5 tops around are
 *     road blocks), water, old tree trunks and --avoid boxes. Hills fade out near them, near swamps and near tracks.
 *   - --hill x z r h: a smooth rise up to h high, radius r with a wobbling outline and a lumpy top; stone on steep sides,
 *     a few mossy boulders on top.
 *   - --swamp x z r: a pool flush with the ground (water at ground level, deeper in the middle, mud and clay bottom), a
 *     muddy, mossy ring with reeds, tall grass and ferns, lily pads, a few dead trunks standing in it. Flat ground only.
 *   - --track hw x z x z ...: a corridor of half width hw along the curve (same curve as Pathway) kept free of trees and
 *     hills. Lay the track surface afterwards with Pathway using the same points.
 *   - Trees: a jittered grid (spacing 6) thinned by a forest density: a wandering edge 30 blocks deep inside the box,
 *     clearings from noise, fewer near swamps. Tall kinds in the core, small ones at edges and clearings. A tree is only
 *     planted where all its wood and nearly all its leaves fit into air (old trees around keep their crowns).
 *   - Floor: podzol, moss, coarse dirt patches under the trees, ferns, large ferns, berry bushes, mushrooms in the shade;
 *     grass and a few flowers in clearings.
 * Usage: java Forest <region> <trees dir> x0 z0 x1 z1 [--track hw x z ...]... [--hill x z r h]... [--swamp x z r]...
 *        [--avoid x0 z0 x1 z1]... [--part k n]   (part: only the k-th of n stripes along x, for the BuildKit size limit)
 */
public class Forest {
    static final int PAD = 16;
    static final int BUILT = 1, WATER = 2, TRUNK = 4, ROAD = 8, AVOID = 16;
    static final Set<String> ROADLIKE = Set.of("coarse_dirt", "rooted_dirt", "dirt_path", "gravel", "cobbled_deepslate", "deepslate",
            "cyan_terracotta", "cobblestone", "andesite", "tuff", "podzol", "mossy_cobblestone", "farmland", "sand");
    static final Set<String> GRASS = Set.of("grass_block", "lime_terracotta", "green_concrete_powder", "moss_block");

    static Region r;
    static int bx0, bz0, bx1, bz1, gx0, gz0, W, D;
    static int[][] ground, add, swamp;
    static int[][] flags;
    static double[][] dBlocked, dWater, dTrack, dSwamp;
    static Canvas c = new Canvas(0, 0);
    static Set<Long> treeBlocks = new HashSet<>();

    record Track(double hw, List<double[]> curve) {}

    public static void main(String[] a) throws Exception {
        r = new Region(Path.of(a[0]));
        Path treesDir = Path.of(a[1]);
        bx0 = Integer.parseInt(a[2]); bz0 = Integer.parseInt(a[3]); bx1 = Integer.parseInt(a[4]); bz1 = Integer.parseInt(a[5]);
        List<Track> tracks = new ArrayList<>();
        List<double[]> hills = new ArrayList<>(), swamps = new ArrayList<>(), avoids = new ArrayList<>();
        int part = 0, parts = 1;
        for (int i = 6; i < a.length; ) {
            String opt = a[i++];
            List<Double> v = new ArrayList<>();
            while (i < a.length && !a[i].startsWith("--")) v.add(Double.parseDouble(a[i++]));
            switch (opt) {
                case "--track" -> {
                    double[][] pts = new double[(v.size() - 1) / 2][];
                    for (int k = 0; k < pts.length; k++) pts[k] = new double[] {v.get(1 + 2 * k), v.get(2 + 2 * k)};
                    tracks.add(new Track(v.get(0), curve(pts)));
                }
                case "--hill" -> hills.add(new double[] {v.get(0), v.get(1), v.get(2), v.get(3)});
                case "--swamp" -> swamps.add(new double[] {v.get(0), v.get(1), v.get(2)});
                case "--avoid" -> avoids.add(new double[] {v.get(0), v.get(1), v.get(2), v.get(3)});
                case "--part" -> { part = v.get(0).intValue(); parts = v.get(1).intValue(); }
                default -> throw new IllegalArgumentException(opt);
            }
        }
        c.region = r;
        gx0 = bx0 - PAD; gz0 = bz0 - PAD; W = bx1 - bx0 + 1 + 2 * PAD; D = bz1 - bz0 + 1 + 2 * PAD;
        readColumns(avoids);
        boolean[][] blocked = new boolean[W][D], water = new boolean[W][D], trunk = new boolean[W][D];
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            blocked[i][j] = (flags[i][j] & (BUILT | WATER | ROAD | AVOID)) != 0 || ground[i][j] == Integer.MIN_VALUE;
            water[i][j] = (flags[i][j] & WATER) != 0;
            trunk[i][j] = (flags[i][j] & TRUNK) != 0;
        }
        dBlocked = distance(blocked, 40);
        dWater = distance(water, 40);
        double[][] dTrunk = distance(trunk, 12);
        dTrack = trackDistance(tracks);

        int swampCells = swamps(swamps);
        boolean[][] isSwamp = new boolean[W][D];
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) isSwamp[i][j] = swamp[i][j] > 0;
        dSwamp = distance(isSwamp, 30);
        long earth = hills(hills);
        int[] t = trees(treesDir, dTrunk);
        int floor = floor();
        System.err.println("forest: swamp cells " + swampCells + ", hill earth " + earth + ", trees " + t[0] + " (skipped " + t[1]
                + " spots that did not fit), floor changes " + floor);
        if (parts > 1) {
            int span = (W + parts - 1) / parts, lo = gx0 + part * span, hi = lo + span - 1;
            c.cells.keySet().removeIf(k -> Canvas.x(k) < lo || Canvas.x(k) > hi);
            c.clears.removeIf(cl -> cl.x0() < lo || cl.x0() > hi);
        }
        c.print(new String[0]);
    }

    // ---------- reading the site

    static void readColumns(List<double[]> avoids) throws Exception {
        ground = new int[W][D];
        flags = new int[W][D];
        String[][] top = new String[W][D];
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            int x = gx0 + i, z = gz0 + j;
            int g = Integer.MIN_VALUE, f = 0;
            for (int y = 80; y >= 16; y--) {
                String s = r.state(x, y, z);
                if (Region.isAir(s)) continue;
                String n = n(s);
                if (n.equals("water") || n.equals("lava")) { f |= WATER; g = y; break; }
                if (n.endsWith("leaves") || BorderHills.plant(n) || Tidy.log(n)) continue;
                if (!Export.natural(s)) { f |= BUILT; g = y; break; }
                g = y;
                break;
            }
            if (g != Integer.MIN_VALUE && (f & (BUILT | WATER)) == 0) {
                if (Tidy.log(n(r.state(x, g + 1, z)))) f |= TRUNK;
                top[i][j] = n(r.state(x, g, z));
            }
            for (double[] b : avoids)
                if (x >= Math.min(b[0], b[2]) && x <= Math.max(b[0], b[2]) && z >= Math.min(b[1], b[3]) && z <= Math.max(b[1], b[3])) f |= AVOID;
            ground[i][j] = g;
            flags[i][j] = f;
        }
        // roads: many road-like tops in the 5x5 around
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            int count = 0;
            for (int di = -2; di <= 2; di++) for (int dj = -2; dj <= 2; dj++) {
                int p = i + di, q = j + dj;
                if (p >= 0 && q >= 0 && p < W && q < D && top[p][q] != null && ROADLIKE.contains(top[p][q])) count++;
            }
            if (count >= 8) flags[i][j] |= ROAD;
        }
    }

    // ---------- swamps

    static int swamps(List<double[]> list) throws Exception {
        swamp = new int[W][D]; // 0 none, 1 ring, 2 water, 3 deep
        int seed = 40;
        for (double[] s : list) {
            seed++;
            double sx = s[0], sz = s[1], rad = s[2];
            for (int x = (int) (sx - rad * 1.4); x <= sx + rad * 1.4; x++) for (int z = (int) (sz - rad * 1.4); z <= sz + rad * 1.4; z++) {
                int i = x - gx0, j = z - gz0;
                if (!in(i, j)) continue;
                double reff = rad * (0.7 + 0.6 * BorderHills.valueNoise(x / 7.0, z / 7.0, seed));
                double u = Math.hypot(x - sx, z - sz) / reff;
                if (u >= 1) continue;
                if (dBlocked[i][j] < 3 || dTrack[i][j] < 6 || (flags[i][j] & TRUNK) != 0) continue;
                int cls = u < 0.4 ? 3 : u < 0.75 ? 2 : 1;
                swamp[i][j] = Math.max(swamp[i][j], cls);
            }
        }
        // water only where every neighbour is swamp or flat ground at the same level (it must not run off)
        for (int pass = 0; pass < 3; pass++)
            for (int i = 1; i < W - 1; i++) for (int j = 1; j < D - 1; j++) {
                if (swamp[i][j] < 2) continue;
                for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int p = i + dl[0], q = j + dl[1];
                    boolean ok = ground[p][q] == ground[i][j] && (flags[p][q] & (BUILT | WATER | ROAD)) == 0 || swamp[p][q] >= 2;
                    if (!ok) swamp[i][j] = 1;
                    if (swamp[i][j] == 3 && swamp[p][q] < 2) swamp[i][j] = 2;
                }
            }
        int cells = 0;
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            int cls = swamp[i][j];
            if (cls == 0) continue;
            cells++;
            int x = gx0 + i, z = gz0 + j, g = ground[i][j];
            double h = BorderHills.hash(x * 3 + 7, z * 5 + 1);
            String plantAbove = n(r.state(x, g + 1, z));
            if (cls >= 2) {
                if (BorderHills.plant(plantAbove)) c.clear(x, g + 1, z, x, g + 2, z, "plant over swamp water");
                c.ground(x, g, z, x, g, z, cls == 3 && h < 0.2 ? "seagrass" : "water");
                if (cls == 3) {
                    c.ground(x, g - 1, z, x, g - 1, z, h < 0.35 ? "seagrass" : "water");
                    c.ground(x, g - 2, z, x, g - 2, z, "mud");
                } else c.ground(x, g - 1, z, x, g - 1, z, h < 0.7 ? "mud" : "clay");
                if (cls == 2 && BorderHills.hash(x * 11, z * 13) < 0.16) c.set(x, g + 1, z, "lily_pad");
            } else {
                String b = h < 0.35 ? "mud" : h < 0.6 ? "moss_block" : h < 0.75 ? "coarse_dirt" : h < 0.85 ? "podzol" : "grass_block";
                c.ground(x, g, z, x, g, z, b);
                double p = BorderHills.hash(x * 7 + 2, z * 3 + 9);
                boolean byWater = false;
                for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (in(i + dl[0], j + dl[1]) && swamp[i + dl[0]][j + dl[1]] >= 2) byWater = true;
                if (byWater && p < 0.18 && !b.equals("mud")) {
                    int tall = 1 + (int) (BorderHills.hash(x, z * 7) * 3);
                    for (int y = 1; y <= tall; y++) c.set(x, g + y, z, "sugar_cane");
                } else if (p < 0.32) { c.set(x, g + 1, z, "tall_grass[half=lower]"); c.set(x, g + 2, z, "tall_grass[half=upper]"); }
                else if (p < 0.42) { c.set(x, g + 1, z, "large_fern[half=lower]"); c.set(x, g + 2, z, "large_fern[half=upper]"); }
                else if (p < 0.6) c.set(x, g + 1, z, "fern");
                else if (p < 0.72) c.set(x, g + 1, z, "short_grass");
            }
        }
        // dead trunks standing in the swamps
        for (double[] s : list) {
            for (int k = 0; k < 4; k++) {
                double ang = BorderHills.hash((int) s[0] + k * 17, (int) s[1]) * Math.PI * 2, dist = s[2] * (0.3 + 0.5 * BorderHills.hash(k * 31, (int) s[1]));
                int x = (int) Math.round(s[0] + Math.cos(ang) * dist), z = (int) Math.round(s[1] + Math.sin(ang) * dist), i = x - gx0, j = z - gz0;
                if (!in(i, j) || swamp[i][j] == 0) continue;
                int g = ground[i][j], tall = 3 + (int) (BorderHills.hash(x * 5, z * 3) * 4);
                for (int y = 0; y <= tall; y++) c.ground(x, g + y, z, x, g + y, z, "dark_oak_log[axis=y]");
                if (tall >= 5) c.set(x + 1, g + tall - 1, z, "dark_oak_log[axis=x]");
                treeBlocks.add(Canvas.key(x, g + 1, z));
            }
        }
        return cells;
    }

    // ---------- hills

    static long hills(List<double[]> list) throws Exception {
        double[][] h = new double[W][D];
        int seed = 60;
        for (double[] hl : list) {
            seed++;
            double hx = hl[0], hz = hl[1], rad = hl[2], height = hl[3];
            for (int x = (int) (hx - rad * 1.5); x <= hx + rad * 1.5; x++) for (int z = (int) (hz - rad * 1.5); z <= hz + rad * 1.5; z++) {
                int i = x - gx0, j = z - gz0;
                if (!in(i, j)) continue;
                double reff = rad * (0.75 + 0.5 * BorderHills.valueNoise(x / 14.0, z / 14.0, seed));
                double u = Math.hypot(x - hx, z - hz) / reff;
                if (u >= 1) continue;
                double bump = (1 - u * u) * (1 - u * u);
                bump *= 0.8 + 0.4 * BorderHills.valueNoise(x / 6.0, z / 6.0, seed + 100);
                h[i][j] += height * bump;
            }
        }
        add = new int[W][D];
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            if (h[i][j] <= 0 || swamp[i][j] > 0 || (flags[i][j] & (BUILT | WATER | ROAD | AVOID | TRUNK)) != 0) continue;
            double f = fade(dBlocked[i][j] - 2, 10) * fade(dTrack[i][j] - 3, 10) * fade(dSwamp[i][j] - 2, 8) * fade(dWater[i][j] - 8, 10);
            add[i][j] = (int) Math.round(h[i][j] * f);
        }
        long blocks = 0;
        for (int i = 1; i < W - 1; i++) for (int j = 1; j < D - 1; j++) {
            int hgt = add[i][j];
            if (hgt <= 0) continue;
            int x = gx0 + i, z = gz0 + j, g = ground[i][j], top = g + hgt;
            int steep = 0;
            for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int p = i + dl[0], q = j + dl[1];
                steep = Math.max(steep, top - (ground[p][q] == Integer.MIN_VALUE ? top : ground[p][q] + add[p][q]));
            }
            for (int y = g + 1; y < top; y++) c.set(x, y, z, top - y <= 3 ? "dirt" : "stone");
            double n = BorderHills.hash(x, z);
            String surface = steep >= 3 ? (n < 0.35 ? "stone" : n < 0.6 ? "andesite" : n < 0.8 ? "mossy_cobblestone" : "tuff") : "grass_block";
            c.set(x, top, z, surface);
            if (BorderHills.plant(n(r.state(x, top, z))) || BorderHills.plant(n(r.state(x, top + 1, z))))
                c.clear(x, top + 1, z, x, top + 1, z, "plant left floating by a raised hill");
            blocks += hgt;
            // a few mossy boulders on the upper parts
            if (hgt >= 4 && steep < 3 && BorderHills.hash(x * 17 + 5, z * 19) < 0.012) {
                c.set(x, top + 1, z, "mossy_cobblestone");
                if (BorderHills.hash(x, z * 29) < 0.5) c.set(x + 1, top + 1, z, "cobblestone");
                if (BorderHills.hash(x * 23, z) < 0.3) c.set(x, top + 2, z, "mossy_cobblestone");
            }
        }
        return blocks;
    }

    // ---------- trees

    record Kind(Blueprint bp, int height, int radius) {}

    static int[] trees(Path dir, double[][] dTrunk) throws Exception {
        List<Kind> kinds = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                if (!p.toString().endsWith(".bp")) continue;
                Blueprint bp = Blueprint.load(p);
                kinds.add(new Kind(bp, bp.h, (Math.max(bp.w, bp.d) + 1) / 2));
            }
        }
        kinds.sort(Comparator.comparing(k -> k.bp().name));
        int planted = 0, skipped = 0, spacing = 6;
        for (int cz = bz0; cz <= bz1; cz += spacing) for (int cx = bx0; cx <= bx1; cx += spacing) {
            int x = cx + (int) (BorderHills.hash(cx, cz * 3) * spacing * 0.8), z = cz + (int) (BorderHills.hash(cx * 5, cz) * spacing * 0.8);
            if (x > bx1 || z > bz1) continue;
            int i = x - gx0, j = z - gz0;
            double f = forestness(x, z);
            if (swamp[i][j] >= 2) continue;
            if (swamp[i][j] == 1 || dSwamp[i][j] < 6) f *= 0.35;
            if (f < 0.12 || BorderHills.hash(x * 7 + 3, z * 3 + 7) > f * 0.85) continue;
            if (dBlocked[i][j] < 4 || dWater[i][j] < 10 || dTrunk[i][j] < 5) continue;
            // size: tall in the core, small at edges, clearings and swamps
            double core = f, pick = BorderHills.hash(x * 13, z * 17);
            List<Kind> pool = new ArrayList<>();
            List<Double> wts = new ArrayList<>();
            for (Kind k : kinds) {
                double wgt = k.height() >= 15 ? 0.2 + 1.6 * core * core : k.height() >= 9 ? 1.0 : 1.3 - core;
                if (k.height() <= 7) wgt *= 0.5; // low, wide spruce shrubs: undergrowth
                pool.add(k);
                wts.add(Math.max(0.05, wgt));
            }
            double total = wts.stream().mapToDouble(Double::doubleValue).sum(), v = pick * total;
            int idx = 0;
            while (idx < pool.size() - 1 && (v -= wts.get(idx)) > 0) idx++;
            Kind k = pool.get(idx);
            if (dTrack[i][j] < k.radius() + 1) continue;
            Blueprint bp = k.bp().rotated((int) (BorderHills.hash(x * 19, z * 23) * 4));
            if (stamp(bp, x, z)) planted++;
            else skipped++;
        }
        return new int[] {planted, skipped};
    }

    /**
     * Puts a tree with its trunk at (x, z): the trunk's lowest layer stands on the ground there, which must be level
     * under the whole trunk (a hillside step under a wide trunk would leave it hanging). All its wood and 97% of its
     * leaves must fit.
     */
    static boolean stamp(Blueprint bp, int x, int z) throws Exception {
        int base = -1;
        List<int[]> trunk = new ArrayList<>();
        for (int y = bp.ground + 1; y < bp.h && base < 0; y++) {
            for (int zz = 0; zz < bp.d; zz++) for (int xx = 0; xx < bp.w; xx++)
                if (bp.at(xx, y, zz) != null && Tidy.log(n(bp.at(xx, y, zz)))) trunk.add(new int[] {xx, zz});
            if (!trunk.isEmpty()) base = y;
        }
        if (base < 0) return false;
        int[] anchor = trunk.get(0);
        for (int[] t : trunk)
            if (Math.hypot(t[0] - bp.w / 2.0, t[1] - bp.d / 2.0) < Math.hypot(anchor[0] - bp.w / 2.0, anchor[1] - bp.d / 2.0)) anchor = t;
        int ox = x - anchor[0], oz = z - anchor[1], gy = Integer.MIN_VALUE;
        for (int[] t : trunk) {
            int i = ox + t[0] - gx0, j = oz + t[1] - gz0;
            if (!in(i, j) || swamp[i][j] >= 2) return false;
            int top = ground[i][j] + add[i][j];
            if (gy != Integer.MIN_VALUE && top != gy) return false;
            gy = top;
        }
        List<long[]> place = new ArrayList<>();
        int leaves = 0, blocked = 0;
        for (int y = 0; y < bp.h; y++) for (int zz = 0; zz < bp.d; zz++) for (int xx = 0; xx < bp.w; xx++) {
            String s = bp.at(xx, y, zz);
            if (s == null) continue;
            int wx = ox + xx, wy = gy + 1 + y - base, wz = oz + zz;
            if (wy <= gy) continue; // roots below the trunk's first layer: the ground stays
            int ci = wx - gx0, cj = wz - gz0;
            if (in(ci, cj) && dTrack[ci][cj] < 0.5) return undo(place); // nothing may hang over a track
            boolean wood = Tidy.log(n(s));
            long key = Canvas.key(wx, wy, wz);
            String world = r.state(wx, wy, wz);
            if (!wood) leaves++;
            if (!Region.isAir(world) && !BorderHills.plant(n(world))) {
                if (wood) return undo(place);
                blocked++;
                continue;
            }
            Canvas.Cell there = c.cells.get(key);
            if (there != null) {
                // crowns of the new trees may interlock; wood may pass through a neighbour's leaves, never through wood or earth
                boolean otherTree = treeBlocks.contains(key), otherWood = otherTree && Tidy.log(n(there.block()));
                if (!otherTree || otherWood) { if (wood) return undo(place); blocked++; continue; }
                if (!wood) continue;
            }
            place.add(new long[] {key, there == null ? 0 : 1});
            if (there != null) displaced.put(key, there);
            c.set(wx, wy, wz, s);
        }
        if (blocked > leaves * 0.03 + 1) return undo(place);
        for (long[] p : place) { treeBlocks.add(p[0]); displaced.remove(p[0]); }
        return true;
    }

    static final Map<Long, Canvas.Cell> displaced = new HashMap<>();

    // a tree that does not fit: take its blocks back (and the neighbour's leaves its trunk replaced)
    static boolean undo(List<long[]> place) {
        for (long[] p : place) {
            Canvas.Cell before = displaced.remove(p[0]);
            if (before != null) c.cells.put(p[0], before);
            else c.cells.remove(p[0]);
        }
        return false;
    }

    static double forestness(int x, int z) {
        double edge = Math.min(Math.min(x - bx0, bx1 - x), Math.min(z - bz0, bz1 - z)) + 24 * (BorderHills.valueNoise(x / 30.0, z / 30.0, 3) - 0.5);
        double e = fade(edge, 30);
        double clearing = fade((BorderHills.valueNoise(x / 38.0, z / 38.0, 4) - 0.22) * 100, 15);
        return e * clearing;
    }

    // ---------- floor

    static int floor() throws Exception {
        boolean[][] trunks = new boolean[W][D];
        for (long k : treeBlocks) {
            int i = Canvas.x(k) - gx0, j = Canvas.z(k) - gz0;
            if (in(i, j) && Canvas.y(k) == ground[i][j] + add[i][j] + 1) trunks[i][j] = true;
        }
        double[][] dTree = distance(trunks, 8);
        int changes = 0;
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            int x = gx0 + i, z = gz0 + j;
            if (x < bx0 || x > bx1 || z < bz0 || z > bz1) continue;
            if (swamp[i][j] > 0 || dBlocked[i][j] < 2 || dTrack[i][j] < 1 || dWater[i][j] < 6) continue;
            int g = ground[i][j], top = g + add[i][j];
            double f = forestness(x, z);
            boolean shade = dTree[i][j] <= 4;
            String cur = add[i][j] > 0 ? Region.name(c.cells.get(Canvas.key(x, top, z)).block()).replace("minecraft:", "") : n(r.state(x, g, z));
            if (!GRASS.contains(cur)) continue;
            double h = BorderHills.hash(x * 3 + 1, z * 7 + 5);
            String b = cur;
            if (shade || f > 0.6) b = h < 0.3 ? "podzol" : h < 0.38 ? "coarse_dirt" : h < 0.58 ? "moss_block" : h < 0.8 ? "grass_block" : cur;
            else if (add[i][j] > 0) b = h < 0.6 ? "grass_block" : h < 0.85 ? "lime_terracotta" : "green_concrete_powder";
            if (!b.equals(cur)) {
                if (add[i][j] > 0) c.set(x, top, z, b);
                else c.swap(x, top, z, "minecraft:" + cur, b);
                changes++;
            }
            long above = Canvas.key(x, top + 1, z);
            if (c.cells.containsKey(above)) continue;
            String w = r.state(x, top + 1, z);
            if (!Region.isAir(w) && !BorderHills.plant(n(w))) continue;
            double p = BorderHills.hash(x * 5 + 3, z * 11 + 2);
            if (shade || f > 0.6) {
                if (p < 0.13) c.set(x, top + 1, z, "fern");
                else if (p < 0.17 && Region.isAir(r.state(x, top + 2, z))) { c.set(x, top + 1, z, "large_fern[half=lower]"); c.set(x, top + 2, z, "large_fern[half=upper]"); }
                else if (p < 0.185 && !b.equals("moss_block")) c.set(x, top + 1, z, "sweet_berry_bush[age=3]");
                else if (p < 0.195 && shade) c.set(x, top + 1, z, p < 0.19 ? "brown_mushroom" : "red_mushroom");
                else if (p < 0.27) c.set(x, top + 1, z, "short_grass");
            } else {
                if (p < 0.2) c.set(x, top + 1, z, "short_grass");
                else if (p < 0.23 && Region.isAir(r.state(x, top + 2, z))) { c.set(x, top + 1, z, "tall_grass[half=lower]"); c.set(x, top + 2, z, "tall_grass[half=upper]"); }
                else if (p < 0.245) c.set(x, top + 1, z, new String[] {"dandelion", "poppy", "cornflower", "oxeye_daisy"}[(int) (BorderHills.hash(x, z * 3) * 4)]);
            }
        }
        return changes;
    }

    // ---------- helpers

    static List<double[]> curve(double[][] points) {
        List<double[]> curve = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            double[] p0 = points[Math.max(0, i - 1)], p1 = points[i], p2 = points[i + 1], p3 = points[Math.min(points.length - 1, i + 2)];
            int n = (int) Math.ceil(Math.hypot(p2[0] - p1[0], p2[1] - p1[1]) / 0.5);
            for (int k = 0; k < n; k++) {
                double t = (double) k / n;
                curve.add(new double[] {Pathway.cr(p0[0], p1[0], p2[0], p3[0], t), Pathway.cr(p0[1], p1[1], p2[1], p3[1], t)});
            }
        }
        curve.add(points[points.length - 1]);
        return curve;
    }

    /** Distance from each column to the nearest track edge (negative inside a corridor), up to 40 beyond it. */
    static double[][] trackDistance(List<Track> tracks) {
        double[][] d = new double[W][D];
        for (double[] row : d) Arrays.fill(row, 40);
        for (Track t : tracks) {
            int reach = (int) Math.ceil(t.hw() + 40);
            for (double[] p : t.curve()) {
                int pi = (int) Math.round(p[0]) - gx0, pj = (int) Math.round(p[1]) - gz0;
                for (int i = Math.max(0, pi - reach); i <= Math.min(W - 1, pi + reach); i++)
                    for (int j = Math.max(0, pj - reach); j <= Math.min(D - 1, pj + reach); j++) {
                        double v = Math.hypot(gx0 + i - p[0], gz0 + j - p[1]) - t.hw();
                        if (v < d[i][j]) d[i][j] = v;
                    }
            }
        }
        return d;
    }

    /** Grid distance (8-neighbour chamfer) to the nearest marked column, capped. */
    static double[][] distance(boolean[][] src, double cap) {
        double[][] d = new double[W][D];
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) d[i][j] = src[i][j] ? 0 : cap;
        double s2 = Math.sqrt(2);
        for (int i = 0; i < W; i++) for (int j = 0; j < D; j++) {
            if (i > 0) d[i][j] = Math.min(d[i][j], d[i - 1][j] + 1);
            if (j > 0) d[i][j] = Math.min(d[i][j], d[i][j - 1] + 1);
            if (i > 0 && j > 0) d[i][j] = Math.min(d[i][j], d[i - 1][j - 1] + s2);
            if (i > 0 && j < D - 1) d[i][j] = Math.min(d[i][j], d[i - 1][j + 1] + s2);
        }
        for (int i = W - 1; i >= 0; i--) for (int j = D - 1; j >= 0; j--) {
            if (i < W - 1) d[i][j] = Math.min(d[i][j], d[i + 1][j] + 1);
            if (j < D - 1) d[i][j] = Math.min(d[i][j], d[i][j + 1] + 1);
            if (i < W - 1 && j < D - 1) d[i][j] = Math.min(d[i][j], d[i + 1][j + 1] + s2);
            if (i < W - 1 && j > 0) d[i][j] = Math.min(d[i][j], d[i + 1][j - 1] + s2);
        }
        return d;
    }

    static double fade(double v, double over) {
        double t = Math.max(0, Math.min(1, v / over));
        return t * t * (3 - 2 * t);
    }

    static boolean in(int i, int j) { return i >= 0 && j >= 0 && i < W && j < D; }

    static String n(String s) { return Region.name(s).replace("minecraft:", ""); }
}
