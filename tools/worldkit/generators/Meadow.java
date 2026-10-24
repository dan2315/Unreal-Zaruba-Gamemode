import java.nio.file.*;
import java.util.*;

/**
 * Vegetation over open land so fields stop looking bare and the parts of the map blend into each other. On every column
 * whose top is natural grass ground with air above (not a road, a build, water, or an --avoid box):
 *   - grass everywhere (short grass, some tall grass), denser in noisy patches;
 *   - flower patches: blobs from low-frequency noise, one or two kinds per patch (tulips, daisies, cornflowers,
 *     dandelions, poppies, alliums, bluets, and tall lilacs, peonies, rose bushes);
 *   - bushes: small leaf clumps (oak, azalea, flowering azalea, spruce), sparse in the open;
 *   - edges blend: near trees (forest edges) ferns, large ferns, berry bushes and more bushes; along roads a verge of
 *     tall grass and ferns; around buildings tall grass and ferns grown up to the walls.
 * Only plants and bushes are added (into air); nothing is replaced. Columns that already carry a plant are left.
 * Plants go only on blocks that can hold them (grass, dirt, podzol, moss...); bushes (leaves) go anywhere.
 * Usage: java Meadow <region> x0 z0 x1 z1 [--avoid x0 z0 x1 z1]... [--part k n] [--density d]
 */
public class Meadow {
    static final Set<String> GROUND = Set.of("grass_block", "lime_terracotta", "green_concrete_powder", "moss_block", "podzol", "coarse_dirt", "rooted_dirt");
    static final String[][] FLOWERS = {
            {"poppy", "cornflower"}, {"dandelion", "oxeye_daisy"}, {"azure_bluet", "white_tulip"}, {"allium", "pink_tulip"},
            {"red_tulip", "orange_tulip"}, {"cornflower", "oxeye_daisy"}, {"lilac", "dandelion"}, {"peony", "azure_bluet"},
            {"rose_bush", "poppy"}, {"dandelion", "cornflower"}};
    static final String[] BUSH = {"oak_leaves", "azalea_leaves", "flowering_azalea_leaves", "spruce_leaves", "birch_leaves"};

    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        List<int[]> avoid = new ArrayList<>();
        int part = 0, parts = 1;
        double density = 1;
        for (int i = 5; i < a.length; i++) {
            switch (a[i]) {
                case "--avoid" -> { avoid.add(new int[] {Integer.parseInt(a[i + 1]), Integer.parseInt(a[i + 2]), Integer.parseInt(a[i + 3]), Integer.parseInt(a[i + 4])}); i += 4; }
                case "--part" -> { part = Integer.parseInt(a[i + 1]); parts = Integer.parseInt(a[i + 2]); i += 2; }
                case "--density" -> { density = Double.parseDouble(a[i + 1]); i += 1; }
                default -> throw new IllegalArgumentException(a[i]);
            }
        }
        if (parts > 1) { // a stripe along x, with the margin read but not drawn
            int span = (x1 - x0 + parts) / parts, lo = x0 + part * span;
            x1 = Math.min(x1, lo + span - 1);
            x0 = lo;
        }
        int M = 8, w = x1 - x0 + 1 + 2 * M, d = z1 - z0 + 1 + 2 * M;
        int[][] g = new int[w][d];
        byte[][] kind = new byte[w][d]; // 0 open ground, 1 road/build/water, 2 tree (trunk or canopy over it), 3 has plant
        String[][] top = new String[w][d];
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            int x = x0 - M + i, z = z0 - M + j, gy = Integer.MIN_VALUE;
            byte k = 0;
            boolean canopy = false;
            for (int y = 90; y >= 16; y--) {
                String s = r.state(x, y, z);
                if (Region.isAir(s)) continue;
                String n = Region.name(s).replace("minecraft:", "");
                if (n.endsWith("leaves")) { canopy = true; continue; }
                if (BorderHills.plant(n)) { k = 3; continue; }
                if (Tidy.log(n)) { k = 2; continue; }
                if (n.equals("water") || !Export.natural(s)) { k = 1; gy = y; break; }
                gy = y;
                top[i][j] = n;
                break;
            }
            if (canopy && k == 0) k = 2;
            g[i][j] = gy;
            kind[i][j] = gy == Integer.MIN_VALUE ? 1 : k;
        }
        // roads: road-like tops in many of the 5x5 around
        boolean[][] road = new boolean[w][d];
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            int count = 0;
            for (int di = -2; di <= 2; di++) for (int dj = -2; dj <= 2; dj++) {
                int p = i + di, q = j + dj;
                if (p >= 0 && q >= 0 && p < w && q < d && top[p][q] != null && Forest.ROADLIKE.contains(top[p][q])) count++;
            }
            road[i][j] = count >= 8;
        }
        int[][] dBuilt = bfs(w, d, (i, j) -> kind[i][j] == 1 && !road[i][j], 12);
        int[][] dRoad = bfs(w, d, (i, j) -> road[i][j], 12);
        int[][] dTree = bfs(w, d, (i, j) -> kind[i][j] == 2, 12);

        Canvas c = new Canvas(0, 0);
        Map<String, Integer> made = new TreeMap<>();
        for (int i = M; i < w - M; i++) for (int j = M; j < d - M; j++) {
            int x = x0 - M + i, z = z0 - M + j, gy = g[i][j];
            if (kind[i][j] != 0 || road[i][j] || top[i][j] == null || !GROUND.contains(top[i][j])) continue;
            boolean skip = false;
            for (int[] b : avoid) skip |= x >= Math.min(b[0], b[2]) && x <= Math.max(b[0], b[2]) && z >= Math.min(b[1], b[3]) && z <= Math.max(b[1], b[3]);
            if (skip || dBuilt[i][j] < 1) continue;
            double p = BorderHills.hash(x * 7 + 11, z * 13 + 5);
            if (p > density) continue;
            p /= density;
            boolean edge = dTree[i][j] >= 1 && dTree[i][j] <= 7, verge = dRoad[i][j] >= 2 && dRoad[i][j] <= 4, yard = dBuilt[i][j] <= 4;
            double patch = BorderHills.valueNoise(x / 14.0, z / 14.0, 21), lush = BorderHills.valueNoise(x / 9.0, z / 9.0, 22);
            String plant = null;
            boolean tall = false;
            // bushes: forest edges, sometimes in the open (never on road verges)
            double bushChance = edge ? 0.035 * (8 - dTree[i][j]) / 7.0 : 0.0018;
            if (!verge && dRoad[i][j] > 4 && dBuilt[i][j] > 3 && BorderHills.hash(x * 3 + 1, z * 29 + 7) < bushChance) {
                bush(c, x, gy, z, BorderHills.hash(x, z * 31));
                made.merge("bush", 1, Integer::sum);
                continue;
            }
            if (patch > 0.68 && !verge) {
                String[] kinds = FLOWERS[(int) (BorderHills.valueNoise(x / 40.0, z / 40.0, 23) * FLOWERS.length) % FLOWERS.length];
                if (p < 0.4) { plant = kinds[p < 0.24 ? 0 : 1]; tall = plant.equals("lilac") || plant.equals("peony") || plant.equals("rose_bush"); }
            }
            if (plant == null && (edge || yard)) {
                double e = p;
                if (e < 0.14) plant = "fern";
                else if (e < 0.2) { plant = "large_fern"; tall = true; }
                else if (e < 0.23 && edge && dTree[i][j] <= 3) plant = "sweet_berry_bush[age=3]";
                else if (e < 0.3) { plant = "tall_grass"; tall = true; }
            }
            if (plant == null && verge) {
                if (p < 0.22) { plant = "tall_grass"; tall = true; }
                else if (p < 0.3) plant = "fern";
            }
            if (plant == null) {
                double grass = 0.16 + 0.3 * lush;
                if (p < grass * 0.12) { plant = "tall_grass"; tall = true; }
                else if (p < grass) plant = "short_grass";
                else if (p < grass + 0.006) plant = new String[] {"dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet"}[(int) (BorderHills.hash(x, z * 5) * 5)];
            }
            if (plant == null) continue;
            if (!BorderHills.holdsPlants(top[i][j])) continue; // painted grass (terracotta, concrete powder) cannot hold plants: they pop off as items
            if (tall) {
                c.set(x, gy + 1, z, plant + "[half=lower]");
                c.set(x, gy + 2, z, plant + "[half=upper]");
            } else c.set(x, gy + 1, z, plant);
            made.merge(plant.replaceAll("\\[.*", ""), 1, Integer::sum);
        }
        System.err.println("meadow " + x0 + ".." + x1 + ": " + made);
        c.print(new String[0]);
    }

    /** A small bush of persistent leaves on the ground at (x, gy+1, z): 1x1 to 2x2, sometimes 2 high. */
    static void bush(Canvas c, int x, int gy, int z, double h) {
        String leaf = BUSH[(int) (h * BUSH.length)] + "[distance=1,persistent=true,waterlogged=false]";
        c.set(x, gy + 1, z, leaf);
        if (h > 0.3) c.set(x + 1, gy + 1, z, leaf);
        if (h > 0.55) c.set(x, gy + 1, z + 1, leaf);
        if (h > 0.75) { c.set(x + 1, gy + 1, z + 1, leaf); c.set(x, gy + 2, z, leaf); }
    }

    interface Cell { boolean test(int i, int j); }

    static int[][] bfs(int w, int d, Cell src, int cap) {
        int[][] dist = new int[w][d];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            dist[i][j] = src.test(i, j) ? 0 : cap;
            if (dist[i][j] == 0) q.add(new int[] {i, j});
        }
        while (!q.isEmpty()) {
            int[] e = q.poll();
            int nd = dist[e[0]][e[1]] + 1;
            if (nd >= cap) continue;
            for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int p = e[0] + dl[0], s = e[1] + dl[1];
                if (p < 0 || s < 0 || p >= w || s >= d || dist[p][s] <= nd) continue;
                dist[p][s] = nd;
                q.add(new int[] {p, s});
            }
        }
        return dist;
    }
}
