import java.nio.file.*;
import java.util.*;

/**
 * A farmstead (khutor) around a yard centred at (cx, cz): the imported windmill on the north side, sails toward the yard,
 * two izbas from the rural library facing the yard from the west and the east, a well in the middle, haystacks, a log
 * pile, a trodden yard (coarse dirt and path patches), and a fenced wheat and carrot field south of the yard with a gate.
 * Every piece is set on its own ground (Props.place) and is skipped if its footprint is not free; the whole layout can
 * be turned (q quarter turns clockwise). Prints the gate position on stderr: lay a Pathway trail from there to a road.
 * Usage: java Farmstead <region> <rural dir> cx cz q [designA designB] [--no-mill]
 */
public class Farmstead {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        Path rural = Path.of(a[1]);
        int cx = Integer.parseInt(a[2]), cz = Integer.parseInt(a[3]), q = Integer.parseInt(a[4]);
        List<String> rest = new ArrayList<>(Arrays.asList(a).subList(5, a.length));
        boolean mill = !rest.remove("--no-mill");
        String da = rest.size() > 0 ? rest.get(0) : "izba_spruce", db = rest.size() > 1 ? rest.get(1) : "izba_oak_log";
        Canvas c = new Canvas(0, 0);
        c.region = r;
        long seed = cx * 31L + cz;
        List<String> report = new ArrayList<>();
        // local frame: yard centre (0, 0), north = -z; turned by q around the centre. The windmill is the imported
        // molino (props/windmill_molino.bp, 17x16, door and sails to the south) on the north side of the yard
        if (mill) piece(c, r, Blueprint.load(rural.resolveSibling("props").resolve("windmill_molino.bp")), -8, -24, 0, q, cx, cz, report);
        Blueprint ha = house(rural, da, 1), hb = house(rural, db, 3);
        piece(c, r, ha, -7 - ha.w, 2 - ha.d / 2, 1, q, cx, cz, report);   // door east, west of the yard
        piece(c, r, hb, 7, 2 - hb.d / 2, 3, q, cx, cz, report);           // door west, east of the yard
        piece(c, r, Props.design("well", seed), -2, -2, 0, q, cx, cz, report);
        piece(c, r, Props.design("haystack", seed + 1), 9, -16, 0, q, cx, cz, report);
        piece(c, r, Props.design("haystack", seed + 2), 12, -21, 0, q, cx, cz, report);
        piece(c, r, Props.design("haystack", seed + 3), -14, 13, 0, q, cx, cz, report);
        piece(c, r, Props.design("logpile", seed), -17, -17, 0, q, cx, cz, report);
        // trodden yard
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 12; z++) {
            double h = BorderHills.hash((int) (x + seed), z * 7 + 3), dist = Math.hypot(x, z * 0.9) / 11.0;
            if (h > 0.65 * (1.1 - dist)) continue;
            int[] w = turn(x, z, q, cx, cz);
            int gy = top(r, w[0], w[1]);
            if (gy == Integer.MIN_VALUE || c.cells.containsKey(Canvas.key(w[0], gy, w[1]))) continue; // not under the well and the houses
            c.ground(w[0], gy, w[1], w[0], gy, w[1], h < 0.25 ? "dirt_path" : h < 0.45 ? "coarse_dirt" : "rooted_dirt");
        }
        // the field: rows of wheat and carrots south of the yard, an oak fence round it with a gate on the yard side
        int fx0 = -14, fx1 = 14, fz0 = 18, fz1 = 34;
        for (int x = fx0; x <= fx1; x++) for (int z = fz0; z <= fz1; z++) {
            int[] w = turn(x, z, q, cx, cz);
            int gy = top(r, w[0], w[1]);
            if (gy == Integer.MIN_VALUE) continue;
            boolean edge = x == fx0 || x == fx1 || z == fz0 || z == fz1;
            if (edge) {
                if (z == fz0 && Math.abs(x) <= 1) continue; // gate
                c.set(w[0], gy + 1, w[1], "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
                continue;
            }
            boolean furrow = Math.floorMod(x, 4) == 0;
            if (furrow) { c.ground(w[0], gy, w[1], w[0], gy, w[1], "coarse_dirt"); continue; }
            c.ground(w[0], gy, w[1], w[0], gy, w[1], "farmland[moisture=7]");
            int age = 4 + (int) (BorderHills.hash(x * 3, z * 5) * 4);
            c.set(w[0], gy + 1, w[1], (z > (fz0 + fz1) / 2 ? "carrots" : "wheat") + "[age=" + age + "]");
        }
        // fence posts join up: give the fence its connections along the edge
        for (var e : c.cells.entrySet()) {
            String b = e.getValue().block();
            if (!b.startsWith("minecraft:oak_fence")) continue;
            long k = e.getKey();
            int x = Canvas.x(k), y = Canvas.y(k), z = Canvas.z(k);
            StringBuilder s = new StringBuilder("minecraft:oak_fence[");
            for (String[] side : new String[][] {{"east", "1", "0"}, {"north", "0", "-1"}, {"south", "0", "1"}, {"west", "-1", "0"}}) {
                boolean join = false;
                for (int dy = -1; dy <= 1; dy++) {
                    Canvas.Cell o = c.cells.get(Canvas.key(x + Integer.parseInt(side[1]), y + dy, z + Integer.parseInt(side[2])));
                    join |= o != null && o.block().startsWith("minecraft:oak_fence") && dy == 0;
                }
                s.append(side[0]).append('=').append(join).append(side[0].equals("south") ? ",waterlogged=false," : ",");
            }
            s.setLength(s.length() - 1);
            e.setValue(new Canvas.Cell(s.append(']').toString(), e.getValue().filter(), null));
        }
        int[] gate = turn(0, fz0 - 2, q, cx, cz);
        report.forEach(System.err::println);
        System.err.println("farmstead at " + cx + " " + cz + "; gate " + gate[0] + " " + gate[1] + "; lay the trail from the yard's other side");
        c.print(new String[0]);
    }

    static Blueprint house(Path rural, String name, int door) throws Exception {
        RuralInfill.House h = RuralInfill.orient(Blueprint.load(rural.resolve(name + ".bp")), door);
        if (h == null) throw new IllegalStateException(name + ": no door found");
        return h.bp();
    }

    /** A piece whose local footprint corner is (lx, lz), turned with the layout; skipped if its ground is not free. */
    static void piece(Canvas c, Region r, Blueprint bp, int lx, int lz, int ownQ, int q, int cx, int cz, List<String> report) throws Exception {
        Blueprint t = bp.rotated(q);
        // the footprint's corners after turning the layout; the new north-west corner
        int[] p0 = turn(lx, lz, q, cx, cz), p1 = turn(lx + bp.w - 1, lz + bp.d - 1, q, cx, cz);
        int ox = Math.min(p0[0], p1[0]), oz = Math.min(p0[1], p1[1]);
        boolean ok = Props.place(c, r, t, ox, oz);
        report.add((ok ? "placed " : "SKIPPED (not free) ") + bp.name + " at " + ox + " " + oz);
    }

    /** Local (x, z) turned q quarters clockwise about the yard centre, to world coordinates. */
    static int[] turn(int x, int z, int q, int cx, int cz) {
        for (int k = 0; k < Math.floorMod(q, 4); k++) { int nx = -z; z = x; x = nx; }
        return new int[] {cx + x, cz + z};
    }

    static int top(Region r, int x, int z) throws Exception {
        for (int y = 90; y >= 16; y--) {
            String s = r.state(x, y, z), n = Region.name(s).replace("minecraft:", "");
            if (Region.isAir(s) || BorderHills.plant(n)) continue;
            if (!Export.natural(s) || n.endsWith("leaves") || Tidy.log(n) || n.equals("water")) return Integer.MIN_VALUE;
            return y;
        }
        return Integer.MIN_VALUE;
    }
}
