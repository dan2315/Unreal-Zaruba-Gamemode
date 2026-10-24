import java.nio.file.*;
import java.util.*;

/**
 * The standard vehicle spawn plot, in a row of `count` next to each other. One plot:
 *   - a 30x30 pad (the largest vehicle the rules allow is 30x30x30) bordered with TFMG caution blocks;
 *   - its vehicle purchase block 3 blocks in front of the pad, in the middle, facing away from it: the vehicle spawns
 *     on the pad (the block's gap of 2 is the 2 blocks between it and the pad);
 *   - a scaffold gantry on each side (TFMG steel frames, createdeco catwalk decks at 4 heights, railings on the outer
 *     side and the ends, ladders at the front) so players can board a tall vehicle from any level. Neighbouring pads
 *     share the gantry between them;
 *   - a paved apron in front of the row, where all the purchase blocks are.
 * Local frame: x across the row (pads at 0..29, next pad 35 further), z away from the terminals (pad at 0..29),
 * y = ground layer G. The frame is placed so that vehicles spawn east (pads in a column going south) or south (pads in
 * a row going east). With --region and --cut, terrain standing above G over the plots is listed as clears (hills);
 * holes below G are filled with dirt.
 * Usage: java SpawnPlots <east|south> <padX0> <G> <padZ0> <count> <red|blue> [--region <dir> --cut]
 *   padX0/padZ0 = the world corner of the first pad with the smallest x and z.
 */
public class SpawnPlots {
    static final int PAD = 30, STEP = 35, GANTRY_LEFT = -4;
    static final int[] DECKS = {4, 9, 14, 19};
    static String dir;
    static int X0, Z0, G;
    static Canvas c;

    public static void main(String[] a) throws Exception {
        dir = a[0];
        X0 = Integer.parseInt(a[1]); G = Integer.parseInt(a[2]); Z0 = Integer.parseInt(a[3]);
        int count = Integer.parseInt(a[4]);
        String style = a[5];
        List<String> rest = Arrays.asList(a).subList(6, a.length);
        int ri = rest.indexOf("--region");
        Region region = ri >= 0 ? new Region(Path.of(rest.get(ri + 1))) : null;
        boolean cut = rest.contains("--cut");
        regionRef = region;
        c = new Canvas(0, 0);

        int width = (count - 1) * STEP + PAD; // local x 0..width-1 are pads and shared gantries
        int lx0 = GANTRY_LEFT, lx1 = width + 3, lz0 = -7, lz1 = PAD - 1;

        // site: fill holes under everything down to the ground, cut what stands above (listed as clears)
        for (int lx = lx0; lx <= lx1; lx++) for (int lz = lz0; lz <= lz1; lz++) {
            int[] w = world(lx, lz);
            if (region == null) c.fill(w[0], G - 6, w[1], w[0], G - 1, w[1], "dirt");
            else for (int y = G - 6; y < G; y++) { String s = region.state(w[0], y, w[1]); if (Region.isAir(s) || Region.name(s).endsWith("water")) c.set(w[0], y, w[1], "dirt"); }
            if (cut && region != null) {
                int top = region.top(w[0], w[1], SpawnPlots::solid);
                if (top > G) c.clear(w[0], G + 1, w[1], w[0], Math.min(top + 6, G + 31), w[1], "terrain over the spawn plots");
            }
        }

        // apron in front of the row
        for (int lx = lx0; lx <= lx1; lx++) for (int lz = lz0; lz <= -1; lz++) ground(lx, G, lz, apron(style, lx, lz));
        for (int p = 0; p < count; p++) {
            int px = p * STEP;
            // pad
            for (int x = 0; x < PAD; x++) for (int z = 0; z < PAD; z++) {
                boolean border = x == 0 || z == 0 || x == PAD - 1 || z == PAD - 1;
                ground(px + x, G, z, border ? "tfmg:yellow_caution_block[facing=south]" : pad(style, px + x, z));
            }
            // terminal: the purchase block faces the apron, the pad is behind it
            set(px + PAD / 2 - 1, G + 1, -3, "unrealzaruba:vehicle_purchase_block[facing=" + dirName("north") + "]");
            ground(px + PAD / 2 - 1, G, -3, "tfmg:yellow_caution_block[facing=south]");
            // lamp posts on both sides of the terminal
            for (int dx : new int[] {-3, 3}) {
                for (int y = G + 1; y <= G + 3; y++) set(px + PAD / 2 - 1 + dx, y, -3, "tfmg:steel_frame[waterlogged=false]");
                set(px + PAD / 2 - 1 + dx, G + 4, -3, "minecraft:lantern[hanging=false,waterlogged=false]");
            }
        }
        // gantries: the outer two and one between each pair of pads
        for (int p = 0; p <= count; p++) {
            int gx = p == 0 ? GANTRY_LEFT : p * STEP - 4;
            String side = p == 0 ? "west" : p == count ? "east" : null;
            gantry(gx, side);
        }
        c.print(new String[0]);
    }

    /** A 3-wide gantry at local x gx..gx+2 along the whole pad; railings on `outer` (west/east in the local frame) and the ends. */
    static void gantry(int gx, String outer) {
        int[] cols = {0, 9, 19, PAD - 1};
        int top = G + DECKS[DECKS.length - 1];
        for (int z : cols) for (int x : new int[] {gx, gx + 2}) for (int y = G + 1; y <= top; y++) set(x, y, z, "tfmg:steel_frame[waterlogged=false]");
        for (int d : DECKS) {
            int y = G + d;
            for (int x = gx; x <= gx + 2; x++) for (int z = 0; z < PAD; z++) {
                final int zz = z; boolean column = (x == gx || x == gx + 2) && Arrays.stream(cols).anyMatch(cz -> cz == zz) && y <= top;
                if (!column) set(x, y, z, "createdeco:industrial_iron_catwalk[bottom=false,waterlogged=false]");
            }
            for (int x = gx; x <= gx + 2; x++) for (int z = 0; z < PAD; z++) {
                boolean outerEdge = outer != null && x == (outer.equals("west") ? gx : gx + 2);
                boolean front = z == 0 && x != gx, back = z == PAD - 1;
                if (!outerEdge && !front && !back) continue;
                final int rz = z; if ((x == gx || x == gx + 2) && Arrays.stream(cols).anyMatch(cz -> cz == rz) && y + 1 < top) continue; // the column goes on up there
                Map<String, Boolean> rail = new TreeMap<>(Map.of("north", front, "south", back, "east", outerEdge && outer.equals("east"), "west", outerEdge && outer.equals("west")));
                StringBuilder s = new StringBuilder("createdeco:industrial_iron_catwalk_railing[");
                Map<String, Boolean> world = new TreeMap<>();
                rail.forEach((k, v) -> world.put(dirName(k), v));
                world.forEach((k, v) -> s.append(k).append('=').append(v).append(','));
                s.append("waterlogged=false]");
                set(x, y + 1, z, s.toString());
            }
        }
        // ladder at the front of the outer-left column, from the ground to the top deck
        for (int y = G + 1; y <= top; y++) set(gx, y, -1, "createdeco:industrial_iron_ladder[facing=" + dirName("north") + ",waterlogged=false]");
    }

    /** Terrain and trees count as in the way; plants (grass, ferns, flowers) do not: the pad replaces them anyway. */
    static boolean solid(String state) {
        String n = Region.name(state).replace("minecraft:", "");
        if (n.endsWith("leaves")) return false;
        return !(n.contains("grass") && !n.equals("grass_block")) && !n.contains("fern") && !n.contains("flower") && !n.contains("tulip")
                && !n.equals("poppy") && !n.equals("dandelion") && !n.equals("cornflower") && !n.equals("oxeye_daisy") && !n.equals("azure_bluet")
                && !n.equals("allium") && !n.equals("lily_of_the_valley") && !n.contains("bush") && !n.equals("vine") && !n.contains("petals")
                && !n.equals("lilac") && !n.equals("peony") && !n.equals("sunflower") && !n.equals("snow");
    }

    static String apron(String style, int x, int z) {
        double n = noise(x, z);
        if (style.equals("blue")) return n < 0.55 ? "cobblestone" : n < 0.85 ? "polished_andesite" : "andesite";
        return n < 0.45 ? "gravel" : n < 0.75 ? "andesite" : n < 0.9 ? "coarse_dirt" : "cobblestone";
    }

    static String pad(String style, int x, int z) {
        double n = noise(x * 3 + 7, z * 3 + 11);
        if (style.equals("blue")) return n < 0.6 ? "polished_andesite" : n < 0.85 ? "andesite" : "cobblestone";
        return n < 0.5 ? "polished_andesite" : n < 0.8 ? "andesite" : "gravel";
    }

    static double noise(int x, int z) {
        long h = x * 73856093L ^ z * 19349663L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h >>> 8) & 0xFFFF) / 65536.0;
    }

    /** Local -> world column. east: local x runs along +z, local z along +x. south: local x along +x, local z along +z. */
    static int[] world(int lx, int lz) {
        return dir.equals("east") ? new int[] {X0 + lz, Z0 + lx} : new int[] {X0 + lx, Z0 + lz};
    }

    /** A direction named in the local frame (north = toward the terminals), as the world direction. */
    static String dirName(String local) {
        if (!dir.equals("east")) return local;
        return switch (local) {
            case "north" -> "west";
            case "south" -> "east";
            case "west" -> "north";
            case "east" -> "south";
            default -> local;
        };
    }

    static void set(int lx, int y, int lz, String block) {
        int[] w = world(lx, lz);
        c.set(w[0], y, w[1], block);
    }

    static Region regionRef;
    static final Set<String> SURFACE = new HashSet<>(Arrays.asList("lime_terracotta", "green_concrete_powder", "diorite", "granite", "andesite", "stone",
            "gravel", "moss_block", "cobblestone", "mossy_cobblestone", "tuff", "calcite", "clay", "sand", "coarse_dirt", "podzol", "rooted_dirt", "dirt", "grass_block", "dirt_path"));

    /** Ground: replaces the natural surface there (the map paints grass with terracotta and concrete powder, which the ground rule does not cover). */
    static void ground(int lx, int y, int lz, String block) throws java.io.IOException {
        int[] w = world(lx, lz);
        if (regionRef != null) {
            String s = regionRef.state(w[0], y, w[1]);
            String n = Region.name(s);
            if (SURFACE.contains(n.replace("minecraft:", "")) && !n.equals(Region.name(Canvas.id(block)))) { c.swap(w[0], y, w[1], n, block); return; }
        }
        c.ground(w[0], y, w[1], w[0], y, w[1], block);
    }
}
