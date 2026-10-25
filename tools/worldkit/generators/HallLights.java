import java.nio.file.*;
import java.util.*;

/**
 * Lights for an indoor hall, planned against a light simulation of the hall as it is in the region files.
 *
 * <p>Lamps are createdeco cage lamps lit without redstone (inverted=true): pendants hang on a chain from the first block
 * above them (the ceiling), "under" lamps hang directly below a catwalk or platform. Before printing the build, the block
 * light of the hall is simulated (existing lamps plus the new ones; full blocks stop light, everything else lets it through
 * one level weaker per block, as in the game for most shapes) and the standing spots of each level are printed as a map:
 * digit = light 0-9, '+' = 10 and more. Sky light through doors and windows is left out, so the real hall is a little
 * brighter. Monsters spawn only at block light 0.
 *
 * <p>Usage: java HallLights <region> <layout> [--sim-only]. Layouts are written in {@link #layout}; "power_station" is the
 * Электростанция turbine hall (768 49 194 capture platform).
 */
public class HallLights {
    record Lamp(int x, int y, int z, boolean pendant, String color) {}

    static final String CHAIN = "minecraft:chain[axis=y,waterlogged=false]";

    static String lampBlock(String color) {
        return "createdeco:" + color + "_industrial_iron_lamp[facing=down,inverted=true,lit=true,waterlogged=false]";
    }

    /** Hall box for the simulation, standing levels to report, and the lamps. */
    record Layout(int x0, int y0, int z0, int x1, int y1, int z1, int[] stands, List<Lamp> lamps) {}

    static Layout layout(String name) {
        List<Lamp> l = new ArrayList<>();
        if (name.equals("power_station")) {
            // Turbine hall: floor stand 42, catwalks (stand 49) along the walls at x 754-755 / 785-786 and z 180-181 / 211-212,
            // a spine at x 770-771 and the capture platform 767-774 x 193-200 at stand 49; ceiling at 59
            // The capture platform: a bright square over the capture area (768-773 x 194-199) and its centre
            for (int[] p : new int[][]{{768, 194}, {773, 194}, {768, 199}, {773, 199}, {770, 196}, {771, 197}}) l.add(new Lamp(p[0], 53, p[1], true, "yellow"));
            // Over the catwalks, every 8 blocks, high enough to walk and jump under (stand 49)
            for (int z : new int[]{186, 194, 202}) {
                l.add(new Lamp(754, 53, z, true, "yellow"));
                l.add(new Lamp(786, 53, z, true, "yellow"));
            }
            for (int x : new int[]{762, 778}) {
                l.add(new Lamp(x, 53, 180, true, "yellow"));
                l.add(new Lamp(x, 53, 212, true, "yellow"));
            }
            for (int z : new int[]{186, 207}) l.add(new Lamp(770, 53, z, true, "yellow"));
            // Under the catwalks, lighting the floor along the walls
            for (int z : new int[]{184, 192, 200, 208}) {
                l.add(new Lamp(755, 47, z, false, "yellow"));
                l.add(new Lamp(785, 47, z, false, "yellow"));
            }
            for (int x : new int[]{760, 766, 776, 782}) {
                l.add(new Lamp(x, 47, 181, false, "yellow"));
                l.add(new Lamp(x, 47, 211, false, "yellow"));
            }
            // Under the spine and the platform
            for (int z : new int[]{186, 191, 202, 207}) l.add(new Lamp(771, 47, z, false, "yellow"));
            for (int[] p : new int[][]{{767, 193}, {774, 193}, {767, 200}, {774, 200}}) l.add(new Lamp(p[0], 47, p[1], false, "yellow"));
            // The four turbine-generator sets (PowerHall units, x 755-761 / 779-785, z 181-190 / 202-211): a pendant over each
            // generator and one over each turbine beside its steam pipe
            for (int x : new int[]{758, 782}) for (int z : new int[]{188, 204}) l.add(new Lamp(x, 52, z, true, "yellow"));
            for (int x : new int[]{756, 784}) for (int z : new int[]{183, 209}) l.add(new Lamp(x, 52, z, true, "yellow"));
            // The corridor from the doors to the middle, beside the sets: pendants low over the floor (stand 42; 46 clears a jump)
            for (int x : new int[]{764, 777}) for (int z : new int[]{184, 189, 203, 208}) l.add(new Lamp(x, 46, z, true, "yellow"));
            // The ejector lanes either side of the tower (pads at x 760 / 781, z 196-197, launching toward the tower): behind
            // the pads, so nothing hangs in the flight path
            for (int x : new int[]{757, 784}) for (int z : new int[]{194, 199}) l.add(new Lamp(x, 46, z, true, "yellow"));
            return new Layout(748, 38, 172, 793, 62, 220, new int[]{42, 49}, l);
        }
        throw new IllegalArgumentException("unknown layout " + name);
    }

    static final String[] SEE_THROUGH = {"air", "chain", "lamp", "lantern", "catwalk", "railing", "bars", "glass", "pane", "slab",
            "stairs", "_wall", "fence", "door", "ladder", "decal", "copycat", "sign", "banner", "support", "shaft", "seat", "torch",
            "button", "lever", "carpet", "plant", "grass", "flower", "fern", "vine", "pressure_plate", "rail", "display_board",
            "stressometer", "speedometer", "ejector", "water", "scaffold", "hatch", "valve", "pipe", "girder"};

    static boolean opaque(String state) {
        String n = Region.name(state);
        for (String s : SEE_THROUGH) if (n.contains(s)) return false;
        return true;
    }

    static int emission(String state) {
        if (!state.contains("lit=true") && !state.contains("lantern")) return 0;
        String n = Region.name(state);
        return n.contains("lamp") || n.contains("lantern") || n.contains("light") ? 15 : 0;
    }

    public static void main(String[] a) throws Exception {
        Region reg = new Region(Path.of(a[0]));
        Layout lay = layout(a[1]);
        boolean simOnly = Arrays.asList(a).contains("--sim-only");
        int nx = lay.x1 - lay.x0 + 1, ny = lay.y1 - lay.y0 + 1, nz = lay.z1 - lay.z0 + 1;
        String[][][] world = new String[nx][ny][nz];
        for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) for (int k = 0; k < nz; k++) world[i][j][k] = reg.state(lay.x0 + i, lay.y0 + j, lay.z0 + k);

        Canvas c = new Canvas(0, 0);
        int chains = 0, placed = 0;
        List<String> problems = new ArrayList<>();
        for (Lamp lamp : lay.lamps) {
            int i = lamp.x - lay.x0, j = lamp.y - lay.y0, k = lamp.z - lay.z0;
            if (!Region.isAir(world[i][j][k])) { problems.add("lamp spot taken " + lamp + ": " + world[i][j][k]); continue; }
            int top = j + 1;
            while (top < ny && Region.isAir(world[i][top][k])) top++;
            if (top >= ny) { problems.add("nothing above " + lamp); continue; }
            if (!lamp.pendant && top != j + 1) { problems.add("under-lamp with air above " + lamp); continue; }
            for (int y = j + 1; y < top; y++) {
                c.set(lamp.x, lay.y0 + y, lamp.z, CHAIN);
                world[i][y][k] = CHAIN;
                chains++;
            }
            c.set(lamp.x, lamp.y, lamp.z, lampBlock(lamp.color));
            world[i][j][k] = lampBlock(lamp.color);
            placed++;
        }

        // Block light flood fill
        int[][][] light = new int[nx][ny][nz];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) for (int k = 0; k < nz; k++) {
            int e = emission(world[i][j][k]);
            if (e > 0) { light[i][j][k] = e; queue.add(new int[]{i, j, k}); }
        }
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int v = light[p[0]][p[1]][p[2]] - 1;
            if (v <= 0) continue;
            for (int[] d : dirs) {
                int i = p[0] + d[0], j = p[1] + d[1], k = p[2] + d[2];
                if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz) continue;
                if (opaque(world[i][j][k]) || light[i][j][k] >= v) continue;
                light[i][j][k] = v;
                queue.add(new int[]{i, j, k});
            }
        }

        for (int sy : lay.stands) {
            int j = sy - lay.y0, spots = 0, dark = 0, zero = 0, sum = 0;
            StringBuilder map = new StringBuilder();
            for (int k = 0; k < nz; k++) {
                StringBuilder row = new StringBuilder(String.format("%5d ", lay.z0 + k));
                boolean any = false;
                for (int i = 0; i < nx; i++) {
                    boolean stand = Region.isAir(world[i][j][k]) && j + 1 < ny && Region.isAir(world[i][j + 1][k]) && j > 0
                            && !Region.isAir(world[i][j - 1][k]) && !world[i][j - 1][k].contains("chain") && !world[i][j - 1][k].contains("lamp");
                    // only inside the hall: something solid overhead
                    boolean roofed = false;
                    for (int y = j + 2; y < ny && !roofed; y++) roofed = !Region.isAir(world[i][y][k]) && !world[i][y][k].contains("chain") && !world[i][y][k].contains("lamp");
                    if (!stand || !roofed) { row.append(stand ? ',' : '.'); continue; }
                    int v = light[i][j][k];
                    spots++; sum += v; any = true;
                    if (v < 7) dark++;
                    if (v == 0) zero++;
                    row.append(v >= 10 ? '+' : (char) ('0' + v));
                }
                if (any) map.append(row).append('\n');
            }
            System.err.printf("stand y=%d: %d spots under a roof, average light %.1f, below 7: %d, zero: %d%n", sy, spots, spots == 0 ? 0.0 : (double) sum / spots, dark, zero);
            System.err.print(map);
        }
        System.err.println("lamps " + placed + ", chain blocks " + chains);
        problems.forEach(p -> System.err.println("  ! " + p));
        if (!simOnly) c.print(new String[0]);
    }
}
