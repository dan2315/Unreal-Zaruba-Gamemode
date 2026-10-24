import java.nio.file.*;
import java.util.*;

/**
 * Levels what a road, ramp or plot cut left behind around it: cut faces, ditches, ragged one-block steps, half-cut and
 * floating trees. Reference areas are the new surfaces ("x0 z0 x1 z1 y", or "auto" to take the ground as it is now,
 * for a ramp). Natural ground near them is reshaped; built blocks and anything standing on the ground are left alone.
 *   - Cut: ground is lowered to at most refY + slope * distance from the references, so a wall becomes a bank.
 *   - Fill: ground within 2 of a fixed-height reference is raised to its level, within 5 to one below (ditches).
 *   - Smooth: within smoothDist of a fixed reference, two passes of a 3x3 median remove one- and two-block bumps,
 *     pits and ragged edges (slopes and straight edges are kept); columns with a tree trunk stay as they are.
 *   - Trees: a tree (logs connected to each other, with the leaves and vines nearest to them) goes whole when its trunk
 *     loses the ground under it, when the ground cut passes through it, when it stands within treeClear of a fixed
 *     reference, or when it is a bare stump (1-3 upright logs, no leaves). Leaves reaching no log and touching nothing
 *     solid (crowns left in the air by earlier clears) go too. Trees crossing the edge of the area are kept.
 * New tops get the map's grass mix. Lowering and tree removal are clears: apply with `/buildkit apply <name> clear`.
 * Usage: java Tidy <region> x0 z0 x1 z1 slope smoothDist treeClear  ref...   (ref = x0 z0 x1 z1 y|auto)
 */
public class Tidy {
    static final int M = 10, Y0 = 20, Y1 = 130;
    static int ax0, az0, w, h, ex0, ez0, ex1, ez1;
    static Region r;

    public static void main(String[] a) throws Exception {
        r = new Region(Path.of(a[0]));
        ex0 = Integer.parseInt(a[1]); ez0 = Integer.parseInt(a[2]); ex1 = Integer.parseInt(a[3]); ez1 = Integer.parseInt(a[4]);
        double slope = Double.parseDouble(a[5]);
        int smoothDist = Integer.parseInt(a[6]), treeClear = Integer.parseInt(a[7]);
        ax0 = ex0 - M; az0 = ez0 - M; w = ex1 - ex0 + 1 + 2 * M; h = ez1 - ez0 + 1 + 2 * M;

        // ground per column: top block that is not a plant, tree part or water; "terrain" when that block is natural and
        // no water covers it; "floor" when it is a built full block with nothing on it (a ditch lined with road blocks)
        int[][] old = new int[w][h];
        boolean[][] terrain = new boolean[w][h], floor = new boolean[w][h], trunk = new boolean[w][h];
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
            int x = ax0 + i, z = az0 + j;
            int g = r.top(x, z, s -> { String n = n(s); return !member(n) && !BorderHills.plant(n) && !n.equals("water"); });
            old[i][j] = g;
            if (g == Integer.MIN_VALUE) continue;
            String top = n(r.state(x, g, z)), above = n(r.state(x, g + 1, z));
            terrain[i][j] = Export.natural(r.state(x, g, z)) && !above.equals("water");
            floor[i][j] = !terrain[i][j] && !above.equals("water") && !top.matches(".*(slab|stairs|wall|fence|pane|bars|door|lantern|chain|rail|button|plate|torch|sign|ladder|scaffold|catwalk|girder|beam|copycat|glass).*");
            trunk[i][j] = log(above);
        }

        // references: fixed height or the ground as it is
        double[][] refY = new double[w][h];
        boolean[][] ref = new boolean[w][h], fixed = new boolean[w][h];
        for (int k = 8; k + 4 < a.length; k += 5) {
            int rx0 = Integer.parseInt(a[k]), rz0 = Integer.parseInt(a[k + 1]), rx1 = Integer.parseInt(a[k + 2]), rz1 = Integer.parseInt(a[k + 3]);
            boolean auto = a[k + 4].equals("auto");
            for (int x = Math.min(rx0, rx1); x <= Math.max(rx0, rx1); x++) for (int z = Math.min(rz0, rz1); z <= Math.max(rz0, rz1); z++) {
                int i = x - ax0, j = z - az0;
                if (i < 0 || j < 0 || i >= w || j >= h || old[i][j] == Integer.MIN_VALUE) continue;
                ref[i][j] = true;
                fixed[i][j] = !auto;
                refY[i][j] = auto ? old[i][j] : Integer.parseInt(a[k + 4]);
            }
        }

        // cap: lowest refY + slope * path length (8-neighbour Dijkstra); distance to any / to fixed references
        double[][] cap = grow(ref, refY, slope);
        double[][] dist = grow(ref, null, 1), fdist = grow(fixed, null, 1);
        double[][] fy = nearestY(fixed, refY);

        int[][] nh = new int[w][h], hi = new int[w][h], lo = new int[w][h];
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
            nh[i][j] = old[i][j];
            hi[i][j] = Integer.MAX_VALUE;
            lo[i][j] = Integer.MIN_VALUE;
            if (!editable(i, j) || !terrain[i][j] || ref[i][j] || dist[i][j] > 40) continue;
            hi[i][j] = (int) Math.floor(cap[i][j] + 1e-9); // a trunk standing on cut ground goes with its tree
            if (!trunk[i][j] && fdist[i][j] <= 5) lo[i][j] = (int) fy[i][j] - (fdist[i][j] <= 2.01 ? 0 : 1);
            nh[i][j] = Math.max(Math.min(nh[i][j], hi[i][j]), lo[i][j]);
        }
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
            if (editable(i, j) && floor[i][j] && !ref[i][j] && fdist[i][j] <= 2.01 && old[i][j] < fy[i][j]) nh[i][j] = (int) fy[i][j];
        }
        for (int pass = 0; pass < 2; pass++) {
            int[][] next = new int[w][h];
            for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
                next[i][j] = nh[i][j];
                if (!editable(i, j) || !terrain[i][j] || trunk[i][j] || fixed[i][j] || fdist[i][j] > smoothDist) continue;
                List<Integer> v = new ArrayList<>();
                for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                    int p = i + di, q = j + dj;
                    if (p < 0 || q < 0 || p >= w || q >= h) continue;
                    if (terrain[p][q]) v.add(nh[p][q]);
                    else if (fixed[p][q]) v.add((int) refY[p][q]);
                }
                if (v.size() < 5) continue;
                Collections.sort(v);
                int m = v.get(v.size() / 2);
                if (Math.abs(m - nh[i][j]) <= 2) next[i][j] = Math.max(Math.min(m, hi[i][j]), lo[i][j]);
            }
            nh = next;
        }

        Canvas c = new Canvas(0, 0);
        c.region = r;
        int cut = 0, filled = 0;
        boolean[][] lowered = new boolean[w][h];
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
            int x = ax0 + i, z = az0 + j, o = old[i][j], n = nh[i][j];
            if (n == o) continue;
            if (n < o) {
                c.clear(x, n + 1, z, x, o + 2, z, "ground over the bank and bumps by the road");
                lowered[i][j] = true;
                cut++;
            } else {
                for (int y = o + 1; y < n; y++) c.set(x, y, z, "dirt");
                for (int y = n + 1; y <= n + 2; y++) if (BorderHills.plant(n(r.state(x, y, z)))) c.clear(x, y, z, x, y, z, "plant over raised ground");
                filled++;
            }
            double s = BorderHills.hash(x, z);
            String top = s < 0.6 ? "grass_block" : s < 0.85 ? "lime_terracotta" : "green_concrete_powder";
            if (n < o) c.ground(x, n, z, x, n, z, top);
            else c.set(x, n, z, top);
        }
        int[] trees = trees(c, old, nh, lowered, fixed, fdist, treeClear);
        System.err.println("tidy: " + cut + " columns lowered, " + filled + " raised; trees removed " + trees[0] + ", stumps " + trees[1]
                + ", loose leaf clumps " + trees[2]);
        c.print(new String[0]);
    }

    static boolean editable(int i, int j) {
        int x = ax0 + i, z = az0 + j;
        return x >= ex0 && x <= ex1 && z >= ez0 && z <= ez1;
    }

    static String n(String s) { return Region.name(s).replace("minecraft:", ""); }

    static boolean member(String n) {
        return n.endsWith("leaves") || n.equals("vine") || log(n);
    }

    // trunks and branches: the tree library also builds them from fences and copycat walls and beams
    static boolean log(String n) {
        return n.endsWith("_log") || n.endsWith("_wood") || n.endsWith("_fence") || n.equals("copycats:copycat_wall") || n.equals("copycats:copycat_beam");
    }

    // weighted 8-neighbour Dijkstra: value at sources (base, or 0) plus cost * path length
    static double[][] grow(boolean[][] src, double[][] base, double cost) {
        double[][] d = new double[w][h];
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(e -> e[0]));
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
            d[i][j] = src[i][j] ? (base == null ? 0 : base[i][j]) : Double.MAX_VALUE;
            if (src[i][j]) pq.add(new double[] {d[i][j], i, j});
        }
        while (!pq.isEmpty()) {
            double[] e = pq.poll();
            int i = (int) e[1], j = (int) e[2];
            if (e[0] > d[i][j]) continue;
            for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                int p = i + di, q = j + dj;
                if ((di == 0 && dj == 0) || p < 0 || q < 0 || p >= w || q >= h) continue;
                double v = e[0] + cost * (di != 0 && dj != 0 ? Math.sqrt(2) : 1);
                if (v < d[p][q]) { d[p][q] = v; pq.add(new double[] {v, p, q}); }
            }
        }
        return d;
    }

    // the height of the nearest fixed reference cell (breadth-first, 8-neighbour)
    static double[][] nearestY(boolean[][] src, double[][] y) {
        double[][] out = new double[w][h];
        boolean[][] seen = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) if (src[i][j]) { seen[i][j] = true; out[i][j] = y[i][j]; q.add(new int[] {i, j}); }
        while (!q.isEmpty()) {
            int[] e = q.poll();
            for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                int p = e[0] + di, s = e[1] + dj;
                if (p < 0 || s < 0 || p >= w || s >= h || seen[p][s]) continue;
                seen[p][s] = true;
                out[p][s] = out[e[0]][e[1]];
                q.add(new int[] {p, s});
            }
        }
        return out;
    }

    /** Finds trees and loose leaves in the area and clears the ones that would be left broken. {trees, stumps, leaf clumps}. */
    static int[] trees(Canvas c, int[][] old, int[][] nh, boolean[][] lowered, boolean[][] fixed, double[][] fdist, int treeClear) throws Exception {
        int H = Y1 - Y0 + 1;
        byte[] kind = new byte[w * h * H]; // 0 other, 1 log, 2 leaves/vine, 3 solid
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) for (int y = Y0; y <= Y1; y++) {
            String s = r.state(ax0 + i, y, az0 + j);
            String n = n(s);
            byte k = log(n) ? 1 : member(n) ? 2 : Region.isAir(s) || BorderHills.plant(n) || n.equals("water") ? (byte) 0 : 3;
            // ground being cut away is air afterwards
            if (k == 3 && lowered[i][j] && y > nh[i][j] && y <= old[i][j] + 2) k = 0;
            kind[idx(i, y, j)] = k;
        }
        int[] tree = new int[kind.length];
        Arrays.fill(tree, -1);
        List<List<Integer>> logs = new ArrayList<>();
        // log components, 26-connected
        for (int p = 0; p < kind.length; p++) {
            if (kind[p] != 1 || tree[p] >= 0) continue;
            List<Integer> comp = new ArrayList<>();
            ArrayDeque<Integer> q = new ArrayDeque<>(List.of(p));
            tree[p] = logs.size();
            while (!q.isEmpty()) {
                int e = q.poll();
                comp.add(e);
                int i = e / (h * H), j = e / H % h, y = e % H;
                for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dy = -1; dy <= 1; dy++) {
                    int pi = i + di, pj = j + dj, py = y + dy;
                    if (pi < 0 || pj < 0 || py < 0 || pi >= w || pj >= h || py >= H) continue;
                    int f = idx(pi, py + Y0, pj);
                    if (kind[f] == 1 && tree[f] < 0) { tree[f] = logs.size(); q.add(f); }
                }
            }
            logs.add(comp);
        }
        // leaves and vines go to the nearest log (6-connected, up to 7 steps)
        int[] step = new int[kind.length];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (List<Integer> comp : logs) q.addAll(comp);
        List<List<Integer>> leaves = new ArrayList<>();
        for (int t = 0; t < logs.size(); t++) leaves.add(new ArrayList<>());
        while (!q.isEmpty()) {
            int e = q.poll();
            if (step[e] >= 7) continue;
            for (int f : six(e)) {
                if (f < 0 || kind[f] != 2 || tree[f] >= 0) continue;
                tree[f] = tree[e];
                step[f] = step[e] + 1;
                leaves.get(tree[e]).add(f);
                q.add(f);
            }
        }
        int removed = 0, stumps = 0, clumps = 0;
        for (int t = 0; t < logs.size(); t++) {
            List<Integer> lg = logs.get(t);
            boolean edge = false, supported = false, cutThrough = false, nearRoad = false, upright = true;
            for (int e : lg) {
                int i = e / (h * H), j = e / H % h, y = e % H + Y0;
                if (!editable(i, j) || y >= Y1 - 1) edge = true;
                int below = e - 1;
                if (y > Y0 && kind[below] == 3) supported = true;
                if (lowered[i][j] && y <= old[i][j] + 3) cutThrough = true;
                if (fdist[i][j] <= treeClear && !fixed[i][j] && leaves.get(t).size() >= 5) nearRoad = true;
                String s = r.state(ax0 + i, y, az0 + j);
                if (!n(s).endsWith("_log") || !s.contains("axis=y")) upright = false;
            }
            for (int e : leaves.get(t)) {
                int i = e / (h * H), j = e / H % h;
                if (!editable(i, j)) edge = true;
            }
            if (edge) continue;
            boolean stump = upright && lg.size() <= 3 && leaves.get(t).isEmpty() && supported;
            if (supported && !cutThrough && !nearRoad && !stump) continue;
            // road and floor blocks made of wood (stripped wood patches) sit in a surface: only clear free-standing wood
            boolean inSurface = false;
            for (int e : lg) for (int f : six(e)) if (f >= 0 && kind[f] == 3 && f != e - 1) inSurface = true;
            if (inSurface && leaves.get(t).isEmpty()) continue;
            clearAll(c, lg, H, "broken or stray tree by the road");
            clearAll(c, leaves.get(t), H, "broken or stray tree by the road");
            if (stump) stumps++; else removed++;
        }
        // leaves that reached no log: clear the clumps that touch nothing solid
        boolean[] seen = new boolean[kind.length];
        for (int p = 0; p < kind.length; p++) {
            if (kind[p] != 2 || tree[p] >= 0 || seen[p]) continue;
            List<Integer> comp = new ArrayList<>();
            ArrayDeque<Integer> cq = new ArrayDeque<>(List.of(p));
            seen[p] = true;
            boolean touches = false, edge = false;
            while (!cq.isEmpty()) {
                int e = cq.poll();
                comp.add(e);
                int i = e / (h * H), j = e / H % h;
                if (!editable(i, j)) edge = true;
                for (int f : six(e)) {
                    if (f < 0) continue;
                    if (kind[f] == 3 || kind[f] == 1) touches = true;
                    if (kind[f] == 2 && tree[f] < 0 && !seen[f]) { seen[f] = true; cq.add(f); }
                }
            }
            if (touches || edge) continue;
            clearAll(c, comp, H, "leaves left in the air");
            clumps++;
        }
        return new int[] {removed, stumps, clumps};
    }

    static int idx(int i, int y, int j) { return (i * h + j) * (Y1 - Y0 + 1) + (y - Y0); }

    static int[] six(int e) {
        int H = Y1 - Y0 + 1;
        int i = e / (h * H), j = e / H % h, y = e % H;
        return new int[] {i > 0 ? e - h * H : -1, i < w - 1 ? e + h * H : -1, j > 0 ? e - H : -1, j < h - 1 ? e + H : -1,
                y > 0 ? e - 1 : -1, y < H - 1 ? e + 1 : -1};
    }

    // one clear per vertical run in a column
    static void clearAll(Canvas c, List<Integer> blocks, int H, String reason) {
        TreeSet<Integer> sorted = new TreeSet<>(blocks);
        Integer start = null, prev = null;
        for (int e : sorted) {
            if (start != null && e == prev + 1 && e / H == start / H) { prev = e; continue; }
            if (start != null) emit(c, start, prev, H, reason);
            start = e;
            prev = e;
        }
        if (start != null) emit(c, start, prev, H, reason);
    }

    static void emit(Canvas c, int from, int to, int H, String reason) {
        int i = from / (h * H), j = from / H % h;
        c.clear(ax0 + i, from % H + Y0, az0 + j, ax0 + i, to % H + Y0, az0 + j, reason);
    }
}
