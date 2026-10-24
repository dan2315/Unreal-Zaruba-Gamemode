import java.nio.file.*;
import java.util.*;

/**
 * Finds the houses in an area and the fenced lots around them, for copying them into the blueprint library.
 * A house = a group of columns (at least `minArea`) with 3+ built blocks above the ground that are not fences or plants.
 * Its lot = the house's box widened in each direction up to the first fence line (within 24 blocks), or by 3 if none.
 * Prints per lot: the box (x0 z0 x1 z1), ground y range, the house box and its height, and the main blocks.
 * Usage: java Lots <region dir> x0 z0 x1 z1 [minArea]
 */
public class Lots {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        int minArea = a.length > 5 ? Integer.parseInt(a[5]) : 40;
        int w = x1 - x0 + 1, d = z1 - z0 + 1;
        int[][] ground = new int[d][w];
        boolean[][] house = new boolean[d][w], fence = new boolean[d][w];
        int[][] top = new int[d][w];
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) {
            int g = Integer.MIN_VALUE, built = 0, fences = 0, t = Integer.MIN_VALUE;
            for (int y = 20; y <= 90; y++) {
                String s = r.state(x, y, z);
                if (Region.isAir(s)) continue;
                String n = Region.name(s).replace("minecraft:", "");
                if (Export.natural(s) || n.equals("water")) {
                    if (!n.endsWith("leaves") && !n.contains("grass") && !n.contains("fern") && !n.equals("water")) g = y;
                    continue;
                }
                if (n.contains("fence") || n.contains("wall") && !n.contains("wall_") || n.endsWith("_gate")) { fences++; continue; }
                built++;
                t = Math.max(t, y);
            }
            ground[z - z0][x - x0] = g;
            house[z - z0][x - x0] = built >= 3;
            fence[z - z0][x - x0] = fences > 0;
            top[z - z0][x - x0] = t;
        }
        boolean[][] seen = new boolean[d][w];
        int n = 0;
        for (int sz = 0; sz < d; sz++) for (int sx = 0; sx < w; sx++) {
            if (!house[sz][sx] || seen[sz][sx]) continue;
            Deque<int[]> q = new ArrayDeque<>();
            q.add(new int[] {sx, sz});
            seen[sz][sx] = true;
            int area = 0, mx0 = sx, mx1 = sx, mz0 = sz, mz1 = sz, maxTop = Integer.MIN_VALUE;
            while (!q.isEmpty()) {
                int[] p = q.poll();
                area++;
                mx0 = Math.min(mx0, p[0]); mx1 = Math.max(mx1, p[0]); mz0 = Math.min(mz0, p[1]); mz1 = Math.max(mz1, p[1]);
                maxTop = Math.max(maxTop, top[p[1]][p[0]]);
                for (int[] dlt : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = p[0] + dlt[0], nz = p[1] + dlt[1];
                    if (nx < 0 || nz < 0 || nx >= w || nz >= d || seen[nz][nx] || !house[nz][nx]) continue;
                    seen[nz][nx] = true;
                    q.add(new int[] {nx, nz});
                }
            }
            if (area < minArea) continue;
            int cx = (mx0 + mx1) / 2, cz = (mz0 + mz1) / 2;
            int lx0 = ray(fence, cx, cz, mx0, -1, 0, w, d), lx1 = ray(fence, cx, cz, mx1, 1, 0, w, d);
            int lz0 = ray(fence, cx, cz, mz0, 0, -1, w, d), lz1 = ray(fence, cx, cz, mz1, 0, 1, w, d);
            int gmin = Integer.MAX_VALUE, gmax = Integer.MIN_VALUE;
            Map<String, Integer> counts = new HashMap<>();
            for (int z = lz0; z <= lz1; z++) for (int x = lx0; x <= lx1; x++) {
                if (x < 0 || z < 0 || x >= w || z >= d) continue;
                int g = ground[z][x];
                if (g != Integer.MIN_VALUE) { gmin = Math.min(gmin, g); gmax = Math.max(gmax, g); }
            }
            for (int z = mz0; z <= mz1; z++) for (int x = mx0; x <= mx1; x++)
                for (int y = ground[z][x] + 1; y <= top[z][x]; y++) {
                    String s = r.state(x0 + x, y, z0 + z);
                    if (!Region.isAir(s) && !Export.natural(s)) counts.merge(Region.name(s).replace("minecraft:", ""), 1, Integer::sum);
                }
            StringBuilder main = new StringBuilder();
            counts.entrySet().stream().sorted((p, q2) -> q2.getValue() - p.getValue()).limit(6).forEach(e -> main.append(e.getKey()).append(' ').append(e.getValue()).append(", "));
            n++;
            System.out.printf("lot %d: x %d..%d z %d..%d (%dx%d), ground y %d..%d; house x %d..%d z %d..%d top y %d, area %d%n   %s%n",
                    n, x0 + lx0, x0 + lx1, z0 + lz0, z0 + lz1, lx1 - lx0 + 1, lz1 - lz0 + 1, gmin, gmax,
                    x0 + mx0, x0 + mx1, z0 + mz0, z0 + mz1, maxTop, area, main);
        }
    }

    /** From the house edge `from` outward along (dx, dz) through the centre line, the first fence column (or from +/- 3). */
    static int ray(boolean[][] fence, int cx, int cz, int from, int dx, int dz, int w, int d) {
        for (int k = 1; k <= 24; k++) {
            int x = dx != 0 ? from + dx * k : cx, z = dz != 0 ? from + dz * k : cz;
            if (x < 0 || z < 0 || x >= w || z >= d) break;
            if (fence[z][x]) return dx != 0 ? x : z;
        }
        return from + (dx != 0 ? dx : dz) * 3;
    }
}
