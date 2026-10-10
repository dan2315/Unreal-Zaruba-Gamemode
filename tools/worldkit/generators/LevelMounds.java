import java.nio.file.*;
import java.util.*;

/**
 * Levels the small raised mounds (ground bumps with rocks, wheat, flowers) that touch a road corridor, so vehicles can
 * drive through. A mound is a group of 8-connected columns with non-tree blocks above the ground layer; every column of
 * a mound that reaches into the corridor is lowered (whole mounds, not cut at the corridor edge). The ground layer of a
 * levelled column gets the road texture inside the corridor and grass block outside it. Trees are left to Pathway.
 * Same curve arguments as Pathway; apply with "/buildkit apply <name> clear".
 * Usage: java -cp <worldkit classes> LevelMounds <region dir> <groundY> <half width> x z x z ... > level.build
 */
public class LevelMounds {
    static final String[] ROAD = {"coarse_dirt", "podzol", "rooted_dirt", "coarse_dirt", "podzol", "rooted_dirt", "moss_block"};

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        int ground = Integer.parseInt(a[1]);
        double halfWidth = Double.parseDouble(a[2]);
        double[][] points = new double[(a.length - 3) / 2][];
        for (int i = 0; i < points.length; i++) points[i] = new double[] {Double.parseDouble(a[3 + 2 * i]), Double.parseDouble(a[4 + 2 * i])};
        List<double[]> curve = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            double[] p0 = points[Math.max(0, i - 1)], p1 = points[i], p2 = points[i + 1], p3 = points[Math.min(points.length - 1, i + 2)];
            int n = (int) Math.ceil(Math.hypot(p2[0] - p1[0], p2[1] - p1[1]) / 0.5);
            for (int k = 0; k < n; k++) {
                double t = (double) k / n;
                curve.add(new double[] {Pathway.cr(p0[0], p1[0], p2[0], p3[0], t), Pathway.cr(p0[1], p1[1], p2[1], p3[1], t), 0});
            }
        }
        double reach = halfWidth * 1.15;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (double[] c : curve) {
            minX = Math.min(minX, (int) (c[0] - reach) - 12); maxX = Math.max(maxX, (int) (c[0] + reach) + 12);
            minZ = Math.min(minZ, (int) (c[1] - reach) - 12); maxZ = Math.max(maxZ, (int) (c[1] + reach) + 12);
        }

        // columns with something solid (not a tree, not a plant) above the ground layer
        Map<Long, TreeMap<Integer, String>> raised = new HashMap<>();
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                TreeMap<Integer, String> col = Surface.column(dir, x, z);
                boolean bump = false;
                for (int y = ground + 1; y <= ground + 4; y++) {
                    String b = col.get(y);
                    if (b != null && !isTree(b) && !isPlant(b)) bump = true;
                }
                if (bump) raised.put(key(x, z), col);
            }

        Canvas canvas = new Canvas(0, 0);
        Set<Long> seen = new HashSet<>();
        int mounds = 0, columns = 0;
        for (long start : raised.keySet()) {
            if (seen.contains(start)) continue;
            List<Long> group = new ArrayList<>();
            boolean touches = false;
            ArrayDeque<Long> queue = new ArrayDeque<>(List.of(start));
            seen.add(start);
            while (!queue.isEmpty()) {
                long k = queue.poll();
                group.add(k);
                int x = (int) (k >> 32), z = (int) k;
                if (Pathway.distance(curve, x, z) <= reach) touches = true;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        long n = key(x + dx, z + dz);
                        if (raised.containsKey(n) && seen.add(n)) queue.add(n);
                    }
            }
            if (!touches || group.size() > 120) continue;   // big raised areas are terrain or builds, not mounds
            mounds++;
            for (long k : group) {
                int x = (int) (k >> 32), z = (int) k;
                TreeMap<Integer, String> col = raised.get(k);
                int top = ground;
                for (int y = ground + 1; y <= ground + 6; y++) {
                    String b = col.get(y);
                    if (b != null && !isTree(b)) top = y;
                }
                canvas.clear(x, ground + 1, z, x, top, z, "mound in the road");
                String under = col.get(ground);
                double u = Pathway.distance(curve, x, z) / halfWidth;
                String cover = u <= 1.0 && Pathway.hash(x, z, 11) < 0.75 ? ROAD[(int) (Pathway.hash(x, z, 12) * ROAD.length)] : "grass_block";
                if (under != null && !under.endsWith(":" + cover)) canvas.swap(x, ground, z, under, cover);
                columns++;
            }
        }
        System.err.println("mounds touching the road: " + mounds + ", columns levelled: " + columns);
        canvas.print(new String[0]);
    }

    static boolean isTree(String b) {
        return b.endsWith("leaves") || b.endsWith("_log") || b.endsWith("_wood");
    }

    static boolean isPlant(String b) {
        return Pathway.PLANTS.contains(b.substring(b.indexOf(':') + 1)) || b.endsWith("sunflower") || b.endsWith("blue_orchid");
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
