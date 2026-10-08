import java.util.*;

/**
 * Prints the commands that build a respawn base for a capture point, for the match dimension.
 *   compound: java Base.java compound <cx> <groundY> <cz> <R> <padLen> <tents yes|no>
 *   outpost:  java Base.java outpost  <cx> <groundY> <cz> <padLen>
 * groundY is the top ground block (the floor is laid at that y; players stand at groundY+1). The vehicle spawner
 * sits in the east gate facing west, so vehicles appear east of the base on the pad.
 */
public class Base {
    static int CX, G, CZ;
    static final List<String> out = new ArrayList<>();

    public static void main(String[] a) {
        String kind = a[0];
        CX = Integer.parseInt(a[1]); G = Integer.parseInt(a[2]); CZ = Integer.parseInt(a[3]);
        if (kind.equals("compound")) compound(Integer.parseInt(a[4]), Integer.parseInt(a[5]), a[6].equals("yes"));
        else outpost(Integer.parseInt(a[4]));
        out.forEach(c -> System.out.println("execute in unrealzaruba:game_dim run " + c));
    }

    // ================= walled compound =================

    static void compound(int R, int padLen, boolean tents) {
        int F = G + 1;
        int p0 = R + 2, p1 = R + 1 + padLen;
        out.add("forceload add " + (CX - R - 30) + " " + (CZ - R - 6) + " " + (CX + p1 + 6) + " " + (CZ + R + 6));

        // ---- clear trees and plants, fill holes, level ----
        int m = R + 4;
        fill(-m, F, -m, m, F + 12, m, "air");
        fill(-m, F + 13, -m, m, F + 28, m, "air");
        fill(R + 1, F, -R - 2, p1 + 3, F + 20, R + 2, "air");
        fillReplace(-R - 1, G - 4, -R - 1, p1 + 1, G - 1, R + 1, "dirt", "air");
        fillReplace(-R - 1, G - 4, -R - 1, p1 + 1, G - 1, R + 1, "dirt", "water");
        fill(-R - 1, G - 1, -R - 1, R + 1, G - 1, R + 1, "stone");

        // ---- foundation and plaza ----
        fill(-R, G, -R, R, G, R, "stone_bricks");
        fill(-R + 1, G, -R + 1, R - 1, G, R - 1, "polished_andesite");
        fill(-R + 1, G, -1, R - 1, G, 1, "smooth_stone");
        fill(-1, G, -R + 1, 1, G, R - 1, "smooth_stone");
        fill(-4, G, -4, 4, G, 4, "polished_blackstone_bricks");
        fill(-3, G, -3, 3, G, 3, "smooth_stone");
        fill(-2, G, -2, 2, G, 2, "chiseled_stone_bricks");
        set(0, G, 0, "lodestone");
        for (int[] d : new int[][] {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) set(d[0], G, d[1], "sea_lantern");
        for (int[] d : new int[][] {{-R + 1, 0}, {R - 1, 0}, {0, -R + 1}, {0, R - 1}}) set(d[0], G, d[1], "sea_lantern");

        // ---- walls with 7-wide gates ----
        for (int side = 0; side < 4; side++) {
            for (int[] seg : new int[][] {{-R, -4}, {4, R}}) wallRun(side, seg[0], seg[1], R, F);
            for (int t : new int[] {-4, 4}) {
                int[] p = sidePos(side, t, R);
                set(p[0], F, p[1], "chiseled_stone_bricks");
                set(p[0], F + 1, p[1], "stone_bricks");
                set(p[0], F + 2, p[1], "polished_andesite");
                set(p[0], F + 3, p[1], "lantern");
            }
        }
        for (int[] c : new int[][] {{R, -R}, {R, R}, {-R, R}}) {
            fill(c[0], F, c[1], c[0], F + 2, c[1], "stone_bricks");
            set(c[0], F + 3, c[1], "chiseled_stone_bricks");
            set(c[0], F + 4, c[1], "lantern");
        }

        // ---- hedge outside, not on the vehicle side or in front of gates ----
        for (int side = 0; side < 4; side++) {
            if (side == 1) continue;
            for (int t = -R - 1; t <= R + 1; t++) {
                if (Math.abs(t) <= 5) continue;
                int[] p = sidePos(side, t, R + 1);
                set(p[0], F, p[1], (Math.floorMod(t * 7 + side, 3) == 0 ? "flowering_azalea_leaves" : "azalea_leaves") + "[persistent=true]");
            }
        }

        tower(-R, -R, F);

        if (tents) {
            tent(-R + 3, -R + 9, 5, 9, F);
            tent(R - 9, R - 3, 5, 9, F);
        } else {
            // supply corner in the south-west instead of tents
            fill(-R + 2, F, R - 2, -R + 4, F, R - 2, "barrel[facing=up]");
            set(-R + 2, F + 1, R - 2, "barrel[facing=up]");
            set(-R + 2, F, R - 4, "hay_block");
        }

        // supply corner north-east
        int sx = R - 4, sz = -R + 2;
        fill(sx - 2, F, sz, sx + 2, F, sz, "barrel[facing=up]");
        fill(sx - 1, F + 1, sz, sx + 1, F + 1, sz, "barrel[facing=up]");
        set(sx - 2, F, sz + 2, "hay_block");
        set(sx - 1, F, sz + 2, "hay_block");
        set(sx + 2, F, sz + 2, "crafting_table");

        // plaza lamp posts
        for (int[] d : new int[][] {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
            fill(d[0], F, d[1], d[0], F + 2, d[1], "spruce_fence");
            set(d[0], F + 3, d[1], "lantern");
        }

        spawnerAndPad(R, F, p0, p1, R);
    }

    static void tower(int x0, int z0, int F) {
        int x1 = x0 + 4, z1 = z0 + 4, floorY = F + 5;
        for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(c[0], F, c[1], c[0], F + 8, c[1], "stripped_spruce_log");
        fill(x0, floorY, z0, x1, floorY, z1, "spruce_planks");
        fill(x0, floorY + 1, z0, x1, floorY + 1, z0, "spruce_fence");
        fill(x0, floorY + 1, z0, x0, floorY + 1, z1, "spruce_fence");
        fill(x1, floorY + 1, z0, x1, floorY + 1, z1 - 2, "spruce_fence");
        fill(x0, floorY + 1, z1, x1 - 1, floorY + 1, z1, "spruce_fence");
        for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) set(c[0], floorY + 1, c[1], "stripped_spruce_log");
        fill(x1, F, z1 - 1, x1, floorY - 1, z1 - 1, "stripped_spruce_log");
        fill(x1 + 1, F, z1 - 1, x1 + 1, floorY, z1 - 1, "ladder[facing=east]");
        int roofY = F + 9;
        fill(x0 - 1, roofY, z0 - 1, x1 + 1, roofY, z1 + 1, "dark_oak_planks");
        fill(x0 - 1, roofY, z0 - 1, x1 + 1, roofY, z0 - 1, "dark_oak_stairs[facing=south]");
        fill(x0 - 1, roofY, z1 + 1, x1 + 1, roofY, z1 + 1, "dark_oak_stairs[facing=north]");
        fill(x0 - 1, roofY, z0, x0 - 1, roofY, z1, "dark_oak_stairs[facing=east]");
        fill(x1 + 1, roofY, z0, x1 + 1, roofY, z1, "dark_oak_stairs[facing=west]");
        fill(x0, roofY + 1, z0, x1, roofY + 1, z1, "dark_oak_planks");
        fill(x0, roofY + 1, z0, x1, roofY + 1, z0, "dark_oak_stairs[facing=south]");
        fill(x0, roofY + 1, z1, x1, roofY + 1, z1, "dark_oak_stairs[facing=north]");
        fill(x0, roofY + 1, z0 + 1, x0, roofY + 1, z1 - 1, "dark_oak_stairs[facing=east]");
        fill(x1, roofY + 1, z0 + 1, x1, roofY + 1, z1 - 1, "dark_oak_stairs[facing=west]");
        fill(x0 + 1, roofY + 2, z0 + 1, x1 - 1, roofY + 2, z1 - 1, "dark_oak_slab");
        int mx = x0 + 2, mz = z0 + 2;
        set(mx, roofY - 1, mz, "lantern[hanging=true]");
        fill(mx, roofY + 3, mz, mx, roofY + 6, mz, "spruce_fence");
        set(mx, roofY + 7, mz, "white_banner[rotation=4]");
        set(x0 + 1, F, z0 + 1, "barrel[facing=up]");
        set(x0 + 2, F, z0 + 1, "barrel[facing=up]");
        set(x0 + 1, F + 1, z0 + 1, "barrel[facing=up]");
        set(x0 + 1, F, z0 + 2, "hay_block");
    }

    // ================= open outpost on existing paving =================

    static void outpost(int padLen) {
        int F = G + 1;
        out.add("forceload add " + (CX - 10) + " " + (CZ - 10) + " " + (CX + 10 + padLen) + " " + (CZ + 10));
        fill(-6, F, -6, 6, F + 6, 6, "air");
        // plaza: dark ring, light field, cross, lodestone
        fill(-6, G, -6, 6, G, 6, "polished_blackstone_bricks");
        fill(-5, G, -5, 5, G, 5, "polished_andesite");
        fill(-5, G, -1, 5, G, 1, "smooth_stone");
        fill(-1, G, -5, 1, G, 5, "smooth_stone");
        fill(-2, G, -2, 2, G, 2, "chiseled_stone_bricks");
        set(0, G, 0, "lodestone");
        for (int[] d : new int[][] {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            set(d[0], G, d[1], "sea_lantern");
            fill(d[0], F, d[1], d[0], F + 2, d[1], "stone_brick_wall");
            set(d[0], F + 3, d[1], "lantern");
        }
        // flag and supplies on the west side (by the hall)
        fill(-5, F, 0, -5, F + 5, 0, "spruce_fence");
        set(-5, F + 6, 0, "white_banner[rotation=12]");
        set(-5, F, -3, "barrel[facing=up]");
        set(-5, F + 1, -3, "barrel[facing=up]");
        set(-4, F, -3, "barrel[facing=up]");
        set(-5, F, 3, "hay_block");
        set(-4, F, 3, "crafting_table");
        // spawner on the east edge facing the plaza; vehicles appear east of it
        set(6, F, 0, "unrealzaruba:vehicle_purchase_block[facing=west]");
        set(5, F, -2, "stone_bricks");
        set(5, F + 1, -2, sign(4));
        set(5, F, 2, "stone_bricks");
        set(5, F + 1, 2, "lantern");
        // pad markings painted on the paving
        int p0 = 8, p1 = 7 + padLen;
        fill(p0, G, -12, p1, G, -12, "yellow_concrete");
        fill(p0, G, 12, p1, G, 12, "yellow_concrete");
        fill(p1, G, -12, p1, G, 12, "yellow_concrete");
        for (int t = p0 + 3; t <= p1 - 3; t += 4) set(t, G, 0, "white_concrete");
    }

    // ================= shared =================

    static void spawnerAndPad(int R, int F, int p0, int p1, int half) {
        set(R, F, 0, "unrealzaruba:vehicle_purchase_block[facing=west]");
        set(R - 1, F, -2, "stone_bricks");
        set(R - 1, F + 1, -2, sign(4));
        set(R - 1, F, 2, "stone_bricks");
        set(R - 1, F + 1, 2, "lantern");
        fill(p0, G, -half, p1, G, half, "gray_concrete");
        fill(p0, G, -half, p1, G, -half, "yellow_concrete");
        fill(p0, G, half, p1, G, half, "yellow_concrete");
        fill(p1, G, -half, p1, G, half, "yellow_concrete");
        fill(p0, G, -half, p0, G, -5, "yellow_concrete");
        fill(p0, G, 5, p0, G, half, "yellow_concrete");
        fill(R + 1, G, -3, R + 1, G, 3, "smooth_stone");
        for (int t = p0 + 3; t <= p1 - 3; t += 4) set(t, G, 0, "white_concrete");
        for (int[] c : new int[][] {{p1 + 1, -half}, {p1 + 1, half}}) {
            fill(c[0], F, c[1], c[0], F + 3, c[1], "create:metal_girder");
            set(c[0], F + 4, c[1], "lantern");
        }
    }

    static String sign(int rotation) {
        return "oak_sign[rotation=" + rotation + "]{front_text:{messages:['\"\"','{\"text\":\"\\\\u0421\\\\u043f\\\\u0430\\\\u0432\\\\u043d\",\"bold\":true}','{\"text\":\"\\\\u0442\\\\u0435\\\\u0445\\\\u043d\\\\u0438\\\\u043a\\\\u0438\"}','\"\"']}}";
    }

    static int[] sidePos(int side, int t, int r) {
        return switch (side) {
            case 0 -> new int[] {t, -r};
            case 1 -> new int[] {r, t};
            case 2 -> new int[] {t, r};
            default -> new int[] {-r, t};
        };
    }

    static void wallRun(int side, int t0, int t1, int r, int f) {
        int[] a = sidePos(side, t0, r), b = sidePos(side, t1, r);
        fill(a[0], f, a[1], b[0], f, b[1], "stone_bricks");
        fill(a[0], f + 1, a[1], b[0], f + 1, b[1], "stone_brick_wall");
    }

    static void tent(int x0, int x1, int z0, int z1, int f) {
        for (int x = x0; x <= x1; x++) {
            String canvas = (x - x0) % 2 == 0 ? "white_wool" : "light_gray_wool";
            set(x, f, z0, canvas);
            set(x, f, z1, canvas);
            set(x, f + 1, z0 + 1, canvas);
            set(x, f + 1, z1 - 1, canvas);
            set(x, f + 2, z0 + 2, canvas);
        }
        set(x0 - 1, f, z0 + 2, "spruce_fence");
        set(x0 - 1, f + 1, z0 + 2, "spruce_fence");
        set(x0 - 1, f + 2, z0 + 2, "lantern");
        set(x1 + 1, f, z0 + 2, "spruce_fence");
        set(x1 + 1, f + 1, z0 + 2, "spruce_fence");
        set(x0 + 1, f, z0 + 1, "barrel[facing=up]");
        set(x1 - 1, f, z1 - 1, "barrel[facing=up]");
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
