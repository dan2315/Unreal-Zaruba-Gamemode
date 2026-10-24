import java.util.*;

/**
 * A fishing hut on the west shore of the capture points lake, in the style of the village izbas (spruce log walls,
 * dark oak corner posts, spruce stair roof, oak fences, barrels), with a plank pier on log posts over the water and a
 * wide road from the yard down the shore into the lake (a slipway: vehicles and boats can go in and out of the water).
 * Coordinates: hut door at (dx, G+1, dz); the slipway runs east from the yard into the water along z = dz+4..dz+12.
 * Usage: java FishingHut <doorX> <G> <doorZ> <waterY> [region dir]
 */
public class FishingHut {
    static Canvas c;

    public static void main(String[] a) {
        int dx = Integer.parseInt(a[0]), G = Integer.parseInt(a[1]), dz = Integer.parseInt(a[2]), water = Integer.parseInt(a[3]);
        c = new Canvas(0, 0);
        if (a.length > 4) c.region = new Region(java.nio.file.Path.of(a[4]));
        // the hut: 9 x 7 outside, door in the middle of the south wall
        int x0 = dx - 4, x1 = dx + 4, z0 = dz - 6, z1 = dz;
        c.ground(x0 - 1, G, z0 - 1, x1 + 1, G, z1 + 1, "cobblestone");
        c.fill(x0, G + 1, z0, x1, G + 1, z1, "spruce_planks");
        for (int y = G + 1; y <= G + 4; y++) {
            for (int x = x0; x <= x1; x++) {
                c.set(x, y, z0, "spruce_log[axis=x]");
                c.set(x, y, z1, "spruce_log[axis=x]");
            }
            for (int z = z0; z <= z1; z++) {
                c.set(x0, y, z, "spruce_log[axis=z]");
                c.set(x1, y, z, "spruce_log[axis=z]");
            }
            for (int[] k : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) c.set(k[0], y, k[1], "dark_oak_log[axis=y]");
        }
        for (int y = G + 2; y <= G + 4; y++) for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++) c.set(x, y, z, "cave_air");
        // windows (spruce shutters around glass panes)
        for (int x : new int[] {x0 + 2, x1 - 2}) {
            c.set(x, G + 3, z1, "glass_pane[east=true,north=false,south=false,west=true,waterlogged=false]");
            c.set(x, G + 3, z0, "glass_pane[east=true,north=false,south=false,west=true,waterlogged=false]");
        }
        for (int z : new int[] {z0 + 3}) {
            c.set(x0, G + 3, z, "glass_pane[east=false,north=true,south=true,west=false,waterlogged=false]");
            c.set(x1, G + 3, z, "glass_pane[east=false,north=true,south=true,west=false,waterlogged=false]");
        }
        // door
        c.set(dx, G + 2, z1, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
        c.set(dx, G + 3, z1, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
        // gable roof along x: stairs on the long sides stepping up to a ridge
        for (int step = 0; step <= 4; step++) {
            int y = G + 5 + step;
            int zn = z0 - 1 + step, zs = z1 + 1 - step;
            if (zn > zs) break;
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                if (zn == zs) { c.set(x, y, zn, "dark_oak_slab[type=bottom,waterlogged=false]"); continue; }
                c.set(x, y, zn, "spruce_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
                c.set(x, y, zs, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
                if (x == x0 || x == x1) for (int z = zn + 1; z < zs; z++) c.set(x, y, z, "spruce_planks");
            }
        }
        // chimney on the west end
        for (int y = G + 1; y <= G + 10; y++) c.set(x0 + 1, y, z0 + 1, y <= G + 4 ? "cobblestone" : "stone_bricks");
        c.set(x0 + 1, G + 11, z0 + 1, "campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]");
        c.set(x0 + 1, G + 2, z0 + 2, "cave_air");
        // inside
        c.set(x1 - 1, G + 2, z0 + 1, "barrel[facing=up,open=false]");
        c.set(x1 - 2, G + 2, z0 + 1, "barrel[facing=up,open=false]");
        c.set(x1 - 1, G + 3, z0 + 1, "barrel[facing=up,open=false]");
        c.set(x1 - 1, G + 2, z0 + 3, "crafting_table");
        c.set(x0 + 2, G + 2, z1 - 1, "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
        c.set(dx, G + 5, dz - 3, "lantern[hanging=true,waterlogged=false]");
        // porch: a step and a lantern on a post
        c.set(dx - 1, G + 1, z1 + 1, "spruce_slab[type=bottom,waterlogged=false]");
        c.set(dx, G + 1, z1 + 1, "spruce_slab[type=bottom,waterlogged=false]");
        c.set(dx + 1, G + 1, z1 + 1, "spruce_slab[type=bottom,waterlogged=false]");
        c.set(dx + 2, G + 1, z1 + 1, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        c.set(dx + 2, G + 2, z1 + 1, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        c.set(dx + 2, G + 3, z1 + 1, "lantern[hanging=false,waterlogged=false]");
        // net racks and barrels in the yard
        for (int x = x1 + 2; x <= x1 + 6; x += 4) {
            for (int y = G + 1; y <= G + 3; y++) c.set(x, y, z0 + 1, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        }
        for (int x = x1 + 2; x <= x1 + 6; x++) c.set(x, G + 3, z0 + 1, "oak_fence[east=" + (x < x1 + 6) + ",north=false,south=false,waterlogged=false,west=" + (x > x1 + 2) + "]");
        for (int x = x1 + 3; x <= x1 + 5; x++) c.set(x, G + 2, z0 + 1, "cobweb");
        c.set(x1 + 2, G + 1, z0 + 3, "barrel[facing=up,open=false]");
        c.set(x1 + 3, G + 1, z0 + 3, "barrel[facing=north,open=false]");
        c.set(x1 + 2, G + 1, z0 + 4, "composter[level=3]");

        // slipway: 9 wide from the yard east into the water, cobblestone with gravel, log curbs
        int sz0 = dz + 4, sz1 = dz + 12;
        int shore = x1 + 12;
        for (int x = x0 - 2; x <= shore + 18; x++) {
            int y = x <= shore ? G : Math.max(water - 6, G - (x - shore + 1) / 2);
            for (int z = sz0; z <= sz1; z++) {
                boolean curb = z == sz0 || z == sz1;
                String block = curb ? "stripped_spruce_log[axis=x]" : ((x * 31 + z * 17) % 5 == 0 ? "gravel" : (x + z) % 3 == 0 ? "andesite" : "cobblestone");
                if (x <= shore) c.ground(x, y, z, x, y, z, block);
                else {
                    c.ground(x, y, z, x, y, z, block);
                    for (int yy = y - 1; yy >= y - 6; yy--) c.set(x, yy, z, "cobblestone");
                }
            }
        }
        // lantern posts at the head of the slipway
        for (int z : new int[] {sz0, sz1}) {
            c.set(shore, G + 1, z, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
            c.set(shore, G + 2, z, "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
            c.set(shore, G + 3, z, "lantern[hanging=false,waterlogged=false]");
        }
        // yard path from the door to the slipway
        c.ground(dx - 1, G, z1 + 1, dx + 1, G, sz0, "coarse_dirt");

        // pier on posts: from the shore north of the slipway out over the water, deck one below the ground
        int pz0 = z0 - 1, pz1 = z0 + 3, deck = G - 1;
        for (int x = shore + 1; x <= shore + 16; x++) for (int z = pz0; z <= pz1; z++) c.set(x, deck, z, "spruce_planks");
        for (int x = shore + 4; x <= shore + 16; x += 4) for (int z : new int[] {pz0, pz1})
            for (int y = deck - 1; y >= deck - 12; y--) c.set(x, y, z, "spruce_log[axis=y]");
        for (int x = shore + 1; x <= shore + 16; x++) {
            boolean end = x == shore + 16;
            c.set(x, deck + 1, pz0, "oak_fence[east=" + !end + ",north=false,south=" + end + ",waterlogged=false,west=" + (x > shore + 1) + "]");
        }
        for (int z = pz0 + 1; z <= pz1; z++) c.set(shore + 16, deck + 1, z, "oak_fence[east=false,north=true,south=" + (z < pz1) + ",waterlogged=false,west=false]");
        c.set(shore + 16, deck + 2, pz0, "lantern[hanging=false,waterlogged=false]");
        c.set(shore + 15, deck + 1, pz1, "barrel[facing=up,open=false]");
        c.set(shore + 10, deck + 1, pz1, "spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
        c.print(new String[0]);
    }
}
