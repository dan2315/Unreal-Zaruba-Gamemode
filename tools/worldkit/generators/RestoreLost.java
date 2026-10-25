import java.nio.file.*;
import java.util.*;

/**
 * Puts back blocks of mods this modpack no longer has, which turned into air when the map moved to 1.21: wherever an
 * old version of the map (e.g. the user's game_dim.v2, 1.20.1) has one of them and the map now has air, the nearest
 * block of the current modpack is placed (additive: only into air).
 *   s_a_b:lightsteelblock              -> create:industrial_iron_block
 *   s_a_b:whitehardsteelblock          -> dndecor:white_dark_metal_block
 *   s_a_b:sandbag                      -> minecraft:packed_mud (the colour of sandbags; dndecor beige is not registered)
 *   cbcmodernwarfare:apds_shot         -> tfmg:glass_cable_insulator, same facing (they stood as insulators)
 *   cbcmodernwarfare:solid_fuel_thruster -> dndecor:industrial_boiler size small along the same axis
 *   create_connected:item_silo         -> create:fluid_tank (column, top and bottom from the old neighbours)
 *   design_decor:<x>_sign              -> createdeco:decal_<warning|electrical|creeper> on the wall, same facing
 * Signs are placed only where the block they hang on is a full block, now or in this build: a decal without a sturdy
 * face drops as an item at the first neighbour update (and items get saved into the template).
 * Usage: java RestoreLost <old region dir> <current region dir> x0 y0 z0 x1 y1 z1 [--no-signs]
 */
public class RestoreLost {
    static final Map<String, String> DECALS = Map.of("warning_sign", "warning", "magnet_sign", "electrical", "creeper_sign", "creeper",
            "danger_sign", "warning", "skull_sign", "skull", "radioactive_sign", "radioactive", "fire_sign", "fire");

    static String prop(String state, String key) {
        int i = state.indexOf(key + "=");
        if (i < 0) return null;
        int e = i + key.length() + 1, j = e;
        while (j < state.length() && state.charAt(j) != ',' && state.charAt(j) != ']') j++;
        return state.substring(e, j);
    }

    static String axisOf(String facing) {
        return facing == null ? "y" : switch (facing) { case "north", "south" -> "z"; case "east", "west" -> "x"; default -> "y"; };
    }

    /** The replacement, or null for blocks this tool does not handle. */
    static String replace(Region old, int x, int y, int z, String s) throws Exception {
        String n = Region.name(s), f = prop(s, "facing");
        switch (n) {
            case "s_a_b:lightsteelblock": return "create:industrial_iron_block";
            case "s_a_b:whitehardsteelblock": return "dndecor:white_dark_metal_block";
            case "s_a_b:sandbag": return "minecraft:packed_mud";
            case "cbcmodernwarfare:apds_shot": return "tfmg:glass_cable_insulator[extension=false,facing=" + (f == null ? "up" : f) + ",input_mode=false]";
            case "cbcmodernwarfare:solid_fuel_thruster": return "dndecor:industrial_boiler[axis=" + axisOf(f) + ",size=small]";
            case "create_connected:item_silo": {
                boolean below = Region.name(old.state(x, y - 1, z)).equals(n), above = Region.name(old.state(x, y + 1, z)).equals(n);
                return "create:fluid_tank[bottom=" + !below + ",shape=window,top=" + !above + "]";
            }
        }
        if (n.startsWith("design_decor:") && n.endsWith("_sign")) {
            String d = DECALS.getOrDefault(n.substring("design_decor:".length()), "warning");
            return "createdeco:decal_" + d + "[face=wall,facing=" + (f == null ? "north" : f) + "]";
        }
        return null;
    }

    static final String[] NOT_FULL = {"slab", "stairs", "_wall", "fence", "bars", "pane", "catwalk", "boiler", "support", "door",
            "ladder", "sign", "decal", "insulator", "chain", "lantern", "lamp", "girder", "pipe", "shaft", "button", "rail", "carpet",
            "glass", "leaves", "copycat", "air", "water"};

    static boolean full(String state) {
        String n = Region.name(state);
        for (String s : NOT_FULL) if (n.contains(s)) return false;
        return true;
    }

    public static void main(String[] a) throws Exception {
        Region old = new Region(Path.of(a[0])), cur = new Region(Path.of(a[1]));
        int x0 = Integer.parseInt(a[2]), y0 = Integer.parseInt(a[3]), z0 = Integer.parseInt(a[4]);
        int x1 = Integer.parseInt(a[5]), y1 = Integer.parseInt(a[6]), z1 = Integer.parseInt(a[7]);
        boolean noSigns = Arrays.asList(a).contains("--no-signs");
        Canvas c = new Canvas(0, 0);
        Map<Long, String> placed = new HashMap<>();
        List<int[]> signs = new ArrayList<>();
        List<String> signStates = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            String s = old.state(x, y, z);
            String r = replace(old, x, y, z, s);
            if (r == null || !Region.isAir(cur.state(x, y, z))) continue;
            if (r.contains("decal_") && noSigns) continue;
            if (r.contains("decal_")) { signs.add(new int[]{x, y, z}); signStates.add(r); continue; }
            c.set(x, y, z, r);
            placed.put(Canvas.key(x, y, z), r);
            counts.merge(Region.name(s) + " -> " + Region.name(r), 1, Integer::sum);
        }
        // A wall decal facing f hangs on the block behind it (opposite of f)
        for (int i = 0; i < signs.size(); i++) {
            int[] p = signs.get(i);
            String f = prop(signStates.get(i), "facing");
            int bx = p[0] - (f.equals("east") ? 1 : f.equals("west") ? -1 : 0), bz = p[2] - (f.equals("south") ? 1 : f.equals("north") ? -1 : 0);
            String behind = placed.getOrDefault(Canvas.key(bx, p[1], bz), cur.state(bx, p[1], bz));
            if (!full(behind)) {
                System.err.println("  sign at " + p[0] + " " + p[1] + " " + p[2] + " left out: it would hang on " + behind);
                continue;
            }
            c.set(p[0], p[1], p[2], signStates.get(i));
            counts.merge("sign -> " + Region.name(signStates.get(i)), 1, Integer::sum);
        }
        counts.forEach((k, v) -> System.err.println(v + "  " + k));
        c.print(new String[0]);
    }
}
