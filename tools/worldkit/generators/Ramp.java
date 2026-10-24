import java.nio.file.*;

/**
 * A driveable ramp from a level area up (or down) a hillside: over the columns x0..x1, starting at row zStart and going
 * along z in the direction `dir` (+1 south, -1 north), the ground is set to startY + k / run (k = rows from the start),
 * until it meets the natural ground. Earth above the ramp (and plants, trees on it) is cleared, holes below it filled,
 * the top is grass with the map's painted-grass patches. The clears need `/buildkit apply <name> clear`.
 * Beside the ramp (taperW columns west of x0, taperE east of x1) the ground is only cut, down to a bank rising one block
 * per block away from the ramp, so the ramp's sides are slopes instead of walls.
 * Usage: java Ramp <region dir> x0 x1 zStart dir startY run maxRows [taperW taperE]
 */
public class Ramp {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int x0 = Integer.parseInt(a[1]), x1 = Integer.parseInt(a[2]), zs = Integer.parseInt(a[3]), dir = Integer.parseInt(a[4]);
        int startY = Integer.parseInt(a[5]), run = Integer.parseInt(a[6]), rows = Integer.parseInt(a[7]);
        int taperW = a.length > 8 ? Integer.parseInt(a[8]) : 0, taperE = a.length > 9 ? Integer.parseInt(a[9]) : 0;
        Canvas c = new Canvas(0, 0);
        c.region = r;
        int cleared = 0, filled = 0;
        for (int x = x0 - taperW; x <= x1 + taperE; x++) {
            int side = x < x0 ? x0 - x : x > x1 ? x - x1 : 0; // beside the ramp the cut rises one per block: a bank, not a wall
            for (int k = 0; k < rows; k++) {
                int z = zs + dir * k;
                int target = startY + k / run + side;
                int g = r.top(x, z, s -> {
                    String n = Region.name(s).replace("minecraft:", "");
                    return Export.natural(s) && !n.endsWith("leaves") && !BorderHills.plant(n) && !n.equals("water");
                });
                if (g == Integer.MIN_VALUE) continue;
                if (side > 0 && g <= target) continue; // the bank is lower than the ground there: nothing to cut
                if (g <= target && k > 0) {
                    if (g < target - 1) { // a dip: fill it up
                        for (int y = g + 1; y < target; y++) c.set(x, y, z, "dirt");
                        c.set(x, target, z, "grass_block");
                        filled++;
                        continue;
                    }
                    break; // met the hillside
                }
                if (g > target) {
                    c.clear(x, target + 1, z, x, g + 12, z, "earth over the ramp out of the spawn plots");
                    cleared++;
                }
                double n = BorderHills.hash(x, z);
                c.ground(x, target, z, x, target, z, n < 0.6 ? "grass_block" : n < 0.85 ? "lime_terracotta" : "green_concrete_powder");
            }
        }
        System.err.println("ramp: " + cleared + " columns cut, " + filled + " filled");
        c.print(new String[0]);
    }
}
