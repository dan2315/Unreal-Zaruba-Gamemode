import java.util.*;

/**
 * Lakeside pumping station, a capture point. Coordinates are relative to the yard centre (the capture area);
 * x grows east (away from the lake), z south. Symmetric north/south so neither team's approach is favoured.
 * Usage: java Complex.java <cx> <groundY> <cz>
 */
public class Complex {
    static int CX, G, CZ, F;
    static final List<String> out = new ArrayList<>();

    public static void main(String[] a) {
        CX = Integer.parseInt(a[0]); G = Integer.parseInt(a[1]); CZ = Integer.parseInt(a[2]);
        F = G + 1;
        out.add("forceload add " + (CX - 66) + " " + (CZ - 34) + " " + (CX + 30) + " " + (CZ + 34));

        site();
        yard();
        pumpHouse();
        intakePipes();
        for (int sz : new int[] {-1, 1}) {
            tank(-9, 19 * sz);
            tank(9, 19 * sz);
            pipeRack(11 * sz);
            approachCover(sz);
        }
        controlBuilding();
        lamps();

        out.add("forceload remove " + (CX - 66) + " " + (CZ - 34) + " " + (CX + 30) + " " + (CZ + 34));
        out.forEach(c -> System.out.println("execute in unrealzaruba:game_dim run " + c));
    }

    // ---------------- ground ----------------
    static void site() {
        bigFill(-56, F, -30, 28, F + 30, 30, "air");                          // trees and plants
        fillReplace(-40, G - 4, -28, 26, G - 1, 28, "dirt", "air");
        fill(-40, G, -28, 26, G, 28, "coarse_dirt");
        // gravel roads: N/S paths, east approaches, link to the pier
        fill(-2, G, -28, 2, G, 28, "gravel");
        fill(10, G, -10, 26, G, -7, "gravel");
        fill(10, G, 7, 26, G, 10, "gravel");
        fill(-40, G, -2, -36, G, 2, "gravel");
    }

    // ---------------- capture yard ----------------
    static void yard() {
        fill(-10, G, -9, 10, G, 9, "yellow_concrete");                        // hazard border
        fill(-9, G, -8, 9, G, 8, "smooth_stone");
        fill(-9, G, -1, 9, G, 1, "polished_andesite");
        fill(-1, G, -8, 1, G, 8, "polished_andesite");
        // valve station in the middle: 3x3, two high
        fill(-1, F, -1, 1, F, 1, "polished_andesite");
        fill(-1, F + 1, -1, 1, F + 1, 1, "create:copper_casing");
        set(0, F + 2, 0, "create:copper_valve_handle[facing=up]");
        fill(-1, F + 2, -1, -1, F + 7, -1, "create:fluid_pipe");
        // low walls (crouch cover), mirrored
        for (int sz : new int[] {-1, 1}) {
            fill(-6, F, 4 * sz, -4, F, 4 * sz, "stone_brick_wall");
            fill(4, F, 4 * sz, 6, F, 4 * sz, "stone_brick_wall");
        }
        // crate stacks either side of the centre line, mirrored
        for (int sx : new int[] {-7, 7}) {
            set(sx, F, 0, "barrel[facing=up]");
            set(sx, F + 1, 0, "barrel[facing=up]");
            set(sx, F, sx < 0 ? -1 : 1, "barrel[facing=up]");
        }
    }

    // ---------------- pump house (west, by the lake) ----------------
    static void pumpHouse() {
        int x0 = -36, x1 = -16, z0 = -10, z1 = 10;
        fill(x0, G, z0, x1, G, z1, "polished_andesite");
        // walls: dripstone base course, bricks, granite corner pillars, roof with parapet
        hollow(x0, F, z0, x1, F, z1, "create:cut_dripstone_bricks");
        hollow(x0, F + 1, z0, x1, F + 5, z1, "bricks");
        for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(c[0], F, c[1], c[0], F + 7, c[1], "create:cut_granite_bricks");
        for (int z = z0 + 3; z <= z1 - 3; z += 4) { fill(x0, F, z, x0, F + 6, z, "create:cut_granite_bricks"); fill(x1, F, z, x1, F + 6, z, "create:cut_granite_bricks"); }
        fill(x0, F + 6, z0, x1, F + 6, z1, "smooth_stone");
        hollow(x0, F + 7, z0, x1, F + 7, z1, "stone_brick_wall");
        // windows
        for (int x = x0 + 2; x <= x1 - 2; x += 3) { fill(x, F + 2, z0, x, F + 3, z0, "light_gray_stained_glass_pane"); fill(x, F + 2, z1, x, F + 3, z1, "light_gray_stained_glass_pane"); }
        // doors: east to the yard (wide), west to the pier, north and south side doors
        fill(x1, F, -2, x1, F + 3, 2, "air");
        fill(x0, F, -1, x0, F + 2, 1, "air");
        fill(-27, F, z0, -25, F + 2, z0, "air");
        fill(-27, F, z1, -25, F + 2, z1, "air");
        // interior: two pump lines on andesite plinths (waist-high cover), pumps, lights
        for (int pz : new int[] {-5, 5}) {
            fill(x0 + 1, F, pz, x1 - 4, F, pz, "polished_andesite");
            fill(x0 + 1, F + 1, pz, x1 - 4, F + 1, pz, "dndecor:industrial_boiler[axis=x,size=normal]");
            fill(x1 - 3, F, pz, x1 - 3, F + 1, pz, "create:copper_casing");
            fill(x1 - 3, F + 2, pz, x1 - 3, F + 5, pz, "create:fluid_pipe");
        }
        set(-24, F, 0, "barrel[facing=up]");
        set(-24, F + 1, 0, "barrel[facing=up]");
        set(-28, F, 0, "barrel[facing=up]");
        for (int x = x0 + 4; x <= x1 - 4; x += 6) for (int z : new int[] {-7, 0, 7}) set(x, F + 5, z, "lantern[hanging=true]");
        // signs of life: lamps over the doors
        set(x1 + 1, F + 4, -3, "lantern");
        set(x1 + 1, F + 3, -3, "create:metal_girder");
        set(x1 + 1, F + 4, 3, "lantern");
        set(x1 + 1, F + 3, 3, "create:metal_girder");
    }

    // ---------------- intake pipes and pier into the lake ----------------
    static void intakePipes() {
        int from = -37, shore = -54, end = -64;
        for (int pz : new int[] {-5, 5}) {
            fill(end, F + 1, pz, from, F + 1, pz, "dndecor:industrial_boiler[axis=x,size=normal]");
            for (int x = from; x >= end; x -= 4) fill(x, G - 3, pz, x, F, pz, "create:metal_girder");
            // intakes: pipes bend down into the water at the far end
            fill(end, G - 4, pz, end, F, pz, "dndecor:industrial_boiler[axis=y,size=normal]");
            set(end, G - 5, pz, "create:copper_casing");
        }
        // pier between the pipes
        fill(end + 2, G, -2, -37, G, 2, "spruce_planks");
        fill(end + 2, F, -2, -37, F, -2, "spruce_fence");
        fill(end + 2, F, 2, -37, F, 2, "spruce_fence");
        for (int x = shore; x >= end + 2; x -= 4) for (int z : new int[] {-2, 2}) fill(x, G - 4, z, x, G - 1, z, "spruce_log");
        set(end + 2, F + 1, -2, "lantern");
        set(end + 2, F + 1, 2, "lantern");
    }

    // ---------------- storage tanks ----------------
    static void tank(int cx, int cz) {
        int r = 3;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > r + 0.5) continue;
            boolean wall = d > r - 0.5;
            int x = cx + dx, z = cz + dz;
            fill(x, G, z, x, G, z, "polished_andesite");
            if (wall) {
                fill(x, F, z, x, F + 7, z, "polished_andesite");
                set(x, F + 2, z, "create:andesite_casing");
                set(x, F + 6, z, "create:andesite_casing");
                set(x, F + 8, z, "smooth_stone_slab");
            } else {
                fill(x, F, z, x, F + 7, z, "air");
                set(x, F + 8, z, "smooth_stone_slab");
            }
        }
        // pipe from the tank top to the rack
        int rackZ = cz < 0 ? -11 : 11;
        int dir = cz < 0 ? 1 : -1;
        fill(cx, F + 9, cz, cx, F + 9, cz, "create:fluid_pipe");
        fill(cx, F + 9, cz + dir, cx, F + 9, rackZ - dir, "create:fluid_pipe");
    }

    // ---------------- overhead pipe rack, pump house -> control building ----------------
    static void pipeRack(int z) {
        for (int x : new int[] {-15, -6, 0, 6, 12}) fill(x, F, z, x, F + 7, z, "create:metal_girder");
        fill(-15, F + 8, z, 13, F + 8, z, "create:metal_girder");
        fill(-15, F + 9, z, 13, F + 9, z, "dndecor:industrial_boiler[axis=x,size=normal]");
    }

    // ---------------- cover on the approaches, mirrored ----------------
    static void approachCover(int sz) {
        // container half-way along the tank path
        String container = sz < 0 ? "dndecor:red_container[axis=x,large=false]" : "dndecor:blue_container[axis=x,large=false]";
        fill(-1, F, 14 * sz, 1, F, 14 * sz, container);
        // east approach: crates and a low wall
        set(16, F, 8 * sz, "barrel[facing=up]");
        set(16, F + 1, 8 * sz, "barrel[facing=up]");
        set(17, F, 8 * sz, "barrel[facing=up]");
        fill(21, F, 7 * sz, 21, F, 9 * sz, "stone_brick_wall");
        // outer end of the tank path: sandbag-style slab line
        fill(-3, F, 25 * sz, -1, F, 25 * sz, "smooth_stone_slab");
        fill(1, F, 25 * sz, 3, F, 25 * sz, "smooth_stone_slab");
    }

    // ---------------- control building (east, two floors + roof) ----------------
    static void controlBuilding() {
        int x0 = 13, x1 = 23, z0 = -5, z1 = 5;
        fill(x0, G, z0, x1, G, z1, "polished_andesite");
        hollow(x0, F, z0, x1, F, z1, "create:cut_dripstone_bricks");
        hollow(x0, F + 1, z0, x1, F + 7, z1, "create:cut_granite_bricks");
        for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(c[0], F, c[1], c[0], F + 8, c[1], "stone_bricks");
        fill(x0 + 1, F + 4, z0 + 1, x1 - 1, F + 4, z1 - 1, "spruce_planks");      // second floor
        fill(x0, F + 8, z0, x1, F + 8, z1, "smooth_stone");                       // roof
        hollow(x0, F + 9, z0, x1, F + 9, z1, "stone_brick_wall");
        // windows: ground floor all round, upstairs a long window over the yard
        for (int z = z0 + 2; z <= z1 - 2; z += 2) { set(x1, F + 2, z, "light_gray_stained_glass_pane"); }
        fill(x0, F + 5, -3, x0, F + 6, 3, "light_gray_stained_glass_pane");
        fill(x0 + 2, F + 5, z0, x1 - 2, F + 6, z0, "light_gray_stained_glass_pane");
        fill(x0 + 2, F + 5, z1, x1 - 2, F + 6, z1, "light_gray_stained_glass_pane");
        // doors west (yard) and east (forest)
        fill(x0, F, -1, x0, F + 2, 1, "air");
        fill(x1, F, -1, x1, F + 2, 1, "air");
        // ladder up to the second floor and the roof
        fill(x1 - 1, F + 4, z1 - 1, x1 - 1, F + 4, z1 - 1, "air");
        fill(x1 - 1, F + 8, z1 - 1, x1 - 1, F + 8, z1 - 1, "air");
        fill(x1 - 1, F, z1 - 1, x1 - 1, F + 8, z1 - 1, "ladder[facing=north]");
        // inside: desks and lights
        set(x0 + 2, F, z0 + 1, "crafting_table");
        set(x0 + 3, F, z0 + 1, "barrel[facing=up]");
        set(x0 + 2, F + 5, z0 + 1, "lectern[facing=west]");
        set((x0 + x1) / 2, F + 3, 0, "lantern[hanging=true]");
        set((x0 + x1) / 2, F + 7, 0, "lantern[hanging=true]");
        // antenna
        fill(x0 + 2, F + 9, z1 - 2, x0 + 2, F + 14, z1 - 2, "create:metal_girder");
        set(x0 + 2, F + 15, z1 - 2, "lightning_rod");
    }

    static void lamps() {
        for (int[] p : new int[][] {{-11, -10}, {-11, 10}, {11, -10}, {11, 10}, {0, -27}, {0, 27}}) {
            int x = p[0] + (p[1] == -27 || p[1] == 27 ? 4 : 0);
            fill(x, F, p[1], x, F + 3, p[1], "create:metal_girder");
            set(x, F + 4, p[1], "lantern");
        }
    }

    // ---------------- helpers ----------------
    static void hollow(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        fill(x0, y0, z0, x1, y1, z0, block);
        fill(x0, y0, z1, x1, y1, z1, block);
        fill(x0, y0, z0, x0, y1, z1, block);
        fill(x1, y0, z0, x1, y1, z1, block);
    }

    static void bigFill(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        int area = (x1 - x0 + 1) * (z1 - z0 + 1);
        int layers = Math.max(1, 32768 / area);
        for (int y = y0; y <= y1; y += layers) fill(x0, y, z0, x1, Math.min(y1, y + layers - 1), z1, block);
    }

    static void fill(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        out.add("fill " + (CX + x0) + " " + y0 + " " + (CZ + z0) + " " + (CX + x1) + " " + y1 + " " + (CZ + z1) + " " + id(block));
    }

    static void fillReplace(int x0, int y0, int z0, int x1, int y1, int z1, String block, String replace) {
        out.add("fill " + (CX + x0) + " " + y0 + " " + (CZ + z0) + " " + (CX + x1) + " " + y1 + " " + (CZ + z1) + " " + id(block) + " replace " + id(replace));
    }

    static void set(int x, int y, int z, String block) {
        out.add("setblock " + (CX + x) + " " + y + " " + (CZ + z) + " " + id(block));
    }

    static String id(String block) {
        int end = block.length();
        for (char c : new char[] {'[', '{'}) if (block.indexOf(c) >= 0) end = Math.min(end, block.indexOf(c));
        return block.substring(0, end).contains(":") ? block : "minecraft:" + block;
    }
}
