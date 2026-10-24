import java.nio.file.*;
import java.util.*;

/**
 * Fills empty land beside the rural roads with copies of the village's izbas (blueprints from blueprints/rural/izba_*).
 * Each new lot: the house turned so its door faces the road, on a level pad (the highest ground under it; lower columns get a
 * cobblestone foundation), an oak fence around the yard following the ground with a gap in front, a dirt path from the
 * road to the door, a small vegetable patch and a fruit-tree-sized tree from the library at the back.
 * A spot is used only if the whole lot is natural ground (no builds, trees, water, roads) varying by at most 2 in height,
 * 3-7 blocks from a road (the roads of this area are basalt), and at least `gap` blocks from other houses.
 * Usage: java RuralInfill <region dir> <rural blueprints dir> <trees dir> x0 z0 x1 z1 <max houses> [gap]
 */
public class RuralInfill {
    static Region r;

    record House(Blueprint bp, int doorX, int doorZ, String front) {}

    public static void main(String[] a) throws Exception {
        r = new Region(Path.of(a[0]));
        Path rural = Path.of(a[1]), treesDir = Path.of(a[2]);
        int x0 = Integer.parseInt(a[3]), z0 = Integer.parseInt(a[4]), x1 = Integer.parseInt(a[5]), z1 = Integer.parseInt(a[6]);
        int max = Integer.parseInt(a[7]);
        int gap = a.length > 8 ? Integer.parseInt(a[8]) : 10;
        List<Blueprint> designs = new ArrayList<>();
        try (var files = Files.list(rural)) {
            for (Path p : (Iterable<Path>) files::iterator) if (p.getFileName().toString().startsWith("izba_")) designs.add(Blueprint.load(p));
        }
        designs.sort(Comparator.comparing(b -> b.name));
        List<Blueprint> trees = new ArrayList<>();
        try (var files = Files.list(treesDir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String n = p.getFileName().toString();
                if (n.startsWith("oak_birch_h12") || n.startsWith("oak_dark_oak_h9") || n.startsWith("birch_birch_h9")) trees.add(Blueprint.load(p));
            }
        }

        // the land: ground height, road, free
        int w = x1 - x0 + 1, d = z1 - z0 + 1;
        int[][] ground = new int[d][w];
        boolean[][] road = new boolean[d][w], free = new boolean[d][w];
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) {
            int g = Integer.MIN_VALUE;
            boolean ok = true, isRoad = false;
            for (int y = 80; y >= 20; y--) {
                String s = r.state(x, y, z);
                if (Region.isAir(s)) continue;
                String n = Region.name(s).replace("minecraft:", "");
                if (BorderHills.plant(n)) continue;
                if (n.equals("water") || n.endsWith("leaves") || n.endsWith("_log") || n.endsWith("_wood")) { ok = false; break; }
                if (n.contains("basalt")) { isRoad = true; ok = false; g = y; break; }
                if (!Export.natural(s)) { ok = false; break; }
                g = y;
                break;
            }
            ground[z - z0][x - x0] = g;
            road[z - z0][x - x0] = isRoad;
            free[z - z0][x - x0] = ok && g != Integer.MIN_VALUE;
        }
        // distance to the nearest road (BFS, capped)
        int[][] roadDist = new int[d][w];
        for (int[] row : roadDist) Arrays.fill(row, 99);
        Deque<int[]> q = new ArrayDeque<>();
        for (int gz = 0; gz < d; gz++) for (int gx = 0; gx < w; gx++) if (road[gz][gx]) { roadDist[gz][gx] = 0; q.add(new int[] {gx, gz}); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int nd = roadDist[p[1]][p[0]] + 1;
            if (nd > 30) continue;
            for (int[] dl : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = p[0] + dl[0], nz = p[1] + dl[1];
                if (nx < 0 || nz < 0 || nx >= w || nz >= d || roadDist[nz][nx] <= nd) continue;
                roadDist[nz][nx] = nd;
                q.add(new int[] {nx, nz});
            }
        }

        Canvas c = new Canvas(0, 0);
        c.region = r;
        boolean[][] used = new boolean[d][w];
        int placed = 0, designIndex = 0;
        String[] dirs = {"north", "east", "south", "west"};
        int[][] step = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        search:
        for (int gz = 0; gz < d && placed < max; gz += 3) for (int gx = 0; gx < w && placed < max; gx += 3) {
            for (int dir = 0; dir < 4; dir++) {
                Blueprint base = designs.get((designIndex + placed) % designs.size());
                House h = orient(base, dir);
                if (h == null) continue;
                Blueprint bp = h.bp;
                int yard = 3, back = 7;
                // lot box in grid coordinates: house plus yard; the front (door side) faces the road
                int lw = bp.w + 2 * yard, ld = bp.d + 2 * yard;
                int[] fs = step[dir];
                int lx0 = gx, lz0 = gz;
                // extra room at the back for the garden
                int ex0 = lx0 - (fs[0] > 0 ? back : 0), ex1 = lx0 + lw - 1 + (fs[0] < 0 ? back : 0);
                int ez0 = lz0 - (fs[1] > 0 ? back : 0), ez1 = lz0 + ld - 1 + (fs[1] < 0 ? back : 0);
                if (ex0 < 0 || ez0 < 0 || ex1 >= w || ez1 >= d) continue;
                int gmin = Integer.MAX_VALUE, gmax = Integer.MIN_VALUE;
                boolean ok = true;
                for (int zz = ez0 - gap / 2; zz <= ez1 + gap / 2 && ok; zz++) for (int xx = ex0 - gap / 2; xx <= ex1 + gap / 2 && ok; xx++) {
                    if (xx < 0 || zz < 0 || xx >= w || zz >= d) continue;
                    if (used[zz][xx]) ok = false;
                }
                for (int zz = ez0; zz <= ez1 && ok; zz++) for (int xx = ex0; xx <= ex1 && ok; xx++) {
                    if (!free[zz][xx]) { ok = false; break; }
                    gmin = Math.min(gmin, ground[zz][xx]); gmax = Math.max(gmax, ground[zz][xx]);
                }
                if (!ok || gmax - gmin > 2) continue;
                // the road must be just in front of the door: the cell 3..7 blocks out from the front fence is road
                int fdx = lx0 + yard + h.doorX, fdz = lz0 + yard + h.doorZ;
                int dist = -1;
                for (int k = 1; k <= 12; k++) {
                    int px = fdx + fs[0] * k, pz = fdz + fs[1] * k;
                    if (px < 0 || pz < 0 || px >= w || pz >= d) break;
                    if (road[pz][px]) { dist = k; break; }
                }
                if (dist < yard + 2 || dist > yard + 8) continue;
                // build it
                int gy = gmax;
                int wx = x0 + lx0 + yard, wz = z0 + lz0 + yard;
                Stamp.stamp(c, bp, wx, gy, wz, r, "cobblestone");
                // pad under the house level with the ground
                for (int zz = 0; zz < bp.d; zz++) for (int xx = 0; xx < bp.w; xx++) if (bp.at(xx, bp.ground, zz) == null) c.ground(wx + xx, gy, wz + zz, wx + xx, gy, wz + zz, "coarse_dirt");
                fence(c, x0 + ex0, z0 + ez0, x0 + ex1, z0 + ez1, ground, x0, z0, gy, dir, x0 + fdx, z0 + fdz);
                // path from the door to the road
                for (int k = 1; k < dist; k++) {
                    int px = fdx + fs[0] * k, pz = fdz + fs[1] * k;
                    for (int s = -1; s <= 0; s++) {
                        int qx = px + (fs[1] != 0 ? s : 0), qz = pz + (fs[0] != 0 ? s : 0);
                        if (qx < 0 || qz < 0 || qx >= w || qz >= d || road[qz][qx]) continue;
                        c.ground(x0 + qx, ground[qz][qx], z0 + qz, x0 + qx, ground[qz][qx], z0 + qz, "dirt_path");
                    }
                }
                // garden at the back: rows of crops, a composter, a tree
                int bx = x0 + lx0 + yard + bp.w / 2 - fs[0] * (bp.w / 2 + 3), bz = z0 + lz0 + yard + bp.d / 2 - fs[1] * (bp.d / 2 + 3);
                for (int i = -3; i <= 3; i++) for (int j = -1; j <= 1; j++) {
                    int px = bx + (fs[1] != 0 ? i : j), pz = bz + (fs[0] != 0 ? i : j);
                    int gg = ground[Math.max(0, Math.min(d - 1, pz - z0))][Math.max(0, Math.min(w - 1, px - x0))];
                    if (gg == Integer.MIN_VALUE) continue;
                    boolean row = Math.floorMod(i, 2) == 0;
                    c.ground(px, gg, pz, px, gg, pz, row ? "farmland[moisture=7]" : "coarse_dirt");
                    if (row) c.set(px, gg + 1, pz, (Math.floorMod(i, 4) == 0 ? "carrots[age=7]" : "wheat[age=7]"));
                }
                if (!trees.isEmpty()) {
                    Blueprint t = trees.get(placed % trees.size()).rotated(placed);
                    // beside the vegetable patch, clear of the house box
                    int tx = bx + (fs[1] != 0 ? 7 : -fs[0] * 3) - t.w / 2, tz = bz + (fs[0] != 0 ? 7 : -fs[1] * 3) - t.d / 2;
                    boolean hits = tx < wx + bp.w && tx + t.w > wx && tz < wz + bp.d && tz + t.d > wz;
                    if (hits) { tx = bx + (fs[1] != 0 ? -7 : -fs[0] * 3) - t.w / 2; tz = bz + (fs[0] != 0 ? -7 : -fs[1] * 3) - t.d / 2;
                        hits = tx < wx + bp.w && tx + t.w > wx && tz < wz + bp.d && tz + t.d > wz; }
                    int tg = ground[Math.max(0, Math.min(d - 1, tz + t.d / 2 - z0))][Math.max(0, Math.min(w - 1, tx + t.w / 2 - x0))];
                    if (tg != Integer.MIN_VALUE && !hits) Stamp.stamp(c, t, tx, tg, tz, null, null);
                }
                for (int zz = ez0 - 2; zz <= ez1 + 2; zz++) for (int xx = ex0 - 2; xx <= ex1 + 2; xx++)
                    if (xx >= 0 && zz >= 0 && xx < w && zz < d) used[zz][xx] = true;
                placed++;
                System.err.printf("house %d: %s door to the %s at %d %d %d, lot x %d..%d z %d..%d%n", placed, bp.name, dirs[dir],
                        x0 + fdx, gy + 1, z0 + fdz, x0 + ex0, x0 + ex1, z0 + ez0, z0 + ez1);
                continue search;
            }
        }
        System.err.println(placed + " houses");
        c.print(new String[0]);
    }

    /** The design turned so that its front door faces `dir` (0 north, 1 east, 2 south, 3 west); the door position in the turned box. */
    static House orient(Blueprint base, int dir) {
        for (int q = 0; q < 4; q++) {
            Blueprint bp = base.rotated(q);
            int[] door = frontDoor(bp);
            if (door == null) return null;
            if (door[2] == dir) return new House(bp, door[0], door[1], Blueprint.DIRS[dir]);
        }
        return null;
    }

    /** The lower half of the door nearest to the outside of the box: x, z and the side it is on (0 north .. 3 west). */
    static int[] frontDoor(Blueprint bp) {
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int y = 0; y < bp.h; y++) for (int z = 0; z < bp.d; z++) for (int x = 0; x < bp.w; x++) {
            String s = bp.at(x, y, z);
            if (s == null || !s.contains("_door[") || !s.contains("half=lower") || s.contains("trapdoor")) continue;
            int[] dists = {z, bp.w - 1 - x, bp.d - 1 - z, x};
            int side = 0;
            for (int i = 1; i < 4; i++) if (dists[i] < dists[side]) side = i;
            if (dists[side] < bestDist) { bestDist = dists[side]; best = new int[] {x, z, side}; }
        }
        return best;
    }

    /** Oak fence around the lot on the ground, with a 3-wide gap in front of the door. */
    static void fence(Canvas c, int ax0, int az0, int ax1, int az1, int[][] ground, int x0, int z0, int gy, int dir, int doorX, int doorZ) {
        List<int[]> ring = new ArrayList<>();
        for (int x = ax0; x <= ax1; x++) { ring.add(new int[] {x, az0}); ring.add(new int[] {x, az1}); }
        for (int z = az0 + 1; z < az1; z++) { ring.add(new int[] {ax0, z}); ring.add(new int[] {ax1, z}); }
        for (int[] p : ring) {
            boolean frontSide = dir == 0 ? p[1] == az0 : dir == 2 ? p[1] == az1 : dir == 1 ? p[0] == ax1 : p[0] == ax0;
            boolean gate = frontSide && (dir % 2 == 0 ? Math.abs(p[0] - doorX) <= 1 : Math.abs(p[1] - doorZ) <= 1);
            if (gate) continue;
            int g = ground[p[1] - z0][p[0] - x0];
            boolean ns = p[0] == ax0 || p[0] == ax1, ew = p[1] == az0 || p[1] == az1;
            String s = "oak_fence[east=" + ew + ",north=" + ns + ",south=" + ns + ",waterlogged=false,west=" + ew + "]";
            c.set(p[0], g + 1, p[1], s);
        }
    }
}
