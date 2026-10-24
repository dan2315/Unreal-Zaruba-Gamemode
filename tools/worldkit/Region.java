import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Full block states ("ns:id[key=value,...]", properties sorted) and block entities read from a dimension's region
 * files, for the tools that copy builds (Export, Trees, Find). Chunks are decoded once and kept.
 */
public class Region {
    final Path dir;
    final Map<Long, Decoded> chunks = new HashMap<>();

    record Decoded(Map<Integer, int[]> sections, Map<Integer, String[]> palettes, Map<Long, Map<String, Object>> blockEntities) {}

    public Region(Path dir) {
        this.dir = dir;
    }

    /** The state at a position: "minecraft:air" where nothing is (or no chunk). */
    public String state(int x, int y, int z) throws IOException {
        Decoded d = decoded(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (d == null) return "minecraft:air";
        int sy = Math.floorDiv(y, 16);
        int[] data = d.sections.get(sy);
        if (data == null) return "minecraft:air";
        String[] palette = d.palettes.get(sy);
        int idx = (Math.floorMod(y, 16) * 16 + Math.floorMod(z, 16)) * 16 + Math.floorMod(x, 16);
        return palette[data[idx]];
    }

    /** The block entity at a position (its NBT as read), or null. */
    public Map<String, Object> blockEntity(int x, int y, int z) throws IOException {
        Decoded d = decoded(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        return d == null ? null : d.blockEntities.get(BlockPosKey.of(x, y, z));
    }

    /** The highest y with a block that is not air, or Integer.MIN_VALUE. */
    public int top(int x, int z, java.util.function.Predicate<String> counts) throws IOException {
        Decoded d = decoded(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (d == null) return Integer.MIN_VALUE;
        List<Integer> ys = new ArrayList<>(d.sections.keySet());
        ys.sort(Comparator.reverseOrder());
        for (int sy : ys) {
            for (int ly = 15; ly >= 0; ly--) {
                String s = state(x, sy * 16 + ly, z);
                if (!isAir(s) && counts.test(s)) return sy * 16 + ly;
            }
        }
        return Integer.MIN_VALUE;
    }

    static boolean isAir(String s) {
        return s.equals("minecraft:air") || s.equals("minecraft:cave_air") || s.equals("minecraft:void_air");
    }

    static String name(String state) {
        int i = state.indexOf('[');
        return i < 0 ? state : state.substring(0, i);
    }

    @SuppressWarnings("unchecked")
    Decoded decoded(int cx, int cz) throws IOException {
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        if (chunks.containsKey(key)) return chunks.get(key);
        Map<String, Object> chunk = Surface.chunk(dir, cx, cz);
        Surface.cache.clear();
        Decoded result = null;
        if (chunk != null) {
            Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
            Map<Integer, int[]> sections = new HashMap<>();
            Map<Integer, String[]> palettes = new HashMap<>();
            for (Object o : (List<Object>) root.getOrDefault("sections", List.of())) {
                Map<String, Object> s = (Map<String, Object>) o;
                Map<String, Object> states = (Map<String, Object>) s.get("block_states");
                if (states == null) continue;
                List<Object> palette = (List<Object>) states.get("palette");
                long[] data = (long[]) states.get("data");
                int sy = (Byte) s.get("Y");
                String[] names = new String[palette.size()];
                for (int i = 0; i < names.length; i++) names[i] = stateString((Map<String, Object>) palette.get(i));
                int bits = palette.size() <= 1 ? 0 : Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                int[] values = new int[4096];
                if (bits > 0) {
                    int perLong = 64 / bits;
                    for (int idx = 0; idx < 4096; idx++) values[idx] = (int) ((data[idx / perLong] >>> ((idx % perLong) * bits)) & ((1L << bits) - 1));
                }
                sections.put(sy, values);
                palettes.put(sy, names);
            }
            Map<Long, Map<String, Object>> bes = new HashMap<>();
            Object list = root.get("block_entities");
            if (list == null) list = root.get("TileEntities");
            if (list instanceof List<?> l) {
                for (Object o : l) {
                    Map<String, Object> be = (Map<String, Object>) o;
                    bes.put(BlockPosKey.of((Integer) be.get("x"), (Integer) be.get("y"), (Integer) be.get("z")), be);
                }
            }
            result = new Decoded(sections, palettes, bes);
        }
        if (chunks.size() > 4096) chunks.clear();
        chunks.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    static String stateString(Map<String, Object> entry) {
        String name = (String) entry.get("Name");
        Map<String, Object> props = (Map<String, Object>) entry.get("Properties");
        if (props == null || props.isEmpty()) return name;
        StringBuilder sb = new StringBuilder(name).append('[');
        new TreeMap<>(props).forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
        sb.setLength(sb.length() - 1);
        return sb.append(']').toString();
    }

    /** SNBT of a block entity without its position (the block it sits on gives that). */
    static String snbt(Map<String, Object> be) {
        Map<String, Object> copy = new LinkedHashMap<>(be);
        copy.remove("x"); copy.remove("y"); copy.remove("z"); copy.remove("keepPacked");
        return snbtValue(copy);
    }

    @SuppressWarnings("unchecked")
    static String snbtValue(Object v) {
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            for (var e : ((Map<String, Object>) m).entrySet()) {
                if (sb.length() > 1) sb.append(',');
                String k = e.getKey();
                sb.append(k.matches("[A-Za-z0-9_.+-]+") ? k : quote(k)).append(':').append(snbtValue(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[");
            for (Object o : l) {
                if (sb.length() > 1) sb.append(',');
                sb.append(snbtValue(o));
            }
            return sb.append(']').toString();
        }
        if (v instanceof String s) return quote(s);
        if (v instanceof Byte b) return b + "b";
        if (v instanceof Short s) return s + "s";
        if (v instanceof Integer i) return String.valueOf(i);
        if (v instanceof Long l) return l + "L";
        if (v instanceof Float f) return f + "f";
        if (v instanceof Double d) return d + "d";
        if (v instanceof byte[] a) { StringBuilder sb = new StringBuilder("[B;"); for (int i = 0; i < a.length; i++) sb.append(i > 0 ? "," : "").append(a[i]).append('b'); return sb.append(']').toString(); }
        if (v instanceof int[] a) { StringBuilder sb = new StringBuilder("[I;"); for (int i = 0; i < a.length; i++) sb.append(i > 0 ? "," : "").append(a[i]); return sb.append(']').toString(); }
        if (v instanceof long[] a) { StringBuilder sb = new StringBuilder("[L;"); for (int i = 0; i < a.length; i++) sb.append(i > 0 ? "," : "").append(a[i]).append('L'); return sb.append(']').toString(); }
        return String.valueOf(v);
    }

    static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static final class BlockPosKey {
        static long of(int x, int y, int z) {
            return ((long) (y + 2048) << 40) | ((long) (z + 524288) << 20) | (x + 524288);
        }
    }
}
