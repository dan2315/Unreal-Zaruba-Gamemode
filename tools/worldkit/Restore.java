import java.nio.file.*;
import java.util.*;

/**
 * Commands that put a box back to how it is in a backup region dir: air over the box first, then the original
 * blocks, merged into runs along x. Two-block plants get their half. Block properties other than that are not
 * restored (fine for terrain: grass, dirt, stone, flowers).
 * Usage: java Restore <backup region dir> <x0> <y0> <z0> <x1> <y1> <z1>
 */
public class Restore {
    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        int x0 = Integer.parseInt(a[1]), y0 = Integer.parseInt(a[2]), z0 = Integer.parseInt(a[3]);
        int x1 = Integer.parseInt(a[4]), y1 = Integer.parseInt(a[5]), z1 = Integer.parseInt(a[6]);
        List<String> out = new ArrayList<>();
        // clear in slabs that stay under the 32768-block fill limit
        int area = (x1 - x0 + 1) * (z1 - z0 + 1);
        int layers = Math.max(1, 32768 / area);
        for (int y = y0; y <= y1; y += layers) {
            out.add("fill " + x0 + " " + y + " " + z0 + " " + x1 + " " + Math.min(y1, y + layers - 1) + " " + z1 + " minecraft:air");
        }
        Map<Long, TreeMap<Integer, String>> cols = new HashMap<>();
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                String runBlock = null;
                int runStart = 0;
                for (int x = x0; x <= x1 + 1; x++) {
                    String b = null;
                    if (x <= x1) {
                        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                        TreeMap<Integer, String> col = cols.computeIfAbsent(key, k -> {
                            try { return Surface.column(dir, (int) (k >> 32), (int) (long) k); } catch (Exception e) { throw new RuntimeException(e); }
                        });
                        b = col.get(y);
                        if (b != null && (b.endsWith("tall_grass") || b.endsWith("large_fern") || b.endsWith("lilac") || b.endsWith("rose_bush") || b.endsWith("peony") || b.endsWith("sunflower"))) {
                            b = b + (b.equals(col.get(y - 1)) ? "[half=upper]" : "[half=lower]");
                        }
                    }
                    if (!Objects.equals(b, runBlock)) {
                        if (runBlock != null) {
                            out.add(runStart == x - 1
                                    ? "setblock " + runStart + " " + y + " " + z + " " + runBlock
                                    : "fill " + runStart + " " + y + " " + z + " " + (x - 1) + " " + y + " " + z + " " + runBlock);
                        }
                        runBlock = b;
                        runStart = x;
                    }
                }
            }
        }
        out.forEach(c -> System.out.println("execute in unrealzaruba:game_dim run " + c));
    }
}
