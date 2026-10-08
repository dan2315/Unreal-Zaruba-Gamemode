import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Counts blocks and block entities per namespace:id in a world's region files.
 * Usage: java ScanWorld.java <world dir> <comma-separated namespaces to ignore>
 * Prints: per id the block count, chunk count, and an example position; plus chunk DataVersions.
 */
public class ScanWorld {
    static Set<String> ignore;
    static Map<String, long[]> blocks = new TreeMap<>();      // id -> {count, chunks}
    static Map<String, String> example = new HashMap<>();
    static Map<String, long[]> blockEntities = new TreeMap<>();
    static Map<Integer, Integer> versions = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        Path world = Path.of(args[0]);
        ignore = new HashSet<>(Arrays.asList(args.length > 1 ? args[1].split(",") : new String[0]));
        List<Path> regionDirs = new ArrayList<>();
        try (var walk = Files.walk(world)) {
            walk.filter(p -> Files.isDirectory(p) && p.getFileName().toString().equals("region")).forEach(regionDirs::add);
        }
        for (Path dir : regionDirs) {
            try (var files = Files.list(dir)) {
                for (Path mca : files.filter(p -> p.toString().endsWith(".mca")).toList()) {
                    scanRegion(world.relativize(dir).toString(), mca);
                }
            }
        }
        System.out.println("DataVersions (chunks): " + versions);
        System.out.println("BLOCKS");
        blocks.forEach((id, c) -> System.out.println("  " + id + "  blocks=" + c[0] + " chunks=" + c[1] + "  e.g. " + example.get(id)));
        System.out.println("BLOCK_ENTITIES");
        blockEntities.forEach((id, c) -> System.out.println("  " + id + "  count=" + c[0] + "  e.g. " + example.get("be:" + id)));
    }

    static void scanRegion(String dimension, Path mca) throws IOException {
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
                byte tag = dis.readByte();
                if (tag != 10) continue;
                dis.readUTF();
                @SuppressWarnings("unchecked")
                Map<String, Object> chunk = (Map<String, Object>) read(dis, 10);
                scanChunk(dimension, chunk);
            } catch (Exception e) {
                // skip broken chunk
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void scanChunk(String dim, Map<String, Object> chunk) {
        Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
        Object dv = chunk.get("DataVersion");
        if (dv instanceof Integer v) versions.merge(v, 1, Integer::sum);
        int cx = root.get("xPos") instanceof Integer x ? x : 0;
        int cz = root.get("zPos") instanceof Integer z ? z : 0;
        Set<String> seenInChunk = new HashSet<>();
        List<Object> sections = (List<Object>) root.getOrDefault("sections", List.of());
        for (Object o : sections) {
            Map<String, Object> s = (Map<String, Object>) o;
            Map<String, Object> states = (Map<String, Object>) s.get("block_states");
            if (states == null) continue;
            List<Object> palette = (List<Object>) states.get("palette");
            if (palette == null) continue;
            int sy = s.get("Y") instanceof Byte b ? b : 0;
            long[] counts = new long[palette.size()];
            long[] packed = (long[]) states.get("data");
            if (packed == null || palette.size() == 1) {
                counts[0] = 4096;
            } else {
                int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                int perLong = 64 / bits;
                long mask = (1L << bits) - 1;
                for (int idx = 0; idx < 4096; idx++) {
                    long word = packed[idx / perLong];
                    int v = (int) ((word >>> ((idx % perLong) * bits)) & mask);
                    if (v < counts.length) counts[v]++;
                }
            }
            for (int p = 0; p < palette.size(); p++) {
                if (counts[p] == 0) continue;
                String name = (String) ((Map<String, Object>) palette.get(p)).get("Name");
                if (name == null || ignored(name)) continue;
                long[] c = blocks.computeIfAbsent(name, k -> new long[2]);
                c[0] += counts[p];
                if (seenInChunk.add(name)) c[1]++;
                example.putIfAbsent(name, dim + " " + (cx * 16) + " " + (sy * 16) + " " + (cz * 16));
            }
        }
        List<Object> bes = (List<Object>) root.getOrDefault("block_entities", root.getOrDefault("TileEntities", List.of()));
        for (Object o : bes) {
            Map<String, Object> be = (Map<String, Object>) o;
            String id = (String) be.get("id");
            if (id == null || ignored(id)) continue;
            blockEntities.computeIfAbsent(id, k -> new long[1])[0]++;
            example.putIfAbsent("be:" + id, dim + " " + be.get("x") + " " + be.get("y") + " " + be.get("z"));
        }
    }

    static boolean ignored(String id) {
        int colon = id.indexOf(':');
        return ignore.contains(colon < 0 ? "minecraft" : id.substring(0, colon));
    }

    static Object read(DataInputStream in, int type) throws IOException {
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: { byte[] b = new byte[in.readInt()]; in.readFully(b); return b; }
            case 8: return in.readUTF();
            case 9: {
                int t = in.readByte(); int n = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, n));
                for (int i = 0; i < n; i++) list.add(read(in, t));
                return list;
            }
            case 10: {
                Map<String, Object> map = new HashMap<>();
                while (true) {
                    int t = in.readByte();
                    if (t == 0) return map;
                    String key = in.readUTF();
                    map.put(key, read(in, t));
                }
            }
            case 11: { int[] a = new int[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readInt(); return a; }
            case 12: { long[] a = new long[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readLong(); return a; }
            default: throw new IOException("bad tag " + type);
        }
    }
}
