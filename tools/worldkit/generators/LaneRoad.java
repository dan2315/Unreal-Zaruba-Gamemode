import java.nio.file.*;

/**
 * A straight paved road along x in the style of the main road by the BLUE base: a worn brown edge (rooted dirt, podzol,
 * coarse dirt, dripstone), a yellow terracotta edge line, grey lanes (stone, andesite, cut andesite, 
 * limestone, dead fire coral and dripstone patches), a white dashed centre line of bone blocks (3 on, 4 off, a few worn
 * away), a yellow line and a brown edge on the other side.
 * Surface at y over x xWest..xEast, z z0..z1. West of xRise the surface is one higher (y+1), with a band of bottom slabs
 * 2 wide at xRise..xRise+1 so vehicles roll up the step. Everything standing on the road is cleared (needs
 * `apply <name> clear`), holes under it are filled with dirt, and existing road blocks already there are kept.
 * Usage: java LaneRoad <region dir> xWest xEast z0 z1 y xRise
 */
public class LaneRoad {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int xw = Integer.parseInt(a[1]), xe = Integer.parseInt(a[2]), z0 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        int y = Integer.parseInt(a[5]), xRise = Integer.parseInt(a[6]);
        Canvas c = new Canvas(0, 0);
        c.region = r;
        int width = z1 - z0 + 1;
        for (int x = xw; x <= xe; x++) for (int z = z0; z <= z1; z++) {
            int i = z - z0;
            int sy = x < xRise ? y + 1 : y;
            boolean slab = x >= xRise && x <= xRise + 1;
            boolean centre = width >= 9 && (i == width / 2 || width % 2 == 0 && i == width / 2 - 1);
            String surface = i == 0 || i == width - 1 ? edge(x, z)
                    : (i == 1 || i == width - 2) && BorderHills.hash(x * 3, z * 5) < 0.9 ? "yellow_terracotta"
                    : centre && Math.floorMod(x, 7) < 3 && BorderHills.hash(x * 5, z * 7) < 0.88 ? "bone_block" : lane(x, z);
            // top of the natural ground or of what is there (road blocks count as ground: they are kept)
            int g = r.top(x, z, s -> {
                String n = Region.name(s).replace("minecraft:", "");
                return !n.endsWith("leaves") && !BorderHills.plant(n) && !n.equals("water");
            });
            if (g == Integer.MIN_VALUE) continue;
            if (g > sy) c.clear(x, sy + 1, z, x, Math.max(g, sy) + 8, z, "earth and plants on the road");
            else c.clear(x, sy + 1, z, x, sy + 4, z, "plants on the road");
            for (int yy = g + 1; yy < sy; yy++) c.set(x, yy, z, "dirt");
            if (g >= sy) c.ground(x, sy, z, x, sy, z, surface);
            else c.set(x, sy, z, surface);
            if (slab) c.set(x, sy + 1, z, "smooth_stone_slab[type=bottom,waterlogged=false]");
        }
        c.print(new String[0]);
    }

    static String edge(int x, int z) {
        double n = BorderHills.hash(x * 7 + 1, z * 11 + 2);
        return n < 0.35 ? "rooted_dirt" : n < 0.6 ? "podzol" : n < 0.8 ? "coarse_dirt" : n < 0.9 ? "create:cut_dripstone" : "dripstone_block";
    }

    static String lane(int x, int z) {
        double n = BorderHills.hash(x * 13 + 5, z * 17 + 3);
        return n < 0.33 ? "stone" : n < 0.63 ? "andesite" : n < 0.8 ? "create:cut_andesite" : n < 0.86 ? "create:limestone"
                : n < 0.93 ? "dead_fire_coral_block" : "create:cut_dripstone";
    }
}
