import java.util.*;

/**
 * Additive build canvas shared by the generators. Generators draw blocks in memory, coordinates relative to a centre;
 * a later draw replaces an earlier one at the same spot, and drawing "air" erases (the world is left as it is there).
 * Nothing is ever cleared unless asked:
 *   - normal blocks only go where the world has air, water, grass, flowers and other replaceable plants;
 *   - ground() blocks may also replace natural ground (dirt, grass block, sand, gravel), for floors and paths;
 *   - clear() records a box that must be emptied (trees, a doorway into an existing wall). The clears are listed on
 *     stderr and only run when asked (/buildkit apply <name> clear).
 * Blocks are merged into rectangles per layer; blocks hanging on others (ladders, lanterns, signs...) go last.
 * Output is a build file for the mod (/buildkit apply, check, undo), which also reports what was in the way.
 */
public class Canvas {
    static final String[] PLACE = {"#minecraft:replaceable", "#minecraft:small_flowers", "#minecraft:tall_flowers"};
    static final String[] GROUND = {"#minecraft:replaceable", "#minecraft:small_flowers", "#minecraft:tall_flowers",
            "#minecraft:dirt", "#minecraft:sand", "minecraft:gravel"};
    static final String[] ATTACHED = {"ladder", "lantern", "torch", "sign", "banner", "button", "lever", "lightning_rod", "valve_handle"};

    record Cell(String block, String[] filter) {}
    record Clear(int x0, int y0, int z0, int x1, int y1, int z1, String reason) {}

    final int cx, cz;
    final Map<Long, Cell> cells = new HashMap<>();
    final List<Clear> clears = new ArrayList<>();
    boolean commands;

    public Canvas(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        draw(x0, y0, z0, x1, y1, z1, block, PLACE);
    }

    public void set(int x, int y, int z, String block) {
        draw(x, y, z, x, y, z, block, PLACE);
    }

    public void ground(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        draw(x0, y0, z0, x1, y1, z1, block, GROUND);
    }

    public void clear(int x0, int y0, int z0, int x1, int y1, int z1, String reason) {
        clears.add(new Clear(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), reason));
    }

    void draw(int x0, int y0, int z0, int x1, int y1, int z1, String block, String[] filter) {
        String full = id(block);
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    if (full.equals("minecraft:air")) cells.remove(key(x, y, z));
                    else cells.put(key(x, y, z), new Cell(full, filter));
                }
    }

    /**
     * Prints the build. Default: a build file for the mod's /buildkit command (copy it to unrealzaruba/builds/<name>.build;
     * clears are in the file and run with "/buildkit apply <name> clear"). With --commands: RCON fill commands for the
     * match dimension instead, where --clear also runs the clears (check them with the Skipped tool).
     */
    public void print(String[] args) {
        commands = Arrays.asList(args).contains("--commands");
        boolean doClear = Arrays.asList(args).contains("--clear");
        List<String> out = new ArrayList<>();
        int[] box = bounds();
        String forceload = (cx + box[0]) + " " + (cz + box[2]) + " " + (cx + box[1]) + " " + (cz + box[3]);
        if (commands) out.add("forceload add " + forceload);
        for (Clear c : clears) {
            String area = (cx + c.x0) + " " + c.y0 + " " + (cz + c.z0) + " " + (cx + c.x1) + " " + c.y1 + " " + (cz + c.z1);
            System.err.println("clear (" + (commands && !doClear ? "not run, --clear to run" : commands ? "runs" : "runs with 'apply <name> clear'") + "): " + c.reason + "  " + area);
            if (!commands) {
                out.add("clear " + area + " " + c.reason);
            } else if (doClear) {
                int layers = Math.max(1, 32768 / ((c.x1 - c.x0 + 1) * (c.z1 - c.z0 + 1)));
                for (int y = c.y0; y <= c.y1; y += layers) {
                    out.add("fill " + (cx + c.x0) + " " + y + " " + (cz + c.z0) + " " + (cx + c.x1) + " " + Math.min(c.y1, y + layers - 1) + " " + (cz + c.z1) + " minecraft:air");
                }
            }
        }
        emit(out, false);
        emit(out, true);
        if (commands) {
            out.add("forceload remove " + forceload);
            out.forEach(c -> System.out.println("execute in unrealzaruba:game_dim run " + c));
        } else {
            System.out.println("# worldkit build: clear|place|ground x0 y0 z0 x1 y1 z1 reason|block (see BuildKit in the mod)");
            out.forEach(System.out::println);
        }
    }

    // rectangles per layer (bottom up): runs along x, extended along z while the whole row matches
    void emit(List<String> out, boolean attached) {
        TreeMap<Integer, List<Long>> layers = new TreeMap<>();
        for (long k : cells.keySet()) {
            if (isAttached(cells.get(k).block) == attached) layers.computeIfAbsent(y(k), y -> new ArrayList<>()).add(k);
        }
        for (var layer : layers.entrySet()) {
            int y = layer.getKey();
            Set<Long> done = new HashSet<>();
            List<Long> keys = layer.getValue();
            keys.sort(Comparator.comparingInt(Canvas::z).thenComparingInt(Canvas::x));
            for (long k : keys) {
                if (done.contains(k)) continue;
                Cell cell = cells.get(k);
                int x0 = x(k), z0 = z(k), x1 = x0;
                while (same(x1 + 1, y, z0, cell, done)) x1++;
                int z1 = z0;
                rows:
                while (true) {
                    for (int x = x0; x <= x1; x++) if (!same(x, y, z1 + 1, cell, done)) break rows;
                    z1++;
                }
                for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) done.add(key(x, y, z));
                String area = (cx + x0) + " " + y + " " + (cz + z0) + " " + (cx + x1) + " " + y + " " + (cz + z1);
                if (!commands) out.add((cell.filter == GROUND ? "ground " : "place ") + area + " " + cell.block);
                else for (String f : cell.filter) out.add("fill " + area + " " + cell.block + " replace " + f);
            }
        }
    }

    boolean same(int x, int y, int z, Cell cell, Set<Long> done) {
        long k = key(x, y, z);
        Cell other = cells.get(k);
        return other != null && !done.contains(k) && other.block.equals(cell.block) && other.filter == cell.filter;
    }

    int[] bounds() {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : cells.keySet()) {
            minX = Math.min(minX, x(k)); maxX = Math.max(maxX, x(k));
            minZ = Math.min(minZ, z(k)); maxZ = Math.max(maxZ, z(k));
        }
        for (Clear c : clears) {
            minX = Math.min(minX, c.x0); maxX = Math.max(maxX, c.x1);
            minZ = Math.min(minZ, c.z0); maxZ = Math.max(maxZ, c.z1);
        }
        return new int[] {minX, maxX, minZ, maxZ};
    }

    static boolean isAttached(String block) {
        String name = block.substring(block.indexOf(':') + 1);
        int end = name.indexOf('[') >= 0 ? name.indexOf('[') : name.indexOf('{') >= 0 ? name.indexOf('{') : name.length();
        name = name.substring(0, end);
        for (String a : ATTACHED) if (name.endsWith(a)) return true;
        return false;
    }

    static String id(String block) {
        int end = block.length();
        for (char c : new char[] {'[', '{'}) if (block.indexOf(c) >= 0) end = Math.min(end, block.indexOf(c));
        return block.substring(0, end).contains(":") ? block : "minecraft:" + block;
    }

    // key: y in the top bits, then z and x, each offset to stay positive
    static long key(int x, int y, int z) {
        return ((long) (y + 2048) << 40) | ((long) (z + 524288) << 20) | (x + 524288);
    }

    static int x(long k) { return (int) (k & 0xFFFFF) - 524288; }
    static int z(long k) { return (int) ((k >> 20) & 0xFFFFF) - 524288; }
    static int y(long k) { return (int) (k >> 40) - 2048; }
}
