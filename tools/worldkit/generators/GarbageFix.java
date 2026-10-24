import java.nio.file.*;
import java.util.*;

/**
 * Second pass over the garbage fields (after GarbageFill): roads through them are cleared and piles get solid all the
 * way down. In each box, with the ground layer at groundY:
 *   - Road: a column whose ground block is road surface (deepslate, cyan terracotta, cut tuff, white/gray concrete,
 *     polished deepslate...) with such columns in at least 9 of the 25 around it, widened by `margin` blocks. Every junk
 *     block standing on a road column (up to groundY+14) is cleared, so the road is clean edge to edge; holes in the
 *     road surface are patched with the most common road block around.
 *   - Piles: on every other column, each air (or plant) cell under the highest junk block of the column (up to
 *     groundY+14) is filled with the junk mix, so no pile has air under it.
 * Junk = anything that is not a fence/wall-type boundary, lamp, sign post, road furniture or tree; those are left alone.
 * The fill is the GarbageFill mix without gravel (a falling block over a pocket would drop out).
 * Clears need `/buildkit apply <name> clear`. `scan` mode only prints the palette and the counts.
 * Boxes after --avoid (x0 z0 x1 z1 each, last on the line) are left as they are: sheds and other builds in a field.
 * Usage: java GarbageFix <region> <groundY> <margin> [scan] x0 z0 x1 z1 [x0 z0 x1 z1 ...] [--avoid x0 z0 x1 z1 ...]
 */
public class GarbageFix {
    static final Set<String> ROAD = Set.of("deepslate", "cyan_terracotta", "create:cut_tuff", "white_concrete", "gray_concrete",
            "light_gray_concrete", "polished_deepslate", "create:polished_cut_deepslate", "smooth_stone", "andesite", "black_concrete",
            "yellow_concrete", "create:cut_andesite", "stone");
    // left alone: boundaries and furniture (the field's own fences and lamps)
    static boolean keep(String n) {
        return n.endsWith("_fence") || n.endsWith("fence_gate") || n.contains("lamp") || n.contains("lantern") || n.contains("light")
                || n.contains("chain") || n.contains("bars") || n.endsWith("_pane") || n.contains("barrier") || n.contains("sign") && !n.contains("wall_sign")
                || n.contains("girder") || n.contains("pipe") || n.contains("catwalk") || n.endsWith("leaves") || Tidy.log(n); // trees over the field
    }

    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int g = Integer.parseInt(a[1]), margin = Integer.parseInt(a[2]);
        int i0 = 3;
        boolean scan = a[3].equals("scan");
        if (scan) i0 = 4;
        Canvas c = new Canvas(0, 0);
        Map<String, Integer> palette = new TreeMap<>(), kept = new TreeMap<>(), onRoad = new TreeMap<>();
        List<int[]> avoid = new ArrayList<>();
        for (int k = 0; k < a.length; k++) if (a[k].equals("--avoid")) avoid.add(new int[] {Integer.parseInt(a[k + 1]), Integer.parseInt(a[k + 2]), Integer.parseInt(a[k + 3]), Integer.parseInt(a[k + 4])});
        a = Arrays.stream(a).limit(Arrays.asList(a).indexOf("--avoid") < 0 ? a.length : Arrays.asList(a).indexOf("--avoid")).toArray(String[]::new);
        int roadCols = 0, roadCleared = 0, pockets = 0, pocketCols = 0, patched = 0;
        for (int b = i0; b + 3 < a.length; b += 4) {
            int x0 = Integer.parseInt(a[b]), z0 = Integer.parseInt(a[b + 1]), x1 = Integer.parseInt(a[b + 2]), z1 = Integer.parseInt(a[b + 3]);
            int w = x1 - x0 + 1, d = z1 - z0 + 1, P = 3 + margin;
            // road mask over the box plus a pad
            boolean[][] surf = new boolean[w + 2 * P][d + 2 * P];
            for (int i = 0; i < w + 2 * P; i++) for (int j = 0; j < d + 2 * P; j++)
                surf[i][j] = i >= P && j >= P && i < w + P && j < d + P && ROAD.contains(n(r.state(x0 - P + i, g, z0 - P + j))); // the streets round the field do not count
            boolean[][] road = new boolean[w + 2 * P][d + 2 * P];
            for (int i = 2; i < w + 2 * P - 2; i++) for (int j = 2; j < d + 2 * P - 2; j++) {
                int cnt = 0;
                for (int di = -2; di <= 2; di++) for (int dj = -2; dj <= 2; dj++) if (surf[i + di][j + dj]) cnt++;
                road[i][j] = cnt >= 9;
            }
            boolean[][] wide = new boolean[w + 2 * P][d + 2 * P];
            for (int i = 0; i < w + 2 * P; i++) for (int j = 0; j < d + 2 * P; j++) {
                if (!road[i][j]) continue;
                for (int di = -margin; di <= margin; di++) for (int dj = -margin; dj <= margin; dj++) {
                    int p = i + di, q = j + dj;
                    if (p >= 0 && q >= 0 && p < w + 2 * P && q < d + 2 * P && Math.hypot(di, dj) <= margin + 0.3) wide[p][q] = true;
                }
            }
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                int i = x - x0 + P, j = z - z0 + P;
                boolean skip = false;
                for (int[] v : avoid) skip |= x >= v[0] && x <= v[2] && z >= v[1] && z <= v[3];
                if (skip) continue;
                String[] col = new String[15];
                int top = -1;
                for (int k = 1; k < 15; k++) {
                    col[k] = r.state(x, g + k, z);
                    String n = n(col[k]);
                    if (Region.isAir(col[k]) || BorderHills.plant(n)) continue;
                    if (keep(n)) { kept.merge(n, 1, Integer::sum); continue; }
                    palette.merge(n, 1, Integer::sum);
                    top = k;
                }
                if (wide[i][j]) {
                    roadCols++;
                    for (int k = 1; k < 15; k++) {
                        String n = n(col[k]);
                        if (Region.isAir(col[k]) || keep(n)) continue;
                        if (!scan) c.clear(x, g + k, z, x, g + k, z, "junk on the road through the dump");
                        onRoad.merge(n, 1, Integer::sum);
                        roadCleared++;
                    }
                    if (Region.isAir(r.state(x, g, z))) {
                        String patch = majority(r, x, g, z);
                        if (patch != null) { if (!scan) c.set(x, g, z, patch); patched++; }
                    }
                    continue;
                }
                if (top < 0) continue;
                boolean any = false;
                for (int k = 1; k < top; k++) {
                    String n = n(col[k]);
                    if (!Region.isAir(col[k]) && !BorderHills.plant(n)) continue;
                    if (!scan) {
                        if (BorderHills.plant(n)) c.clear(x, g + k, z, x, g + k, z, "plant inside a pile");
                        c.set(x, g + k, z, solidJunk(x, g + k, z));
                    }
                    pockets++;
                    any = true;
                }
                if (any) pocketCols++;
            }
        }
        System.err.println("road columns " + roadCols + ", junk blocks on them " + roadCleared + ", road holes patched " + patched
                + "; air under piles: " + pockets + " cells in " + pocketCols + " columns");
        if (scan) {
            System.err.println("junk palette: " + palette);
            System.err.println("left alone: " + kept);
            System.err.println("on the roads (would be cleared): " + onRoad);
            return;
        }
        c.print(new String[0]);
    }

    // the GarbageFill mix without gravel: a falling block in a pocket would drop out
    static String solidJunk(int x, int y, int z) {
        String j = GarbageFill.junk(x, y, z);
        return j.equals("gravel") ? (BorderHills.hash(x * 5 + y, z * 3) < 0.5 ? "cobbled_deepslate" : "tuff") : j;
    }

    static String majority(Region r, int x, int g, int z) throws Exception {
        Map<String, Integer> m = new HashMap<>();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            String s = r.state(x + dx, g, z + dz);
            if (ROAD.contains(n(s))) m.merge(s, 1, Integer::sum);
        }
        return m.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    static String n(String s) { return Region.name(s).replace("minecraft:", ""); }
}
