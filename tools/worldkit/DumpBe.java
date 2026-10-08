import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Prints the block entity at x y z of a dimension's region dir. Usage: java DumpBe <region dir> <x> <y> <z> */
public class DumpBe {
    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]), z = Integer.parseInt(a[3]);
        Map<String, Object> chunk = Surface.chunk(Path.of(a[0]), Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
        for (Object o : (List<Object>) root.getOrDefault("block_entities", List.of())) {
            Map<String, Object> be = (Map<String, Object>) o;
            if (Objects.equals(be.get("x"), x) && Objects.equals(be.get("y"), y) && Objects.equals(be.get("z"), z)) {
                System.out.println("DataVersion=" + chunk.get("DataVersion") + " " + show(be));
            }
        }
    }

    @SuppressWarnings("unchecked")
    static String show(Object v) {
        if (v instanceof Map<?, ?> m) {
            StringBuilder b = new StringBuilder("{");
            new TreeMap<>((Map<String, Object>) m).forEach((k, val) -> b.append(k).append(':').append(show(val)).append(", "));
            return b.append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder b = new StringBuilder("[");
            l.forEach(e -> b.append(show(e)).append(", "));
            return b.append(']').toString();
        }
        if (v instanceof String s) return '"' + s + '"';
        if (v instanceof int[] arr) return Arrays.toString(arr);
        return String.valueOf(v) + (v instanceof Byte ? "b" : "");
    }
}
