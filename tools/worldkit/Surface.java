import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Top block (name and y) over an area of a dimension's region files, plus counts of every block above a y level.
 * Usage: java Surface <region dir> <x0> <z0> <x1> <z1> <step> <aboveY>
 */
public class Surface {
    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        int step = Integer.parseInt(a[5]), aboveY = Integer.parseInt(a[6]);
        Map<String, Integer> topCounts = new TreeMap<>(), aboveCounts = new TreeMap<>();
        StringBuilder grid = new StringBuilder();
        Map<String, Character> legend = new LinkedHashMap<>();
        for (int z = z0; z <= z1; z += step) {
            for (int x = x0; x <= x1; x += step) {
                String[] top = top(dir, x, z);
                String name = top == null ? "?" : top[0].replace("minecraft:", "");
                topCounts.merge(name + " y=" + (top == null ? "?" : top[1]), 1, Integer::sum);
                char c = legend.computeIfAbsent(name, k -> (char) ('A' + legend.size()));
                grid.append(c);
            }
            grid.append('\n');
        }
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            for (Map.Entry<Integer, String> e : column(dir, x, z).entrySet()) {
                if (e.getKey() > aboveY) aboveCounts.merge(e.getValue().replace("minecraft:", ""), 1, Integer::sum);
            }
        }
        System.out.println("TOP: " + topCounts);
        System.out.println("LEGEND: " + legend);
        System.out.print(grid);
        System.out.println("ABOVE y=" + aboveY + ": " + aboveCounts);
    }

    static Map<Long, Map<String, Object>> cache = new HashMap<>();

    static String[] top(Path dir, int x, int z) throws IOException {
        TreeMap<Integer, String> col = column(dir, x, z);
        if (col.isEmpty()) return null;
        var last = col.lastEntry();
        return new String[] {last.getValue(), String.valueOf(last.getKey())};
    }

    /** Non-air blocks in a column, by y. */
    @SuppressWarnings("unchecked")
    static TreeMap<Integer, String> column(Path dir, int x, int z) throws IOException {
        TreeMap<Integer, String> out = new TreeMap<>();
        Map<String, Object> chunk = chunk(dir, Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (chunk == null) return out;
        Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
        int lx = Math.floorMod(x, 16), lz = Math.floorMod(z, 16);
        for (Object o : (List<Object>) root.getOrDefault("sections", List.of())) {
            Map<String, Object> s = (Map<String, Object>) o;
            Map<String, Object> states = (Map<String, Object>) s.get("block_states");
            if (states == null) continue;
            List<Object> palette = (List<Object>) states.get("palette");
            long[] data = (long[]) states.get("data");
            int sy = (Byte) s.get("Y");
            int bits = palette.size() <= 1 ? 0 : Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            for (int ly = 0; ly < 16; ly++) {
                int idx = (ly * 16 + lz) * 16 + lx;
                int v = 0;
                if (bits > 0) {
                    int perLong = 64 / bits;
                    v = (int) ((data[idx / perLong] >>> ((idx % perLong) * bits)) & ((1L << bits) - 1));
                }
                String name = (String) ((Map<String, Object>) palette.get(v)).get("Name");
                if (!name.endsWith(":air") && !name.endsWith("cave_air")) out.put(sy * 16 + ly, name);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> chunk(Path dir, int cx, int cz) throws IOException {
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        if (cache.containsKey(key)) return cache.get(key);
        Path mca = dir.resolve("r." + Math.floorDiv(cx, 32) + "." + Math.floorDiv(cz, 32) + ".mca");
        Map<String, Object> result = null;
        if (Files.exists(mca)) {
            byte[] data = Files.readAllBytes(mca);
            int i = Math.floorMod(cx, 32) + Math.floorMod(cz, 32) * 32;
            int loc = ((data[i * 4] & 0xFF) << 16) | ((data[i * 4 + 1] & 0xFF) << 8) | (data[i * 4 + 2] & 0xFF);
            if (loc != 0) {
                int off = loc * 4096;
                int len = ((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16) | ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
                int type = data[off + 4];
                InputStream raw = new ByteArrayInputStream(data, off + 5, len - 1);
                InputStream in = type == 1 ? new GZIPInputStream(raw) : type == 2 ? new InflaterInputStream(raw) : raw;
                DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
                dis.readByte();
                dis.readUTF();
                result = (Map<String, Object>) ScanWorld.read(dis, 10);
            }
        }
        cache.put(key, result);
        return result;
    }
}
