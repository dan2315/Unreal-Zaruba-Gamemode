import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Block counts per id and per property set for the given namespaces, plus references to them inside block entity
 * NBT (copycat materials, items). Usage: java Palette <world dir> <ns1,ns2,...>
 */
public class Palette {
    static Set<String> namespaces;
    static Map<String, long[]> ids = new TreeMap<>();
    static Map<String, Map<String, Long>> props = new TreeMap<>();
    static Map<String, Long> nbtRefs = new TreeMap<>();

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        namespaces = new HashSet<>(Arrays.asList(a[1].split(",")));
        List<Path> dirs = new ArrayList<>();
        try (var w = Files.walk(Path.of(a[0]))) {
            w.filter(p -> Files.isDirectory(p) && p.getFileName().toString().equals("region")).forEach(dirs::add);
        }
        for (Path d : dirs) try (var f = Files.list(d)) {
            for (Path mca : f.filter(p -> p.toString().endsWith(".mca")).toList()) scan(mca);
        }
        System.out.println("BLOCKS (count, id, property sets)");
        ids.forEach((id, c) -> {
            System.out.println(c[0] + "\t" + id);
            props.get(id).entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(6)
                    .forEach(e -> System.out.println("\t\t" + e.getValue() + "\t" + e.getKey()));
        });
        System.out.println("NBT REFERENCES (string values inside block entities)");
        nbtRefs.forEach((k, v) -> System.out.println(v + "\t" + k));
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
                for (Object o : (List<Object>) root.getOrDefault("sections", List.of())) {
                    Map<String, Object> s = (Map<String, Object>) o;
                    Map<String, Object> states = (Map<String, Object>) s.get("block_states");
                    if (states == null) continue;
                    List<Object> palette = (List<Object>) states.get("palette");
                    long[] packed = (long[]) states.get("data");
                    long[] counts = new long[palette.size()];
                    if (packed == null || palette.size() == 1) counts[0] = 4096;
                    else {
                        int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                        int per = 64 / bits;
                        long mask = (1L << bits) - 1;
                        for (int idx = 0; idx < 4096; idx++) {
                            int v = (int) ((packed[idx / per] >>> ((idx % per) * bits)) & mask);
                            if (v < counts.length) counts[v]++;
                        }
                    }
                    for (int p = 0; p < palette.size(); p++) {
                        if (counts[p] == 0) continue;
                        Map<String, Object> st = (Map<String, Object>) palette.get(p);
                        String name = (String) st.get("Name");
                        if (!wanted(name)) continue;
                        ids.computeIfAbsent(name, k -> new long[1])[0] += counts[p];
                        Object pr = st.get("Properties");
                        String key = pr == null ? "{}" : new TreeMap<>((Map<String, Object>) pr).toString();
                        props.computeIfAbsent(name, k -> new HashMap<>()).merge(key, counts[p], Long::sum);
                    }
                }
                for (Object o : (List<Object>) root.getOrDefault("block_entities", List.of())) refs(o, "");
            } catch (Exception e) {
                // skip
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void refs(Object v, String path) {
        if (v instanceof Map<?, ?> m) ((Map<String, Object>) m).forEach((k, x) -> refs(x, path.isEmpty() ? k : path + "." + k));
        else if (v instanceof List<?> l) l.forEach(x -> refs(x, path + "[]"));
        else if (v instanceof String s && wanted(s)) nbtRefs.merge(path.replaceAll("\\.(bottom|top|[a-z]+_[a-z]+)\\.", ".<part>.") + " = " + s, 1L, Long::sum);
    }

    static boolean wanted(String id) {
        int c = id.indexOf(':');
        return c > 0 && namespaces.contains(id.substring(0, c));
    }
}
