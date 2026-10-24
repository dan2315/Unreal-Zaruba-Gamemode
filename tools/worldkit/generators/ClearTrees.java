import java.nio.file.*;
import java.util.*;

/**
 * Removes whole trees: every tree whose lowest wood block (trunk base) lies in the box x0..x1, z0..z1 goes with its
 * leaves and vines; trees standing outside keep everything. A tree is wood connected to itself (logs, wood, fences and
 * copycat walls/beams: the library trees are built from those); leaves and vines go to the nearest wood through leaves
 * (6-connected, up to 7 steps), so a neighbour's crown that touches this one stays; leaves above y 36 that reach no
 * wood at all, under the removed crowns, go too (speckled library crowns). Under the removed crowns the forest
 * floor is turned back to meadow: podzol and moss become grass block, mushrooms are cleared (ferns and grass stay).
 * Everything is a clear or a swap: apply with `/buildkit apply <name> clear`.
 * Usage: java ClearTrees <region> x0 z0 x1 z1
 */
public class ClearTrees {
    static final int PAD = 10, Y0 = 20, Y1 = 110;

    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int bx0 = Integer.parseInt(a[1]), bz0 = Integer.parseInt(a[2]), bx1 = Integer.parseInt(a[3]), bz1 = Integer.parseInt(a[4]);
        int ax0 = bx0 - PAD, az0 = bz0 - PAD, w = bx1 - bx0 + 1 + 2 * PAD, d = bz1 - bz0 + 1 + 2 * PAD, H = Y1 - Y0 + 1;
        byte[] kind = new byte[w * d * H]; // 1 wood, 2 leaves/vine
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) for (int y = Y0; y <= Y1; y++) {
            String n = Region.name(r.state(ax0 + i, y, az0 + j)).replace("minecraft:", "");
            kind[(i * d + j) * H + (y - Y0)] = Tidy.log(n) ? (byte) 1 : n.endsWith("leaves") || n.equals("vine") ? (byte) 2 : 0;
        }
        // wood components (26-connected) and whether their base is in the box
        int[] tree = new int[kind.length];
        Arrays.fill(tree, -1);
        List<Boolean> remove = new ArrayList<>();
        for (int p = 0; p < kind.length; p++) {
            if (kind[p] != 1 || tree[p] >= 0) continue;
            int id = remove.size(), lowY = Integer.MAX_VALUE, lowI = 0, lowJ = 0;
            ArrayDeque<Integer> q = new ArrayDeque<>(List.of(p));
            tree[p] = id;
            while (!q.isEmpty()) {
                int e = q.poll(), i = e / (d * H), j = e / H % d, y = e % H;
                if (y < lowY) { lowY = y; lowI = i; lowJ = j; }
                for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dy = -1; dy <= 1; dy++) {
                    int pi = i + di, pj = j + dj, py = y + dy;
                    if (pi < 0 || pj < 0 || py < 0 || pi >= w || pj >= d || py >= H) continue;
                    int f = (pi * d + pj) * H + py;
                    if (kind[f] == 1 && tree[f] < 0) { tree[f] = id; q.add(f); }
                }
            }
            int bx = ax0 + lowI, bz = az0 + lowJ;
            remove.add(bx >= bx0 && bx <= bx1 && bz >= bz0 && bz <= bz1);
        }
        // leaves to the nearest wood (multi-source BFS through leaves)
        int[] step = new int[kind.length];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int p = 0; p < kind.length; p++) if (kind[p] == 1) q.add(p);
        while (!q.isEmpty()) {
            int e = q.poll();
            if (step[e] >= 7) continue;
            int i = e / (d * H), j = e / H % d, y = e % H;
            int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int[] n : nb) {
                int pi = i + n[0], pj = j + n[1], py = y + n[2];
                if (pi < 0 || pj < 0 || py < 0 || pi >= w || pj >= d || py >= H) continue;
                int f = (pi * d + pj) * H + py;
                if (kind[f] != 2 || tree[f] >= 0) continue;
                tree[f] = tree[e];
                step[f] = step[e] + 1;
                q.add(f);
            }
        }
        Canvas c = new Canvas(0, 0);
        boolean[][] canopy = new boolean[w][d];
        int wood = 0, leaves = 0, trees = 0;
        for (boolean b : remove) if (b) trees++;
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            Integer start = null;
            for (int y = 0; y <= H; y++) {
                int p = (i * d + j) * H + y;
                boolean go = y < H && kind[p] != 0 && tree[p] >= 0 && remove.get(tree[p]);
                if (go) { canopy[i][j] = true; if (kind[p] == 1) wood++; else leaves++; }
                if (go && start == null) start = y;
                if (!go && start != null) {
                    c.clear(ax0 + i, start + Y0, az0 + j, ax0 + i, y - 1 + Y0, az0 + j, "tree in the grove being removed");
                    start = null;
                }
            }
        }
        // leaves that reached no wood (library crowns are speckled) under the removed crowns, above bush height
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            if (!canopy[i][j]) continue;
            for (int y = 37 - Y0; y < H; y++) {
                int p = (i * d + j) * H + y;
                if (kind[p] == 2 && tree[p] < 0) { c.clear(ax0 + i, y + Y0, az0 + j, ax0 + i, y + Y0, az0 + j, "loose leaf of the removed grove"); leaves++; }
            }
        }
        // forest floor back to meadow under the removed crowns
        int floor = 0;
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            if (!canopy[i][j]) continue;
            int x = ax0 + i, z = az0 + j;
            int g = r.top(x, z, s -> { String n = Region.name(s).replace("minecraft:", ""); return !Tidy.member(n) && !BorderHills.plant(n) && !n.contains("mushroom"); });
            if (g == Integer.MIN_VALUE) continue;
            String n = Region.name(r.state(x, g, z));
            if (n.equals("minecraft:podzol") || n.equals("minecraft:moss_block")) { c.swap(x, g, z, n, "grass_block"); floor++; }
            String up = Region.name(r.state(x, g + 1, z));
            if (up.contains("mushroom")) c.clear(x, g + 1, z, x, g + 1, z, "mushroom left in the open");
        }
        System.err.println("trees removed " + trees + " (" + wood + " wood, " + leaves + " leaves), floor cells back to grass " + floor);
        c.print(new String[0]);
    }
}
