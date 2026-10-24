import java.nio.file.*;
import java.util.*;

/**
 * Refills the garbage fields of the city (east), whose bottom layer of junk was a block of a removed mod and turned to
 * air: the piles float. In each box, on columns whose ground is field ground (dirt, podzol, soul soil, coarse dirt,
 * moss, stripped spruce wood), the air under the lowest pile block is filled down to the ground, and elsewhere a thin,
 * patchy layer of junk (one block, sometimes two) is laid, so the field reads as a dump again. Few blocks: gravel,
 * cobbled deepslate, tuff, soul soil, coarse dirt, andesite casing. Roads and anything else are left alone.
 * Usage: java GarbageFill <region dir> <groundY> x0 z0 x1 z1 [x0 z0 x1 z1 ...]
 */
public class GarbageFill {
    static final Set<String> FIELD = new HashSet<>(Arrays.asList("dirt", "podzol", "soul_soil", "coarse_dirt", "moss_block",
            "stripped_spruce_wood", "rooted_dirt", "grass_block", "mud", "packed_mud"));

    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int g = Integer.parseInt(a[1]);
        Canvas c = new Canvas(0, 0);
        int filled = 0, layered = 0;
        for (int i = 2; i + 3 < a.length; i += 4) {
            int x0 = Integer.parseInt(a[i]), z0 = Integer.parseInt(a[i + 1]), x1 = Integer.parseInt(a[i + 2]), z1 = Integer.parseInt(a[i + 3]);
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                String ground = Region.name(r.state(x, g, z)).replaceAll("^[a-z_]+:", "");
                if (!FIELD.contains(ground)) continue;
                int lowest = -1;
                for (int y = g + 1; y <= g + 8; y++) {
                    String s = r.state(x, y, z);
                    if (!Region.isAir(s) && !BorderHills.plant(Region.name(s).replace("minecraft:", ""))) { lowest = y; break; }
                }
                if (lowest > g + 1) {
                    for (int y = g + 1; y < lowest; y++) c.set(x, y, z, junk(x, y, z));
                    filled++;
                } else if (lowest < 0) {
                    double n = BorderHills.valueNoise(x / 6.0, z / 6.0, 11);
                    if (n < 0.35) continue;
                    c.set(x, g + 1, z, junk(x, g + 1, z));
                    if (n > 0.75) c.set(x, g + 2, z, junk(x, g + 2, z));
                    layered++;
                }
            }
        }
        System.err.println("filled under piles: " + filled + " columns, thin layer: " + layered + " columns");
        c.print(new String[0]);
    }

    static String junk(int x, int y, int z) {
        double n = BorderHills.hash(x * 3 + y * 101, z * 7 - y * 13);
        return n < 0.25 ? "gravel" : n < 0.45 ? "cobbled_deepslate" : n < 0.6 ? "tuff" : n < 0.75 ? "soul_soil"
                : n < 0.9 ? "coarse_dirt" : "create:andesite_casing";
    }
}
