import java.nio.file.*;
import java.util.*;

/**
 * Places blueprints from the library into a build file (additive, like every build). Each placement:
 * blueprint, the world position its ground layer's north-west corner goes to (before turning), quarter turns clockwise.
 * With --foundation <block> and --region <dir>, the cells of the lowest solid layer are carried down with that block to
 * the ground below them (houses on uneven ground). Several placements go into one build, so one undo removes them all.
 * Usage: java Stamp [--region <dir>] [--foundation <block>] <bp> <x> <groundY> <z> <quarters> [<bp> <x> <groundY> <z> <quarters> ...]
 */
public class Stamp {
    public static void main(String[] a) throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList(a));
        Region region = null;
        String foundation = null;
        int i = args.indexOf("--region");
        if (i >= 0) { region = new Region(Path.of(args.get(i + 1))); args.remove(i); args.remove(i); }
        i = args.indexOf("--foundation");
        if (i >= 0) { foundation = args.get(i + 1); args.remove(i); args.remove(i); }
        List<String> passthrough = new ArrayList<>();
        for (Iterator<String> it = args.iterator(); it.hasNext(); ) { String s = it.next(); if (s.startsWith("--")) { passthrough.add(s); it.remove(); } }
        Canvas c = new Canvas(0, 0);
        for (int k = 0; k + 4 < args.size(); k += 5) {
            Blueprint bp = Blueprint.load(Path.of(args.get(k))).rotated(Integer.parseInt(args.get(k + 4)));
            int ox = Integer.parseInt(args.get(k + 1)), gy = Integer.parseInt(args.get(k + 2)), oz = Integer.parseInt(args.get(k + 3));
            stamp(c, bp, ox, gy, oz, region, foundation);
            System.err.println("stamped " + bp.name + " at " + ox + " " + gy + " " + oz + " (" + bp.w + "x" + bp.h + "x" + bp.d + ")");
        }
        c.print(passthrough.toArray(new String[0]));
    }

    /** The blueprint's ground layer lands on y = groundY, its north-west corner on (ox, oz). */
    static void stamp(Canvas c, Blueprint bp, int ox, int groundY, int oz, Region region, String foundation) throws Exception {
        int baseY = groundY - bp.ground;
        for (int y = 0; y < bp.h; y++) for (int z = 0; z < bp.d; z++) for (int x = 0; x < bp.w; x++) {
            String s = bp.at(x, y, z);
            if (s != null) c.set(ox + x, baseY + y, oz + z, s);
        }
        if (foundation == null || region == null) return;
        // the lowest solid cell of each column, carried down to the ground
        for (int z = 0; z < bp.d; z++) for (int x = 0; x < bp.w; x++) {
            int low = -1;
            for (int y = 0; y < bp.h; y++) if (bp.at(x, y, z) != null) { low = y; break; }
            if (low < 0 || low > bp.ground + 1) continue;
            int wy = baseY + low - 1;
            for (int n = 0; n < 24; n++, wy--) {
                String s = region.state(ox + x, wy, oz + z);
                String nm = Region.name(s);
                if (!Region.isAir(s) && !nm.endsWith("water") && !nm.endsWith("grass") && !nm.endsWith("fern") && !nm.contains("flower")) break;
                c.set(ox + x, wy, oz + z, foundation);
            }
        }
    }
}
