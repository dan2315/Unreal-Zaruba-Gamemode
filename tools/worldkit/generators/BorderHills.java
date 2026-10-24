import java.nio.file.*;
import java.util.*;

/**
 * Hills along the edges of the map, with a forest on them. Additive: earth is only piled onto the existing ground.
 *   - Height: 0 at the inner edge of the band, rising toward the map edge (smoothstep), shaped by two octaves of value
 *     noise so the hills roll; capped at `maxHeight`.
 *   - It keeps away from everything that is not nature: built blocks, roads, water, and tree trunks: within `margin`
 *     blocks of them the hill fades to nothing, so roads and buildings stay as they are and old trees keep their feet.
 *   - Surface like the map's own grass (grass block with lime terracotta / green concrete powder / moss patches), stone
 *     and gravel on steep slopes, short grass and a few flowers on top.
 *   - Forest: trees from the blueprint library on a jittered grid (`spacing`), on hill ground away from built things,
 *     mostly the map's oak + dark oak leaves kind, some oak, birch and spruce; bushes in between.
 * Usage: java BorderHills <region dir> <trees dir> <sides: any of L R T B> <band width> <max height> <spacing> [exclude x0 z0 x1 z1 ...]
 */
public class BorderHills {
    static final int MAP_X0 = -1024, MAP_X1 = 1023, MAP_Z0 = -512, MAP_Z1 = 511;
    static final int MARGIN = 12;

    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        Path treesDir = Path.of(a[1]);
        String sides = a[2];
        int band = Integer.parseInt(a[3]), maxH = Integer.parseInt(a[4]), spacing = Integer.parseInt(a[5]);
        List<int[]> exclude = new ArrayList<>();
        for (int i = 6; i + 3 < a.length; i += 4)
            exclude.add(new int[] {Integer.parseInt(a[i]), Integer.parseInt(a[i + 1]), Integer.parseInt(a[i + 2]), Integer.parseInt(a[i + 3])});

        int w = MAP_X1 - MAP_X0 + 1, d = MAP_Z1 - MAP_Z0 + 1;
        int[][] ground = new int[d][w];
        int[][] add = new int[d][w];
        byte[][] kind = new byte[d][w]; // 0 not in band, 1 open, 2 blocked (built/water/trunk/excluded)
        // 1. the band: ground height and what blocks it
        for (int zi = MAP_Z0; zi <= MAP_Z1; zi++) for (int xi = MAP_X0; xi <= MAP_X1; xi++) {
            final int x = xi, z = zi;
            int dist = edgeDistance(sides, x, z);
            if (dist >= band + MARGIN) continue;
            int gx = x - MAP_X0, gz = z - MAP_Z0;
            boolean blocked = exclude.stream().anyMatch(e -> x >= Math.min(e[0], e[2]) && x <= Math.max(e[0], e[2]) && z >= Math.min(e[1], e[3]) && z <= Math.max(e[1], e[3]));
            int g = Integer.MIN_VALUE;
            for (int y = 90; y >= 0; y--) {
                String s = r.state(x, y, z);
                if (Region.isAir(s)) continue;
                String n = Region.name(s).replace("minecraft:", "");
                if (n.equals("water") || n.equals("lava")) { blocked = true; break; }
                if (n.endsWith("leaves") || plant(n)) continue;
                if (n.endsWith("_log") || n.endsWith("_wood")) { blocked = true; continue; }
                if (!Export.natural(s)) { blocked = true; continue; }
                g = y;
                break;
            }
            ground[gz][gx] = g;
            kind[gz][gx] = (byte) (g == Integer.MIN_VALUE || blocked ? 2 : 1);
        }
        // 2. distance from blocked columns (BFS, capped)
        int[][] free = new int[d][w];
        for (int[] row : free) Arrays.fill(row, MARGIN);
        Deque<int[]> q = new ArrayDeque<>();
        for (int gz = 0; gz < d; gz++) for (int gx = 0; gx < w; gx++) if (kind[gz][gx] == 2) { free[gz][gx] = 0; q.add(new int[] {gx, gz}); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int nd = free[p[1]][p[0]] + 1;
            if (nd >= MARGIN) continue;
            for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = p[0] + dl[0], nz = p[1] + dl[1];
                if (nx < 0 || nz < 0 || nx >= w || nz >= d || free[nz][nx] <= nd) continue;
                free[nz][nx] = nd;
                q.add(new int[] {nx, nz});
            }
        }
        // 3. heights
        for (int gz = 0; gz < d; gz++) for (int gx = 0; gx < w; gx++) {
            if (kind[gz][gx] != 1) continue;
            int x = gx + MAP_X0, z = gz + MAP_Z0;
            // the inner edge of the hills wanders (no straight contour lines parallel to the map edge)
            double wander = (valueNoise(x / 70.0, z / 70.0, 5) - 0.5) * band * 0.6 + (valueNoise(x / 23.0, z / 23.0, 6) - 0.5) * band * 0.15;
            double t = 1 - (edgeDistance(sides, x, z) + wander) / (double) band;
            if (t <= 0) continue;
            double f = t * t * (3 - 2 * t);
            double n = 0.65 * valueNoise(x / 48.0, z / 48.0, 1) + 0.35 * valueNoise(x / 17.0, z / 17.0, 2);
            double fade = Math.min(1, free[gz][gx] / (double) MARGIN);
            fade = fade * fade * (3 - 2 * fade);
            add[gz][gx] = (int) Math.round(maxH * f * (0.3 + 0.7 * n) * fade);
        }
        // 4. earth, surface, plants
        Canvas c = new Canvas(0, 0);
        long blocks = 0;
        for (int gz = 0; gz < d; gz++) for (int gx = 0; gx < w; gx++) {
            int h = add[gz][gx];
            if (h <= 0) continue;
            int x = gx + MAP_X0, z = gz + MAP_Z0, g = ground[gz][gx], top = g + h;
            int steep = 0;
            for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = gx + dl[0], nz = gz + dl[1];
                if (nx < 0 || nz < 0 || nx >= w || nz >= d || kind[nz][nx] == 0) continue;
                int other = ground[nz][nx] == Integer.MIN_VALUE ? top : ground[nz][nx] + Math.max(0, add[nz][nx]);
                steep = Math.max(steep, top - other);
            }
            for (int y = g + 1; y < top; y++) c.set(x, y, z, top - y <= 3 ? "dirt" : "stone");
            double n = hash(x, z);
            String surface = steep >= 3 ? (n < 0.4 ? "stone" : n < 0.7 ? "andesite" : n < 0.85 ? "tuff" : "gravel")
                    : n < 0.6 ? "grass_block" : n < 0.85 ? "lime_terracotta" : n < 0.95 ? "green_concrete_powder" : "moss_block";
            c.set(x, top, z, surface);
            blocks += h;
            if (steep < 3) {
                double p = hash(x * 7 + 3, z * 5 + 1);
                if (p < 0.22) c.set(x, top + 1, z, "short_grass");
                else if (p < 0.24) { c.set(x, top + 1, z, "tall_grass[half=lower]"); c.set(x, top + 2, z, "tall_grass[half=upper]"); }
                else if (p < 0.25) c.set(x, top + 1, z, p < 0.245 ? "poppy" : "dandelion");
            }
        }
        // 5. forest on the hills
        List<Blueprint> trees = new ArrayList<>(), bushes = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        try (var files = Files.list(treesDir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".bp") || name.startsWith("copycat")) continue;
                Blueprint bp = Blueprint.load(p);
                if (name.contains("azalea")) { bushes.add(bp); continue; }
                double wgt = name.startsWith("oak_dark_oak") ? 5 : name.startsWith("oak_oak") ? 2.5 : name.startsWith("oak_birch") ? 1.2
                        : name.startsWith("birch") ? 0.8 : 1.0;
                trees.add(bp);
                weights.add(wgt);
            }
        }
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        int planted = 0, bushCount = 0;
        if (spacing > 0) for (int cz = MAP_Z0; cz <= MAP_Z1; cz += spacing) for (int cx = MAP_X0; cx <= MAP_X1; cx += spacing) {
            int x = cx + (int) (hash(cx, cz * 3) * spacing * 0.8), z = cz + (int) (hash(cx * 5, cz) * spacing * 0.8);
            if (x > MAP_X1 || z > MAP_Z1) continue;
            int gx = x - MAP_X0, gz = z - MAP_Z0;
            if (kind[gz][gx] != 1 || add[gz][gx] < 2 || free[gz][gx] < 8) continue;
            if (valueNoise(x / 45.0, z / 45.0, 9) < 0.28) continue; // clearings
            int top = ground[gz][gx] + add[gz][gx];
            double pick = hash(x * 13, z * 17);
            Blueprint bp;
            if (!bushes.isEmpty() && pick < 0.12) { bp = bushes.get((int) (hash(x, z * 9) * bushes.size())); bushCount++; }
            else {
                double v = hash(x * 3 + 1, z * 11 + 7) * total;
                int i = 0;
                while (i < trees.size() - 1 && (v -= weights.get(i)) > 0) i++;
                bp = trees.get(i);
                planted++;
            }
            Blueprint turned = bp.rotated((int) (hash(x * 19, z * 23) * 4));
            int ox = x - turned.w / 2, oz = z - turned.d / 2;
            for (int y = 0; y < turned.h; y++) for (int zz = 0; zz < turned.d; zz++) for (int xx = 0; xx < turned.w; xx++) {
                String s = turned.at(xx, y, zz);
                if (s != null) c.set(ox + xx, top - turned.ground + y, oz + zz, s);
            }
        }
        System.err.println("hills: " + blocks + " blocks of earth; trees " + planted + ", bushes " + bushCount);
        c.print(new String[0]);
    }

    static boolean plant(String n) {
        return n.contains("grass") && !n.equals("grass_block") || n.contains("fern") || n.contains("flower") || n.contains("tulip")
                || n.equals("poppy") || n.equals("dandelion") || n.equals("cornflower") || n.equals("oxeye_daisy") || n.equals("azure_bluet")
                || n.equals("allium") || n.contains("bush") || n.equals("vine") || n.contains("petals") || n.equals("lilac") || n.equals("peony")
                || n.equals("rose_bush") || n.equals("sunflower") || n.equals("snow") || n.contains("carpet") || n.contains("mushroom");
    }

    /** Distance to the nearest enabled map edge. */
    static int edgeDistance(String sides, int x, int z) {
        int dist = Integer.MAX_VALUE;
        if (sides.contains("L")) dist = Math.min(dist, x - MAP_X0);
        if (sides.contains("R")) dist = Math.min(dist, MAP_X1 - x);
        if (sides.contains("T")) dist = Math.min(dist, z - MAP_Z0);
        if (sides.contains("B")) dist = Math.min(dist, MAP_Z1 - z);
        return dist;
    }

    static double hash(int x, int z) {
        long h = x * 73856093L ^ z * 19349663L ^ 0x5DEECE66DL;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return ((h >>> 8) & 0xFFFFFF) / (double) 0x1000000;
    }

    static double valueNoise(double x, double z, int seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash(x0 * 31 + seed, z0), b = hash((x0 + 1) * 31 + seed, z0);
        double c = hash(x0 * 31 + seed, z0 + 1), e = hash((x0 + 1) * 31 + seed, z0 + 1);
        return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + e * fx) * fz;
    }
}
