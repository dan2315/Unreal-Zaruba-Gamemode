import java.nio.file.*;
import java.util.*;

/**
 * Dressing of the Электростанция turbine hall: inside x 754-786, z 180-212, floor stand 42, catwalks stand 49 (along the
 * walls, cross catwalks at z 180-181 / 211-212, spine x 770-771), the capture platform 767-774 x 193-200 (stand 49) on a
 * tower with a lower catwalk ring at y45 (x 762-779, z 191-202) that weighted ejectors in the floor (x 760 / 781,
 * z 196-197) launch players onto. Ceiling at 59. Everything stays clear of the ring, the ejector lanes and the doors
 * (north x 770-772, south x 766-768).
 *
 * Parts (comma separated):
 *   units     - four turbine-generator sets, two in the north part and two in the south part of the hall, either side
 *               of the corridor from the doors; point-symmetric for the two teams. Each (along z, 10 long): a plinth
 *               with hazard stripes, a 3-wide turbine cylinder (dndecor boilers with their invisible structure ring,
 *               a brass band, an end cap) on supports, a steam pipe up to the ceiling, a flywheel on the coupling, a
 *               green generator housing with insulators on top and a copper exciter at the end
 *   unitNW    - only the north-west set (a test)
 *   walls     - a copper pipe and two cable tubes along the walls under the catwalks, broken at doors and stairs
 *   consoles  - control desks in the four corners of the capture platform (outside the capture area 768-773 x 194-199):
 *               a 2-high corner with a speedometer, 1-high arms with stressometers (cover on the point)
 *   hazard    - warning decals at the foot and head of the stairs, high-voltage signs on the generators
 * Usage: java PowerHall <region> <parts>
 */
public class PowerHall {
    static Region reg;

    static boolean air(int x, int y, int z) throws Exception {
        return Region.isAir(reg.state(x, y, z));
    }

    // Mirrors of the hall: x about 770, z about 196 (point symmetry = both)
    static int mx(int x) { return 1540 - x; }
    static int mz(int z) { return 392 - z; }

    /**
     * One set along z. cx: centre line; z0: the turbine end (next to a wall); dz: +1 when the set runs south from z0,
     * -1 north. The generator is at the end toward the middle of the hall.
     */
    static void unit(Canvas c, int cx, int z0, int dz) {
        int len = 10;
        String side = cx < 770 ? "west" : "east", away = cx < 770 ? "east" : "west";
        for (int i = 0; i < len; i++) {
            int z = z0 + dz * i;
            for (int x = cx - 3; x <= cx + 3; x++) {
                boolean edgeX = x == cx - 3 || x == cx + 3, edgeZ = i == 0 || i == len - 1;
                String face = edgeX ? (x < cx ? "west" : "east") : (z == Math.min(z0, z0 + dz * (len - 1)) ? "north" : "south");
                c.set(x, 42, z, edgeX || edgeZ ? "railways:yellow_hazard_stripes_diagonal_on_black[facing=" + face + "]" : "create:industrial_iron_block");
            }
        }
        // Turbine: end cap at i=1, cylinder i=2..4 (brass band at i=3), on supports
        int y = 45;
        for (int i = 1; i <= 4; i++) {
            int z = z0 + dz * i;
            String boiler = i == 1 ? "dndecor:industrial_boiler[axis=z,size=medium]"
                    : i == 3 ? "dndecor:brass_boiler[axis=z,size=large]" : "dndecor:industrial_boiler[axis=z,size=large]";
            c.set(cx, y, z, boiler);
            for (int x = cx - 1; x <= cx + 1; x++) for (int yy = y - 1; yy <= y + 1; yy++) {
                if (x == cx && yy == y) continue;
                if (i == 1 && Math.abs(x - cx) + Math.abs(yy - y) == 2) continue; // the cap is narrower: no corners
                c.set(x, yy, z, "dndecor:boiler_structure[facing=north,size=normal]");
            }
            if (i == 2 || i == 4) {
                c.set(cx - 1, 43, z, "createdeco:industrial_iron_support[facing=up,waterlogged=false]");
                c.set(cx + 1, 43, z, "createdeco:industrial_iron_support[facing=up,waterlogged=false]");
            }
        }
        // Steam pipe from the top of the turbine to the ceiling
        for (int yy = 47; yy <= 58; yy++) c.set(cx, yy, z0 + dz * 3, "dndecor:industrial_boiler[axis=y,size=normal]");
        // Coupling with a flywheel on a bearing
        int zc = z0 + dz * 5;
        c.set(cx, y, zc, "dndecor:green_flywheel[axis=z]");
        c.set(cx, 44, zc, "create:industrial_iron_block");
        c.set(cx, 43, zc, "create:industrial_iron_block");
        // Generator housing i=6..8: 5 wide, 4 high, a 3-wide top; insulators on it
        for (int i = 6; i <= 8; i++) {
            int z = z0 + dz * i;
            for (int x = cx - 2; x <= cx + 2; x++) for (int yy = 43; yy <= 46; yy++) {
                boolean panel = (yy == 44 || yy == 45) && (x == cx - 2 || x == cx + 2) && i == 7;
                c.set(x, yy, z, panel ? "dndecor:green_sheet_metal" : "dndecor:green_dark_metal_plating");
            }
            for (int x = cx - 1; x <= cx + 1; x++) c.set(x, 47, z, "dndecor:green_dark_metal_block");
        }
        for (int i = 6; i <= 8; i++) c.set(cx - 1 + (i - 6), 48, z0 + dz * i, "tfmg:glass_cable_insulator[extension=false,facing=up,input_mode=false]");
        // Exciter at the end
        c.set(cx, y, z0 + dz * 9, "dndecor:copper_boiler[axis=z,size=normal]");
        c.set(cx, 44, z0 + dz * 9, "create:industrial_iron_block");
        c.set(cx, 43, z0 + dz * 9, "create:industrial_iron_block");
        // High-voltage signs on both long sides of the generator
        c.set(cx - 3, 45, z0 + dz * 7, "createdeco:decal_electrical[face=wall,facing=west]");
        c.set(cx + 3, 45, z0 + dz * 7, "createdeco:decal_electrical[face=wall,facing=east]");
    }

    static void units(Canvas c, boolean onlyNW) {
        unit(c, 758, 181, +1);            // north-west: turbine at the north wall, generator toward the middle
        if (onlyNW) return;
        unit(c, mx(758), 181, +1);        // north-east (mirror in x)
        unit(c, 758, mz(181), -1);        // south-west (mirror in z)
        unit(c, mx(758), mz(181), -1);    // south-east
    }

    /**
     * A channel cut into the inner layer of a wall (the user allowed drilling the walls), y44-46, one block deep, with a
     * copper pipe at y46 and two cable tubes at y44 and y45 in it: nothing sticks out into the walkway along the wall.
     * Only cells of the hall's plating are cut (checked against the region); the wall stays two blocks thick.
     */
    static void wallRun(Canvas c, boolean alongZ, int fixed, int from, int to) throws Exception {
        String axis = alongZ ? "z" : "x";
        int runStart = -1;
        for (int t = from; t <= to + 1; t++) {
            boolean ok = t <= to;
            int x = alongZ ? fixed : t, z = alongZ ? t : fixed;
            for (int y = 44; ok && y <= 46; y++) ok = Region.name(reg.state(x, y, z)).equals("dndecor:industrial_plating_block");
            if (ok && runStart < 0) runStart = t;
            if (!ok && runStart >= 0) {
                int a = runStart, b = t - 1;
                c.clear(alongZ ? fixed : a, 44, alongZ ? a : fixed, alongZ ? fixed : b, 46, alongZ ? b : fixed, "pipe channel in the wall");
                runStart = -1;
            }
            if (!ok) continue;
            c.set(x, 46, z, "dndecor:copper_boiler[axis=" + axis + ",size=small]");
            c.set(x, 45, z, "tfmg:cable_tube[axis=" + axis + ",concretelogged=false]");
            c.set(x, 44, z, "tfmg:cable_tube[axis=" + axis + ",concretelogged=false]");
        }
    }

    static void walls(Canvas c) throws Exception {
        wallRun(c, true, 753, 181, 211);      // west wall (inner layer x 753)
        wallRun(c, true, 787, 181, 211);      // east wall (x 787)
        wallRun(c, false, 179, 755, 768);     // north wall (z 179), up to the door at x 770-772
        wallRun(c, false, 179, 774, 785);
        wallRun(c, false, 213, 755, 764);     // south wall (z 213), around the door at x 766-768
        wallRun(c, false, 213, 770, 785);
    }

    /** Desks in the platform corners: 2-high corner block with a speedometer, 1-high arms (stressometers, dial up). */
    static void consoles(Canvas c) {
        int[][] corners = {{767, 193, 1, 1}, {774, 193, -1, 1}, {767, 200, 1, -1}, {774, 200, -1, -1}};
        for (int[] k : corners) {
            int x = k[0], z = k[1], sx = k[2], sz = k[3];
            String toward = sz > 0 ? "south" : "north";
            c.set(x, 49, z, "minecraft:smooth_stone");
            c.set(x, 50, z, "create:speedometer[axis_along_first=true,facing=" + toward + "]");
            c.set(x + sx, 49, z, "create:stressometer[axis_along_first=false,facing=up]");
            c.set(x, 49, z + sz, "create:stressometer[axis_along_first=false,facing=up]");
        }
    }

    static String decal(String what, String facing) {
        return "createdeco:decal_" + what + "[face=floor,facing=" + facing + "]";
    }

    static void hazard(Canvas c) {
        // Wall stairs: west x 754 climbing north from z 208 to 203 (head at catwalk 754 49 202), east x 786 climbing south from 185 to 190
        c.set(754, 42, 209, decal("warning", "north"));
        c.set(754, 49, 202, decal("warning", "north"));
        c.set(786, 42, 184, decal("warning", "south"));
        c.set(786, 49, 191, decal("warning", "south"));
        // Stairs from the lower ring up to the platform: foot on the ring (stand 46), head on the platform edge
        for (int z : new int[]{196, 197}) {
            c.set(763, 46, z, decal("warning", "east"));
            c.set(778, 46, z, decal("warning", "west"));
        }
    }

    public static void main(String[] a) throws Exception {
        reg = new Region(Path.of(a[0]));
        Set<String> parts = new HashSet<>(Arrays.asList(a[1].split(",")));
        Canvas c = new Canvas(0, 0);
        if (parts.contains("units")) units(c, false);
        if (parts.contains("unitNW")) units(c, true);
        if (parts.contains("walls")) walls(c);
        if (parts.contains("consoles")) consoles(c);
        if (parts.contains("hazard")) hazard(c);
        c.print(new String[0]);
    }
}
