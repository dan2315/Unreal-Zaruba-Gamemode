import java.nio.file.*;
import java.util.*;

/**
 * Placing structures on the ground (Props.place: set on the highest ground under the footprint, earth or cobblestone
 * under lower columns, skipped when the footprint is not open natural ground), and a few small pieces drawn in code
 * (well, haystack, logpile, used by Farmstead). The other drawn designs (windmill, watchtower, chapel_ruin, car_wreck,
 * bus_stop) are placeholders the user found weak: prefer imported structures (ImportNbt) from the library.
 * Each design is a blueprint whose front (door, sails, bench) faces south at 0 turns; layers 0-1 are the layer under
 * the ground and the ground layer (they replace natural ground: floors, the well's water), everything above is placed
 * into air like any build.
 *   save <dir>                        writes every design to <dir>/<name>.bp (the library copy)
 *   place <region> <type> x z q ...   each structure (a design name or a .bp file from the library) with its footprint's north-west corner at (x, z), turned q quarter
 *                                     turns clockwise; set on the highest ground under it, lower columns get earth
 *                                     (or cobblestone under walls) up to that level. A footprint touching builds,
 *                                     water or tree trunks is skipped (reported on stderr).
 * Usage: java Props save <dir> | java Props place <region> type x z q [type x z q ...]
 */
public class Props {
    static final String[] TYPES = {"windmill", "watchtower", "chapel_ruin", "well", "haystack", "logpile", "car_wreck", "bus_stop"};

    public static void main(String[] a) throws Exception {
        if (a[0].equals("save")) {
            Path dir = Path.of(a[1]);
            Files.createDirectories(dir);
            for (String t : TYPES) {
                Blueprint bp = design(t, 1);
                bp.save(dir.resolve(t + ".bp"));
                System.err.println(t + ": " + bp.w + "x" + bp.h + "x" + bp.d + ", " + bp.count() + " blocks");
            }
            return;
        }
        Region r = new Region(Path.of(a[1]));
        Canvas c = new Canvas(0, 0);
        c.region = r;
        int placed = 0;
        for (int i = 2; i + 3 < a.length; i += 4) {
            String type = a[i];
            int x = Integer.parseInt(a[i + 1]), z = Integer.parseInt(a[i + 2]), q = Integer.parseInt(a[i + 3]);
            Blueprint bp = (type.endsWith(".bp") ? Blueprint.load(Path.of(type)) : design(type, x * 31 + z)).rotated(q);
            if (place(c, r, bp, x, z)) placed++;
            else System.err.println("skipped " + type + " at " + x + " " + z + ": footprint not free");
        }
        System.err.println("props: " + placed + " placed");
        c.print(new String[0]);
    }

    /** Sets the design on the ground; false when its footprint is not open natural ground. */
    static boolean place(Canvas c, Region r, Blueprint bp, int ox, int oz) throws Exception {
        int[][] g = new int[bp.w][bp.d];
        int top = Integer.MIN_VALUE, low = Integer.MAX_VALUE;
        for (int x = 0; x < bp.w; x++) for (int z = 0; z < bp.d; z++) {
            boolean used = false;
            for (int y = 0; y < bp.h && !used; y++) used = bp.at(x, y, z) != null;
            int wx = ox + x, wz = oz + z, gy = Integer.MIN_VALUE;
            for (int y = 90; y >= 16; y--) {
                String s = r.state(wx, y, wz), n = Region.name(s).replace("minecraft:", "");
                if (Region.isAir(s) || BorderHills.plant(n)) continue;
                if (used && (n.equals("water") || Tidy.log(n) || n.endsWith("leaves") || !Export.natural(s))) return false;
                if (n.endsWith("leaves") || Tidy.log(n)) continue;
                gy = y;
                break;
            }
            g[x][z] = gy;
            if (used && gy != Integer.MIN_VALUE) { top = Math.max(top, gy); low = Math.min(low, gy); }
        }
        if (top == Integer.MIN_VALUE || top - low > 4) return false;
        int base = top - bp.ground; // layer `ground` lands on the top ground level
        for (int x = 0; x < bp.w; x++) for (int z = 0; z < bp.d; z++) {
            int wx = ox + x, wz = oz + z;
            int lowest = -1;
            for (int y = 0; y < bp.h; y++) if (bp.at(x, y, z) != null) { lowest = y; break; }
            if (lowest < 0) continue;
            // earth up to the pad level where the ground is lower (only under parts standing on the ground, not under
            // overhangs like sails, braces or roofs)
            if (lowest <= bp.ground + 1 && g[x][z] != Integer.MIN_VALUE && base + lowest > g[x][z] + 1) {
                String fill = lowest > bp.ground ? "cobblestone" : "dirt";
                for (int y = g[x][z] + 1; y < base + lowest; y++) c.set(wx, y, wz, fill);
            }
            for (int y = 0; y < bp.h; y++) {
                String s = bp.at(x, y, z);
                if (s == null) continue;
                if (y <= bp.ground) c.ground(wx, base + y, wz, wx, base + y, wz, s);
                else c.set(wx, base + y, wz, s);
            }
        }
        return true;
    }

    // ---------- designs

    static class D {
        final Blueprint bp;
        final long seed;
        D(int w, int h, int d, String name, long seed) { bp = new Blueprint(w, h, d); bp.name = name; bp.ground = 1; this.seed = seed; bp.tags.add("prop"); }
        void s(int x, int y, int z, String st) {
            if (x < 0 || y < 0 || z < 0 || x >= bp.w || y >= bp.h || z >= bp.d) return;
            bp.cells[y][z][x] = st == null ? -1 : bp.index(st.contains(":") ? st : "minecraft:" + st);
        }
        void box(int x0, int y0, int z0, int x1, int y1, int z1, String st) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) s(x, y, z, st);
        }
        double h(int x, int y, int z) { return BorderHills.hash((int) (x * 31 + seed), z * 17 + y * 131); }
    }

    static Blueprint design(String type, long seed) {
        return switch (type) {
            case "windmill" -> windmill(seed);
            case "watchtower" -> watchtower(seed);
            case "chapel_ruin" -> chapel(seed);
            case "well" -> well(seed);
            case "haystack" -> haystack(seed);
            case "logpile" -> logpile(seed);
            case "car_wreck" -> car(seed);
            case "bus_stop" -> busStop(seed);
            default -> throw new IllegalArgumentException("unknown prop " + type);
        };
    }

    /** Tent-roofed post mill: log-and-plank body on a cobblestone plinth, a four-sided spire, four sails at the front. */
    static Blueprint windmill(long seed) {
        D d = new D(13, 18, 12, "windmill", seed);
        int x0 = 2, x1 = 10, z0 = 1, z1 = 9; // body 9x9
        d.box(x0, 0, z0, x1, 1, z1, "cobblestone");
        d.box(x0 + 1, 1, z0 + 1, x1 - 1, 1, z1 - 1, "spruce_planks");
        for (int y = 2; y <= 2; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            if (x == x0 || x == x1 || z == z0 || z == z1) d.s(x, y, z, d.h(x, y, z) < 0.3 ? "mossy_cobblestone" : "cobblestone");
        for (int y = 3; y <= 9; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
            if (corner) d.s(x, y, z, "spruce_log[axis=y]");
            else if (x == x0 || x == x1 || z == z0 || z == z1) d.s(x, y, z, y == 6 ? "stripped_spruce_log[axis=" + (x == x0 || x == x1 ? "z" : "x") + "]" : "spruce_planks");
        }
        // door and windows
        d.s(6, 3, z1, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
        d.s(6, 4, z1, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
        d.s(5, 5, z1, "spruce_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]");
        for (int[] wpos : new int[][] {{x0, 6, 5}, {x1, 6, 5}, {6, 8, z0}, {6, 8, z1}}) d.s(wpos[0], wpos[1], wpos[2], "glass_pane[east=false,north=false,south=false,waterlogged=false,west=false]");
        // floor inside, a few sacks and barrels
        d.box(x0 + 1, 7, z0 + 1, x1 - 1, 7, z1 - 1, "spruce_planks");
        d.s(4, 3, 3, "barrel[facing=up,open=false]");
        d.s(4, 4, 3, "barrel[facing=up,open=false]");
        d.s(8, 3, 3, "hay_block[axis=y]");
        d.s(8, 3, 4, "hay_block[axis=x]");
        for (int y = 3; y <= 7; y++) d.s(9, y, z1 - 1, "ladder[facing=north,waterlogged=false]");
        // eaves and the spire
        d.box(x0 - 1, 10, z0 - 1, x1 + 1, 10, z1 + 1, null);
        for (int ring = 0; ring <= 4; ring++) {
            int a0 = x0 - 1 + ring, a1 = x1 + 1 - ring, b0 = z0 - 1 + ring, b1 = z1 + 1 - ring, y = 10 + ring;
            if (a0 > a1 || b0 > b1) break;
            for (int x = a0; x <= a1; x++) {
                d.s(x, y, b0, "spruce_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
                d.s(x, y, b1, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
            }
            for (int z = b0 + 1; z < b1; z++) {
                d.s(a0, y, z, "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
                d.s(a1, y, z, "spruce_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
            }
            if (ring < 4) d.box(a0 + 1, y, b0 + 1, a1 - 1, y, b1 - 1, "spruce_planks");
        }
        d.s(6, 15, 5, "spruce_planks");
        d.s(6, 16, 5, "spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        d.s(6, 17, 5, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        // sails: a hub on the front wall, four lattice arms with cloth on one side (a pinwheel)
        int hx = 6, hy = 8, hz = z1 + 1;
        d.s(hx, hy, hz, "stripped_spruce_log[axis=z]");
        d.s(hx, hy, hz + 1, "stripped_spruce_log[axis=z]");
        int sz = hz + 1;
        for (int k = 1; k <= 6; k++) {
            d.s(hx, hy + k, sz, "dark_oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
            if (hy - k >= 2) d.s(hx, hy - k, sz, "dark_oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
            d.s(hx - k, hy, sz, "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
            d.s(hx + k, hy, sz, "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
            if (k >= 2) {
                d.s(hx + 1, hy + k, sz, "white_wool");
                if (hy - k >= 2) d.s(hx - 1, hy - k, sz, "white_wool");
                d.s(hx - k, hy + 1, sz, "white_wool");
                d.s(hx + k, hy - 1, sz, "white_wool");
            }
        }
        return d.bp;
    }

    /** Wooden observation tower: four log legs, braces, a fenced platform reached by a ladder, a slab roof. */
    static Blueprint watchtower(long seed) {
        D d = new D(7, 15, 8, "watchtower", seed);
        int top = 10;
        for (int[] leg : new int[][] {{0, 0}, {5, 0}, {0, 5}, {5, 5}}) {
            d.s(leg[0], 0, leg[1], "cobblestone");
            d.s(leg[0], 1, leg[1], "cobblestone");
            for (int y = 2; y <= top + 3; y++) d.s(leg[0], y, leg[1], y <= top ? "spruce_log[axis=y]" : "spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        }
        for (int y : new int[] {4, 8}) {
            for (int x = 1; x <= 4; x++) { d.s(x, y, 0, "stripped_spruce_log[axis=x]"); d.s(x, y, 5, "stripped_spruce_log[axis=x]"); }
            for (int z = 1; z <= 4; z++) { d.s(0, y, z, "stripped_spruce_log[axis=z]"); d.s(5, y, z, "stripped_spruce_log[axis=z]"); }
        }
        d.box(0, top, 0, 5, top, 5, "spruce_planks");
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 5; z++)
            if ((x == 0 || x == 5 || z == 0 || z == 5) && !(x == 2 && z == 5)) d.s(x, top + 1, z, "spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        d.box(0, top + 4, 0, 5, top + 4, 5, "spruce_slab[type=bottom,waterlogged=false]");
        // ladder up the front, on a plank backing
        for (int y = 2; y <= top - 1; y++) d.s(2, y, 5, "spruce_planks");
        for (int y = 2; y <= top + 1; y++) d.s(2, y, 6, "ladder[facing=south,waterlogged=false]");
        d.s(2, top + 1, 5, null);
        d.s(4, top + 1, 1, "lantern[hanging=false,waterlogged=false]");
        d.s(1, top + 1, 1, "barrel[facing=up,open=false]");
        return d.bp;
    }

    /** A stone church in ruins: broken walls of varying height, window gaps, a door arch, fallen beams, rubble, moss. */
    static Blueprint chapel(long seed) {
        D d = new D(13, 13, 21, "chapel_ruin", seed);
        int x0 = 1, x1 = 11, z0 = 1, z1 = 19;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            double h = d.h(x, 0, z);
            d.s(x, 1, z, h < 0.45 ? "stone_bricks" : h < 0.7 ? "mossy_stone_bricks" : h < 0.85 ? "cracked_stone_bricks" : h < 0.93 ? "cobblestone" : "moss_block");
            d.s(x, 0, z, "cobblestone");
        }
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
            if (!wall) continue;
            // height: high at the corners and the front gable, broken in between
            double t = BorderHills.valueNoise((x + z) / 3.0, (x - z) / 3.0, (int) (seed & 1023));
            int height = 2 + (int) (t * 6);
            boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
            if (corner) height = 8;
            if (z == z1 && Math.abs(x - 6) <= 3) height = Math.max(height, 9 - Math.abs(x - 6));
            for (int y = 2; y < 2 + height; y++) {
                double h = d.h(x, y, z);
                String b = h < 0.5 ? "stone_bricks" : h < 0.75 ? "mossy_stone_bricks" : h < 0.9 ? "cracked_stone_bricks" : "cobblestone";
                if (corner) b = "polished_andesite";
                d.s(x, y, z, b);
            }
            if (height >= 4 && d.h(x, 9, z) < 0.25) d.s(x, 2 + height, z, "stone_brick_slab[type=bottom,waterlogged=false]");
        }
        // door (front, south) with an arch; side windows; a round window in the back wall
        for (int y = 2; y <= 5; y++) for (int x = 5; x <= 7; x++) d.s(x, y, z1, null);
        d.s(5, 6, z1, "stone_brick_stairs[facing=east,half=top,shape=straight,waterlogged=false]");
        d.s(7, 6, z1, "stone_brick_stairs[facing=west,half=top,shape=straight,waterlogged=false]");
        d.s(6, 6, z1, "chiseled_stone_bricks");
        for (int z = 4; z <= z1 - 3; z += 4) for (int y = 4; y <= 5; y++) { d.s(x0, y, z, null); d.s(x1, y, z, null); }
        for (int y = 5; y <= 6; y++) for (int x = 5; x <= 7; x++) if (!(y == 6 && x != 6)) d.s(x, y, z0, null);
        // inside: altar, broken pews, fallen roof beams, rubble and growth
        d.box(5, 2, z0 + 1, 7, 2, z0 + 1, "stone_brick_slab[type=top,waterlogged=false]");
        d.s(6, 3, z0 + 1, "candle[candles=3,lit=false,waterlogged=false]");
        for (int z = z0 + 4; z <= z1 - 3; z += 2) for (int x = 2; x <= 10; x++) {
            if (x == 6) continue;
            if (d.h(x, 2, z) < 0.6) d.s(x, 2, z, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
        }
        for (int z : new int[] {7, 13}) {
            int len = 6 + (int) (d.h(3, 3, z) * 4);
            for (int k = 0; k < len; k++) d.s(2 + k, 2 + (k < 3 ? 1 : 0), z, "dark_oak_log[axis=x]");
        }
        for (int k = 0; k < 18; k++) {
            int x = 2 + (int) (d.h(k, 4, 1) * 9), z = 2 + (int) (d.h(k, 5, 2) * 16);
            double h = d.h(k, 6, 3);
            d.s(x, 2, z, h < 0.4 ? "cobblestone" : h < 0.6 ? "mossy_cobblestone" : h < 0.8 ? "stone_brick_slab[type=bottom,waterlogged=false]" : "gravel");
        }
        for (int k = 0; k < 14; k++) {
            int x = 2 + (int) (d.h(k, 7, 4) * 9), z = 2 + (int) (d.h(k, 8, 5) * 16);
            if (d.bp.at(x, 2, z) == null) d.s(x, 2, z, d.h(k, 9, 6) < 0.6 ? "fern" : "short_grass");
        }
        return d.bp;
    }

    /** Cobblestone well with water, two posts, a little roof and a chain. */
    static Blueprint well(long seed) {
        D d = new D(5, 7, 5, "well", seed);
        d.box(1, 0, 1, 3, 0, 3, "cobblestone");
        d.box(1, 1, 1, 3, 1, 3, "cobblestone");
        d.s(2, 0, 2, "water");
        d.s(2, 1, 2, "water");
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) if (x != 2 || z != 2) d.s(x, 2, z, d.h(x, 2, z) < 0.3 ? "mossy_cobblestone" : "cobblestone");
        for (int x : new int[] {1, 3}) { d.s(x, 3, 2, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]"); d.s(x, 4, 2, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]"); }
        d.box(1, 5, 2, 3, 5, 2, "stripped_oak_log[axis=x]");
        d.s(2, 4, 2, "chain[axis=y,waterlogged=false]");
        for (int x = 0; x <= 4; x++) {
            d.s(x, 5, 1, "spruce_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
            d.s(x, 5, 3, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
            d.s(x, 6, 2, "spruce_slab[type=bottom,waterlogged=false]");
        }
        return d.bp;
    }

    /** A rounded stack of hay bales. */
    static Blueprint haystack(long seed) {
        D d = new D(5, 6, 5, "haystack", seed);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            int r = Math.abs(x - 2) + Math.abs(z - 2);
            int h = r <= 1 ? 3 : r <= 2 ? 2 : r <= 3 ? 1 : 0;
            if (d.h(x, 0, z) < 0.25 && h > 0) h--;
            for (int y = 2; y < 2 + h; y++) d.s(x, y, z, "hay_block[axis=" + (d.h(x, y, z) < 0.5 ? "x" : "z") + "]");
        }
        return d.bp;
    }

    /** Felled logs stacked between stakes. */
    static Blueprint logpile(long seed) {
        D d = new D(8, 6, 3, "logpile", seed);
        String wood = d.h(1, 1, 1) < 0.5 ? "spruce_log" : "oak_log";
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 3; z++) d.s(x, 2, z, wood + "[axis=x]");
            for (int z = 0; z < 2; z++) d.s(x, 3, z, wood + "[axis=x]");
            if (x < 6) d.s(x, 4, 0, wood + "[axis=x]");
        }
        for (int z = 0; z < 3; z++) d.s(7, 2, z, "spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        d.s(7, 3, 0, "spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        return d.bp;
    }

    /** A rusted-out car: body, cabin with broken glass, wheels; some panels missing. */
    static Blueprint car(long seed) {
        D d = new D(5, 5, 7, "car_wreck", seed);
        String body = d.h(0, 0, 0) < 0.5 ? "brown_terracotta" : d.h(0, 0, 1) < 0.5 ? "light_gray_terracotta" : "cyan_terracotta";
        for (int z = 0; z < 7; z++) for (int x = 1; x <= 3; x++) d.s(x, 2, z, body);
        for (int[] wheel : new int[][] {{0, 1}, {4, 1}, {0, 5}, {4, 5}}) d.s(wheel[0], 2, wheel[1], "polished_blackstone");
        d.s(1, 2, 6, "iron_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]");
        for (int x = 1; x <= 3; x++) {
            d.s(x, 3, 0, "granite_slab[type=bottom,waterlogged=false]");
            d.s(x, 3, 5, "granite_slab[type=bottom,waterlogged=false]");
            d.s(x, 3, 6, "granite_slab[type=bottom,waterlogged=false]");
            d.s(x, 4, 2, "granite_slab[type=bottom,waterlogged=false]");
            d.s(x, 4, 3, "granite_slab[type=bottom,waterlogged=false]");
        }
        for (int z = 1; z <= 4; z++) for (int x : new int[] {1, 3}) d.s(x, 3, z, z == 1 || z == 4 ? body : "gray_stained_glass_pane[east=false,north=true,south=true,waterlogged=false,west=false]");
        d.s(2, 3, 2, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
        d.s(2, 3, 4, "gray_stained_glass_pane[east=true,north=false,south=false,waterlogged=false,west=true]");
        for (int k = 0; k < 3; k++) { // missing panels
            int x = 1 + (int) (d.h(k, 3, 9) * 3), y = 2 + (int) (d.h(k, 4, 9) * 3), z = (int) (d.h(k, 5, 9) * 7);
            if (y >= 3) d.s(x, y, z, null);
        }
        return d.bp;
    }

    /** Soviet concrete bus shelter with a mosaic band, a bench and a sign post. */
    static Blueprint busStop(long seed) {
        D d = new D(9, 7, 5, "bus_stop", seed);
        d.box(0, 1, 0, 8, 1, 3, "smooth_stone");
        for (int x = 1; x <= 7; x++) for (int y = 2; y <= 4; y++) {
            String m = y != 3 ? "light_gray_concrete" : new String[] {"cyan_concrete", "white_concrete", "blue_concrete", "white_concrete"}[x % 4];
            d.s(x, y, 0, m);
        }
        for (int y = 2; y <= 4; y++) for (int z = 0; z <= 1; z++) { d.s(1, y, z, "light_gray_concrete"); d.s(7, y, z, "light_gray_concrete"); }
        d.box(0, 5, 0, 8, 5, 3, "smooth_stone_slab[type=bottom,waterlogged=false]");
        for (int x = 3; x <= 5; x++) d.s(x, 2, 1, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
        for (int y = 2; y <= 4; y++) d.s(8, y, 3, "iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]");
        d.s(8, 5, 3, "yellow_concrete");
        return d.bp;
    }
}
