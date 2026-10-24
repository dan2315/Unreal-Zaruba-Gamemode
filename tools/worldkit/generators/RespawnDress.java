import java.nio.file.*;
import java.util.*;

/**
 * Dresses an open respawn plaza in the BLUE base's style (pieces copied from the BLUE base, blueprints in bases/):
 *   - the BLUE flag (blue_flag: stone brick pedestal, spruce mast, blue wool flag) at the old flag pole, turned so the
 *     flag flies over the plaza; the old pole is cleared;
 *   - four BLUE street lamps (blue_street_lamp) north and south of the plaza, their arms reaching over it;
 *   - a low wall round the plaza one block outside it (cut limestone wall between tuff pillar posts with limestone slab
 *     caps, the BLUE base's fence materials), with openings where the paths come in and a wide one towards the
 *     vehicle pad, so the way from the respawn spot to the purchase block and the pad stays free;
 *   - two benches of Create brown seats (BLUE canteen seats) in the east corners.
 * Plaza: x0..x1, z0..z1 on ground layer g; the old flag pole column at (fx, fz). Openings are given per side as
 * from..to along that side. Needs `apply <name> clear` for the old pole.
 * Usage: java RespawnDress <region> <bases dir> g x0 z0 x1 z1 fx fz
 *        --west a b --north a b --south a b --east a b
 */
public class RespawnDress {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        Path bases = Path.of(a[1]);
        int g = Integer.parseInt(a[2]), x0 = Integer.parseInt(a[3]), z0 = Integer.parseInt(a[4]), x1 = Integer.parseInt(a[5]), z1 = Integer.parseInt(a[6]);
        int fx = Integer.parseInt(a[7]), fz = Integer.parseInt(a[8]);
        Map<String, int[]> open = new HashMap<>();
        for (int i = 9; i + 2 < a.length; i += 3) open.put(a[i].substring(2), new int[] {Integer.parseInt(a[i + 1]), Integer.parseInt(a[i + 2])});
        Canvas c = new Canvas(0, 0);

        // the old flag pole: fence column and its banner
        int top = g + 1;
        while (top < g + 20 && !Region.isAir(r.state(fx, top + 1, fz))) top++;
        c.clear(fx, g + 1, fz, fx, top, fz, "old flag pole with a white banner");

        // the BLUE flag, mast on the old pole's spot, flag flying east (three quarter turns)
        Blueprint flag = Blueprint.load(bases.resolve("blue_flag.bp")).rotated(3);
        int[] mast = column(flag, "spruce_fence");
        Stamp.stamp(c, flag, fx - mast[0], g, fz - mast[1], null, null);

        // street lamps: north side arms pointing south, south side arms pointing north
        Blueprint lamp = Blueprint.load(bases.resolve("blue_street_lamp.bp"));
        int lx0 = x0 + 3, lx1 = x1 - 1;
        for (int lx : new int[] {lx0, lx1}) {
            Blueprint south = lamp.rotated(1), north = lamp.rotated(3);
            int[] ps = column(south, "cut_limestone_wall"), pn = column(north, "cut_limestone_wall");
            Stamp.stamp(c, south, lx - ps[0], g, z0 - 2 - ps[1], null, null);
            Stamp.stamp(c, north, lx - pn[0], g, z1 + 2 - pn[1], null, null);
        }

        // low wall one block outside the plaza
        int wx0 = x0 - 1, wx1 = x1 + 1, wz0 = z0 - 1, wz1 = z1 + 1;
        Set<Long> wall = new HashSet<>(), posts = new HashSet<>();
        for (int x = wx0; x <= wx1; x++) for (int z = wz0; z <= wz1; z++) {
            boolean edge = x == wx0 || x == wx1 || z == wz0 || z == wz1;
            if (!edge) continue;
            String side = x == wx0 ? "west" : x == wx1 ? "east" : z == wz0 ? "north" : "south";
            int along = side.equals("west") || side.equals("east") ? z : x;
            int[] o = open.get(side);
            boolean corner = (x == wx0 || x == wx1) && (z == wz0 || z == wz1);
            if (!corner && o != null && along >= o[0] && along <= o[1]) continue;
            boolean openingEnd = !corner && o != null && (along == o[0] - 1 || along == o[1] + 1);
            boolean everyFour = !corner && Math.floorMod(along - (side.equals("west") || side.equals("east") ? wz0 : wx0), 4) == 0;
            if (corner || openingEnd || everyFour) posts.add(Canvas.key(x, 0, z));
            else wall.add(Canvas.key(x, 0, z));
        }
        for (long k : posts) {
            int x = Canvas.x(k), z = Canvas.z(k);
            c.set(x, g + 1, z, "create:tuff_pillar[axis=y]");
            c.set(x, g + 2, z, "create:tuff_pillar[axis=y]");
            c.set(x, g + 3, z, "create:cut_limestone_slab[type=bottom,waterlogged=false]");
        }
        for (long k : wall) {
            int x = Canvas.x(k), z = Canvas.z(k);
            String s = "create:cut_limestone_wall[east=" + side(wall, posts, x + 1, z) + ",north=" + side(wall, posts, x, z - 1)
                    + ",south=" + side(wall, posts, x, z + 1) + ",up=false,waterlogged=false,west=" + side(wall, posts, x - 1, z) + "]";
            c.set(x, g + 1, z, s);
        }

        // benches in the east corners, facing the plaza
        for (int x = x1 - 4; x <= x1 - 2; x++) {
            c.set(x, g + 1, z0 + 1, "create:brown_seat[waterlogged=false]");
            c.set(x, g + 1, z1 - 1, "create:brown_seat[waterlogged=false]");
        }
        System.err.println("respawn dress: posts " + posts.size() + ", wall " + wall.size());
        c.print(new String[0]);
    }

    static String side(Set<Long> wall, Set<Long> posts, int x, int z) {
        long k = Canvas.key(x, 0, z);
        return wall.contains(k) || posts.contains(k) ? "low" : "none";
    }

    /** (x, z) of the blueprint column with the most blocks whose name contains `part` (the mast, the lamp pole). */
    static int[] column(Blueprint bp, String part) {
        int best = -1, bx = 0, bz = 0;
        for (int x = 0; x < bp.w; x++) for (int z = 0; z < bp.d; z++) {
            int n = 0;
            for (int y = 0; y < bp.h; y++) if (bp.at(x, y, z) != null && bp.at(x, y, z).contains(part)) n++;
            if (n > best) { best = n; bx = x; bz = z; }
        }
        return new int[] {bx, bz};
    }
}
