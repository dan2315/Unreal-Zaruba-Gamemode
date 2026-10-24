import java.nio.file.*;
import java.util.*;

/**
 * Finds trees in an area of the map and saves the distinct kinds into the blueprint library (blueprints/trees/).
 * A tree = a trunk (blocks of logs, wood and fences/walls standing on the ground, connected) plus the leaves reachable
 * from it through leaves within 7 blocks; leaves are given to the trunk that reaches them first. Trees are grouped by
 * their wood and leaf blocks and their size (height and crown width, in steps of 3); one example of each group is
 * saved, named after its blocks and size. The blueprint's ground layer is the layer under the trunk (the ground block
 * itself is not part of the tree), so stamping it at ground y puts the trunk on the ground.
 * Usage: java Trees <region dir> <out dir> x0 z0 x1 z1 [minHeight] [maxPerKind]
 */
public class Trees {
    static Region r;

    public static void main(String[] a) throws Exception {
        r = new Region(Path.of(a[0]));
        Path out = Path.of(a[1]);
        int x0 = Integer.parseInt(a[2]), z0 = Integer.parseInt(a[3]), x1 = Integer.parseInt(a[4]), z1 = Integer.parseInt(a[5]);
        int minHeight = a.length > 6 ? Integer.parseInt(a[6]) : 5;
        int maxPerKind = a.length > 7 ? Integer.parseInt(a[7]) : 1;
        Set<Long> claimed = new HashSet<>();
        Map<String, List<Blueprint>> kinds = new TreeMap<>();
        Map<String, Integer> kindCounts = new TreeMap<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            for (int y = 25; y <= 70; y++) {
                String s = r.state(x, y, z);
                if (!trunk(s) || claimed.contains(key(x, y, z))) continue;
                String below = r.state(x, y - 1, z);
                if (trunk(below) || leaf(below) || Region.isAir(below) || Region.name(below).endsWith("water")) continue; // only start at a trunk's foot
                Blueprint bp = grow(x, y, z, claimed);
                if (bp == null || bp.h < minHeight) continue;
                String kind = signature(bp);
                kindCounts.merge(kind, 1, Integer::sum);
                List<Blueprint> list = kinds.computeIfAbsent(kind, k -> new ArrayList<>());
                if (list.size() < maxPerKind) list.add(bp);
            }
        }
        Files.createDirectories(out);
        for (var e : kinds.entrySet()) {
            int i = 0;
            for (Blueprint bp : e.getValue()) {
                bp.name = e.getKey() + (e.getValue().size() > 1 ? "_" + (++i) : "");
                bp.tags.addAll(List.of("tree", "nature"));
                bp.save(out.resolve(bp.name + ".bp"));
                System.out.printf("%-55s seen %3d  %dx%dx%d  %d blocks  from %d %d %d%n", bp.name, kindCounts.get(e.getKey()), bp.w, bp.h, bp.d, bp.count(), bp.source[0], bp.source[1], bp.source[2]);
            }
        }
    }

    static boolean trunk(String s) {
        String n = Region.name(s);
        return n.endsWith("_log") || n.endsWith("_wood") || n.endsWith("_stem") || n.endsWith("_hyphae")
                || n.contains("fence") && !n.contains("gate") || n.contains("copycat_wall") || n.contains("copycat_beam");
    }

    static boolean leaf(String s) {
        String n = Region.name(s);
        return n.endsWith("_leaves") || n.endsWith("wart_block") || n.equals("minecraft:moss_carpet") && false;
    }

    static long key(int x, int y, int z) {
        return Region.BlockPosKey.of(x, y, z);
    }

    /** The tree whose trunk foot is at (x, y, z), as a blueprint (null if it is clearly not a tree: no leaves). */
    static Blueprint grow(int fx, int fy, int fz, Set<Long> claimed) throws Exception {
        Map<Long, String> cells = new HashMap<>();
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[] {fx, fy, fz, 0});
        claimed.add(key(fx, fy, fz));
        cells.put(key(fx, fy, fz), r.state(fx, fy, fz));
        int leaves = 0;
        while (!q.isEmpty() && cells.size() < 6000) {
            int[] p = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                int x = p[0] + dx, y = p[1] + dy, z = p[2] + dz;
                long k = key(x, y, z);
                if (claimed.contains(k) || y < fy) continue;
                if (Math.abs(x - fx) > 12 || Math.abs(z - fz) > 12) continue;
                String s = r.state(x, y, z);
                boolean t = trunk(s), l = leaf(s);
                if (!t && !l) continue;
                int dist = l ? p[3] + 1 : 0;
                if (l && dist > 7) continue;
                claimed.add(k);
                cells.put(k, s);
                if (l) leaves++;
                q.add(new int[] {x, y, z, dist});
            }
        }
        if (leaves < 6) return null;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : cells.keySet()) {
            int x = Canvas.x(k), y = Canvas.y(k), z = Canvas.z(k);
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        // one layer under the foot is the ground layer (kept empty)
        Blueprint bp = new Blueprint(maxX - minX + 1, maxY - fy + 2, maxZ - minZ + 1);
        bp.ground = 0;
        bp.source = new int[] {minX, fy - 1, minZ};
        for (var e : cells.entrySet()) {
            int x = Canvas.x(e.getKey()), y = Canvas.y(e.getKey()), z = Canvas.z(e.getKey());
            String s = e.getValue();
            if (leaf(s)) s = s.replace("persistent=false", "persistent=true");
            bp.cells[y - fy + 1][z - minZ][x - minX] = bp.index(s);
        }
        return bp;
    }

    static String signature(Blueprint bp) {
        TreeMap<String, Integer> wood = new TreeMap<>(), leaves = new TreeMap<>();
        for (int y = 0; y < bp.h; y++) for (int z = 0; z < bp.d; z++) for (int x = 0; x < bp.w; x++) {
            String s = bp.at(x, y, z);
            if (s == null) continue;
            String n = Region.name(s).replaceAll("^[a-z_]+:", "");
            (leaf(s) ? leaves : wood).merge(n, 1, Integer::sum);
        }
        String w = wood.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("x").replaceAll("_(log|wood|fence|stem)$", "");
        String l = leaves.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("x").replace("_leaves", "");
        int width = Math.max(bp.w, bp.d);
        return w + "_" + l + "_h" + (bp.h / 3 * 3) + "_w" + (width / 3 * 3);
    }
}
