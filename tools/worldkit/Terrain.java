import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Reads MOTION_BLOCKING and OCEAN_FLOOR heightmaps from a dimension's region files and rates sites on a circle.
 * Usage: java Terrain.java <region dir> <cx> <cz> <radius> <siteHalfSize>
 */
public class Terrain {
    static Map<Long, int[][]> chunks = new HashMap<>(); // key -> {motionBlocking[256], oceanFloor[256]}
    static int minY = -64;

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        int cx = Integer.parseInt(a[1]), cz = Integer.parseInt(a[2]), radius = Integer.parseInt(a[3]), half = Integer.parseInt(a[4]);
        try (var files = Files.list(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".mca")).toList()) readRegion(p);
        }
        int minCx = Integer.MAX_VALUE, maxCx = Integer.MIN_VALUE, minCz = Integer.MAX_VALUE, maxCz = Integer.MIN_VALUE;
        for (long k : chunks.keySet()) { int x = (int) (k >> 32), z = (int) k; minCx = Math.min(minCx, x); maxCx = Math.max(maxCx, x); minCz = Math.min(minCz, z); maxCz = Math.max(maxCz, z); }
        System.out.printf("chunks=%d  blocks x %d..%d  z %d..%d%n", chunks.size(), minCx * 16, maxCx * 16 + 15, minCz * 16, maxCz * 16 + 15);
        System.out.println("here: ground=" + ground(cx, cz) + " water=" + water(cx, cz));
        System.out.println("angle  x      z     ground  min  max  water%  missing%   (0deg = +x/east, 90deg = +z/south)");
        for (int deg = 0; deg < 360; deg += 15) {
            double r = Math.toRadians(deg);
            int x = cx + (int) Math.round(Math.cos(r) * radius), z = cz + (int) Math.round(Math.sin(r) * radius);
            rate(deg, x, z, half);
        }
        if (a.length > 5) { // extra: rate a specific spot
            rate(-1, Integer.parseInt(a[5]), Integer.parseInt(a[6]), half);
        }
    }

    static void rate(int deg, int x, int z, int half) {
        int n = 0, missing = 0, wet = 0, min = 9999, max = -9999; long sum = 0;
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            Integer g = ground(x + dx, z + dz);
            if (g == null) { missing++; continue; }
            n++; sum += g; min = Math.min(min, g); max = Math.max(max, g);
            if (water(x + dx, z + dz) > 0) wet++;
        }
        int total = (2 * half + 1) * (2 * half + 1);
        System.out.printf("%4d  %6d %6d  %6s %4s %4s  %5.1f  %6.1f%n", deg, x, z, n > 0 ? String.valueOf(sum / n) : "-",
                n > 0 ? min : "-", n > 0 ? max : "-", 100.0 * wet / Math.max(1, n), 100.0 * missing / total);
    }

    /** Height of the top non-fluid surface (ocean floor heightmap: first block above the floor). */
    static Integer ground(int x, int z) {
        int[][] c = chunks.get(key(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        if (c == null || c[1] == null) return null;
        return c[1][Math.floorMod(z, 16) * 16 + Math.floorMod(x, 16)] + minY;
    }

    static int water(int x, int z) {
        int[][] c = chunks.get(key(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        if (c == null || c[0] == null || c[1] == null) return 0;
        int i = Math.floorMod(z, 16) * 16 + Math.floorMod(x, 16);
        return c[0][i] - c[1][i];
    }

    static long key(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }

    @SuppressWarnings("unchecked")
    static void readRegion(Path mca) throws IOException {
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
                if (!"minecraft:full".equals(root.get("Status")) && !"full".equals(root.get("Status"))) continue;
                Map<String, Object> hm = (Map<String, Object>) root.get("Heightmaps");
                if (hm == null) continue;
                int x = (Integer) root.get("xPos"), z = (Integer) root.get("zPos");
                chunks.put(key(x, z), new int[][] {unpack((long[]) hm.get("MOTION_BLOCKING")), unpack((long[]) hm.get("OCEAN_FLOOR"))});
            } catch (Exception e) {
                // skip
            }
        }
    }

    static int[] unpack(long[] longs) {
        if (longs == null) return null;
        int bits = 9, perLong = 64 / bits;
        int[] out = new int[256];
        for (int i = 0; i < 256; i++) out[i] = (int) ((longs[i / perLong] >>> ((i % perLong) * bits)) & 0x1FF);
        return out;
    }
}
