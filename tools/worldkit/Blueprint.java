import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * The worldkit blueprint: a box of blocks copied from the map, kept in tools/worldkit/blueprints/ as a library of
 * reusable pieces (houses, trees, props). Text, one file per piece:
 *
 * <pre>
 * # worldkit blueprint v1
 * name rural_house_a
 * size 12 9 10              width (x), height (y), depth (z)
 * ground 1                  layer of the box that sits on the ground (layers below it are foundation/cellar)
 * source -500 31 100        where its lowest north-west corner was taken from (informational)
 * tags house rural          free words for finding pieces
 * palette
 * 0 minecraft:stone_bricks
 * 1 minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]
 * 2 minecraft:chest[facing=east,type=single,waterlogged=false]{Items:[]}
 * layer 0                   then `depth` rows of `width` tokens: a palette number, or . (nothing: left as it is)
 * 0 0 0 . . 1 ...
 * </pre>
 *
 * Placing (Stamp) is additive like every build: blocks go only into air and replaceable plants.
 */
public class Blueprint {
    String name = "unnamed";
    int w, h, d;
    int ground;
    int[] source = {0, 0, 0};
    List<String> tags = new ArrayList<>();
    List<String> palette = new ArrayList<>();
    int[][][] cells; // [y][z][x], -1 = nothing

    Blueprint(int w, int h, int d) {
        this.w = w; this.h = h; this.d = d;
        cells = new int[h][d][w];
        for (int[][] l : cells) for (int[] r : l) Arrays.fill(r, -1);
    }

    int index(String state) {
        int i = palette.indexOf(state);
        if (i < 0) { palette.add(state); i = palette.size() - 1; }
        return i;
    }

    String at(int x, int y, int z) {
        int v = cells[y][z][x];
        return v < 0 ? null : palette.get(v);
    }

    int count() {
        int n = 0;
        for (int[][] l : cells) for (int[] r : l) for (int v : r) if (v >= 0) n++;
        return n;
    }

    void save(Path file) throws IOException {
        StringBuilder sb = new StringBuilder("# worldkit blueprint v1\n");
        sb.append("name ").append(name).append('\n');
        sb.append("size ").append(w).append(' ').append(h).append(' ').append(d).append('\n');
        sb.append("ground ").append(ground).append('\n');
        sb.append("source ").append(source[0]).append(' ').append(source[1]).append(' ').append(source[2]).append('\n');
        if (!tags.isEmpty()) sb.append("tags ").append(String.join(" ", tags)).append('\n');
        sb.append("palette\n");
        for (int i = 0; i < palette.size(); i++) sb.append(i).append(' ').append(palette.get(i)).append('\n');
        for (int y = 0; y < h; y++) {
            sb.append("layer ").append(y).append('\n');
            for (int z = 0; z < d; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < w; x++) {
                    if (x > 0) row.append(' ');
                    int v = cells[y][z][x];
                    row.append(v < 0 ? "." : String.valueOf(v));
                }
                sb.append(row).append('\n');
            }
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, sb.toString());
    }

    static Blueprint load(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        Blueprint bp = null;
        String name = "unnamed";
        int ground = 0;
        int[] source = {0, 0, 0};
        List<String> tags = new ArrayList<>();
        List<String> palette = new ArrayList<>();
        int i = 0;
        for (; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.startsWith("#") || l.isBlank()) continue;
            String[] p = l.split(" ", 2);
            switch (p[0]) {
                case "name" -> name = p[1].trim();
                case "size" -> { String[] s = p[1].trim().split(" "); bp = new Blueprint(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])); }
                case "ground" -> ground = Integer.parseInt(p[1].trim());
                case "source" -> { String[] s = p[1].trim().split(" "); source = new int[] {Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])}; }
                case "tags" -> tags.addAll(Arrays.asList(p[1].trim().split(" ")));
                case "palette" -> {
                    for (i++; i < lines.size() && !lines.get(i).startsWith("layer "); i++) {
                        String e = lines.get(i);
                        int sp = e.indexOf(' ');
                        palette.add(e.substring(sp + 1));
                    }
                    i--;
                }
                case "layer" -> {
                    int y = Integer.parseInt(p[1].trim());
                    for (int z = 0; z < bp.d; z++) {
                        String[] t = lines.get(++i).split(" ");
                        for (int x = 0; x < bp.w; x++) bp.cells[y][z][x] = t[x].equals(".") ? -1 : Integer.parseInt(t[x]);
                    }
                }
                default -> throw new IOException("bad blueprint line: " + l);
            }
        }
        bp.name = name;
        bp.ground = ground;
        bp.source = source;
        bp.tags = tags;
        bp.palette = palette;
        return bp;
    }

    /** Turned by quarter turns clockwise seen from above (north -> east): coordinates and the block states. */
    Blueprint rotated(int quarters) {
        int q = Math.floorMod(quarters, 4);
        if (q == 0) return this;
        boolean odd = q % 2 == 1;
        Blueprint r = new Blueprint(odd ? d : w, h, odd ? w : d);
        r.name = name; r.ground = ground; r.source = source; r.tags = tags;
        for (String s : palette) r.palette.add(rotateState(s, q));
        for (int y = 0; y < h; y++) for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
            // clockwise quarter turn of the box: (x, z) -> (D-1-z, x) with D the depth before the turn
            int cx = x, cz = z, cw = w, cd = d;
            for (int k = 0; k < q; k++) { int nx2 = cd - 1 - cz; int nz2 = cx; cx = nx2; cz = nz2; int tw = cw; cw = cd; cd = tw; }
            r.cells[y][cz][cx] = cells[y][z][x];
        }
        return r;
    }

    static final String[] DIRS = {"north", "east", "south", "west"};

    static String rotateState(String state, int q) {
        int open = state.indexOf('[');
        if (open < 0) return state;
        int close = state.indexOf(']', open);
        String head = state.substring(0, open), tail = state.substring(close + 1);
        TreeMap<String, String> props = new TreeMap<>();
        for (String kv : state.substring(open + 1, close).split(",")) {
            int eq = kv.indexOf('=');
            props.put(kv.substring(0, eq), kv.substring(eq + 1));
        }
        TreeMap<String, String> out = new TreeMap<>();
        for (var e : props.entrySet()) {
            String k = e.getKey(), v = e.getValue();
            int di = Arrays.asList(DIRS).indexOf(k);
            if (di >= 0) { out.put(DIRS[(di + q) % 4], v); continue; }
            if (k.equals("facing") || k.equals("horizontal_facing")) {
                int vi = Arrays.asList(DIRS).indexOf(v);
                out.put(k, vi >= 0 ? DIRS[(vi + q) % 4] : v);
                continue;
            }
            if ((k.equals("axis") || k.equals("horizontal_axis")) && q % 2 == 1) {
                out.put(k, v.equals("x") ? "z" : v.equals("z") ? "x" : v);
                continue;
            }
            if (k.equals("rotation")) {
                try { out.put(k, String.valueOf(Math.floorMod(Integer.parseInt(v) + 4 * q, 16))); continue; } catch (NumberFormatException ignored) {}
            }
            out.put(k, v);
        }
        StringBuilder sb = new StringBuilder(head).append('[');
        out.forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
        sb.setLength(sb.length() - 1);
        return sb.append(']').append(tail).toString();
    }
}
