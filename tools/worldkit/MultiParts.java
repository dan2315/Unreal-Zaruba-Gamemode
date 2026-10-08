import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * For copycats:multistate_copycat block entities: block id + state properties -> material_data part names.
 * Also old create:copycat entries on multistate blocks: block id + properties. Usage: java MultiParts <region dir>
 */
public class MultiParts {
    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        Map<String, Integer> working = new TreeMap<>(), old = new TreeMap<>();
        Set<String> multi = Set.of("copycats:copycat_slab", "copycats:copycat_byte", "copycats:copycat_board", "copycats:copycat_half_layer");
        try (var files = Files.list(dir)) {
            for (Path mca : files.filter(p -> p.toString().endsWith(".mca")).toList()) {
                String[] p = mca.getFileName().toString().split("\\.");
                int rx = Integer.parseInt(p[1]), rz = Integer.parseInt(p[2]);
                for (int i = 0; i < 1024; i++) {
                    Map<String, Object> chunk;
                    try { chunk = Surface.chunk(dir, rx * 32 + i % 32, rz * 32 + i / 32); } catch (Exception e) { continue; }
                    if (chunk == null) continue;
                    Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
                    List<Object> sections = (List<Object>) root.getOrDefault("sections", List.of());
                    for (Object o : (List<Object>) root.getOrDefault("block_entities", List.of())) {
                        Map<String, Object> be = (Map<String, Object>) o;
                        Map<String, Object> state = stateAt(sections, (Integer) be.get("x"), (Integer) be.get("y"), (Integer) be.get("z"));
                        if (state == null) continue;
                        String block = (String) state.get("Name");
                        if (!multi.contains(block)) continue;
                        String props = String.valueOf(state.get("Properties"));
                        if ("copycats:multistate_copycat".equals(be.get("id"))) {
                            Map<String, Object> md = (Map<String, Object>) be.get("material_data");
                            List<String> parts = new ArrayList<>();
                            if (md != null) md.forEach((k, v) -> {
                                Map<String, Object> mat = (Map<String, Object>) ((Map<String, Object>) v).get("material");
                                parts.add(k + "=" + (mat == null ? "?" : ((String) mat.get("Name")).replace("minecraft:", "")));
                            });
                            Collections.sort(parts);
                            working.merge(block + " " + props + " -> " + parts, 1, Integer::sum);
                        } else if ("create:copycat".equals(be.get("id"))) {
                            old.merge(block + " " + props, 1, Integer::sum);
                        }
                    }
                    Surface.cache.clear();
                }
            }
        }
        System.out.println("WORKING (sample):");
        working.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(40).forEach(e -> System.out.println("  " + e.getValue() + "  " + e.getKey()));
        System.out.println("OLD create:copycat on multistate blocks:");
        old.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).forEach(e -> System.out.println("  " + e.getValue() + "  " + e.getKey()));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> stateAt(List<Object> sections, int x, int y, int z) {
        int sy = Math.floorDiv(y, 16);
        for (Object o : sections) {
            Map<String, Object> s = (Map<String, Object>) o;
            if (!(s.get("Y") instanceof Byte b) || b != sy) continue;
            Map<String, Object> states = (Map<String, Object>) s.get("block_states");
            if (states == null) return null;
            List<Object> palette = (List<Object>) states.get("palette");
            long[] data = (long[]) states.get("data");
            int idx = (Math.floorMod(y, 16) * 16 + Math.floorMod(z, 16)) * 16 + Math.floorMod(x, 16);
            int v = 0;
            if (palette.size() > 1) {
                int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                int perLong = 64 / bits;
                v = (int) ((data[idx / perLong] >>> ((idx % perLong) * bits)) & ((1L << bits) - 1));
            }
            return (Map<String, Object>) palette.get(v);
        }
        return null;
    }
}
