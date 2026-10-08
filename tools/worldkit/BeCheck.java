import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * For every block entity in a world's region files: which block it sits on, counted as "beId @ blockId".
 * Also prints one full example NBT (top-level keys) per pair.
 * Usage: java BeCheck <world dir> [beIdFilter]
 */
public class BeCheck {
    static Map<String, Integer> pairs = new TreeMap<>();
    static Map<String, String> example = new HashMap<>();
    static String filter;

    public static void main(String[] a) throws Exception {
        filter = a.length > 1 ? a[1] : null;
        List<Path> dirs = new ArrayList<>();
        try (var w = Files.walk(Path.of(a[0]))) {
            w.filter(p -> Files.isDirectory(p) && p.getFileName().toString().equals("region")).forEach(dirs::add);
        }
        for (Path d : dirs) try (var f = Files.list(d)) {
            for (Path mca : f.filter(p -> p.toString().endsWith(".mca")).toList()) scan(mca);
        }
        pairs.forEach((k, v) -> System.out.println(v + "  " + k + "   e.g. " + example.get(k)));
    }

    @SuppressWarnings("unchecked")
    static void scan(Path mca) throws IOException {
        byte[] data = Files.readAllBytes(mca);
        if (data.length < 8192) return;
        for (int i = 0; i < 1024; i++) {
            int loc = ((data[i * 4] & 0xFF) << 16) | ((data[i * 4 + 1] & 0xFF) << 8) | (data[i * 4 + 2] & 0xFF);
            if (loc == 0) continue;
            int off = loc * 4096;
            if (off + 5 > data.length) continue;
            int len = ((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16) | ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
            int type = data[off + 4];
            if (len <= 1 || off + 4 + len > data.length) continue;
            InputStream raw = new ByteArrayInputStream(data, off + 5, len - 1);
            InputStream in = type == 1 ? new GZIPInputStream(raw) : type == 2 ? new InflaterInputStream(raw) : raw;
            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
                if (dis.readByte() != 10) continue;
                dis.readUTF();
                Map<String, Object> chunk = (Map<String, Object>) ScanWorld.read(dis, 10);
                Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
                List<Object> bes = (List<Object>) root.getOrDefault("block_entities", root.getOrDefault("TileEntities", List.of()));
                if (bes.isEmpty()) continue;
                List<Object> sections = (List<Object>) root.getOrDefault("sections", List.of());
                for (Object o : bes) {
                    Map<String, Object> be = (Map<String, Object>) o;
                    String id = (String) be.get("id");
                    if (id == null || (filter != null && !id.startsWith(filter))) continue;
                    int x = (Integer) be.get("x"), y = (Integer) be.get("y"), z = (Integer) be.get("z");
                    String block = blockAt(sections, x, y, z);
                    String key = id + " @ " + block;
                    pairs.merge(key, 1, Integer::sum);
                    example.putIfAbsent(key, x + " " + y + " " + z + " keys=" + new TreeSet<>(be.keySet()));
                }
            } catch (Exception e) {
                // skip
            }
        }
    }

    @SuppressWarnings("unchecked")
    static String blockAt(List<Object> sections, int x, int y, int z) {
        int sy = Math.floorDiv(y, 16);
        for (Object o : sections) {
            Map<String, Object> s = (Map<String, Object>) o;
            if (!(s.get("Y") instanceof Byte b) || b != sy) continue;
            Map<String, Object> states = (Map<String, Object>) s.get("block_states");
            if (states == null) return "?";
            List<Object> palette = (List<Object>) states.get("palette");
            long[] data = (long[]) states.get("data");
            int idx = (Math.floorMod(y, 16) * 16 + Math.floorMod(z, 16)) * 16 + Math.floorMod(x, 16);
            int v = 0;
            if (palette.size() > 1) {
                int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                int perLong = 64 / bits;
                v = (int) ((data[idx / perLong] >>> ((idx % perLong) * bits)) & ((1L << bits) - 1));
            }
            return (String) ((Map<String, Object>) palette.get(v)).get("Name");
        }
        return "?";
    }
}
