import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * A top-down picture of a dimension from its region files: one pixel per block (or per `scale` blocks), the colour of
 * the top block, shaded by height against the neighbour to the north, water tinted by depth. Grid lines every 100 blocks
 * with their coordinates written into a side file, so a spot on the picture can be turned back into x/z.
 * Usage: java MapImage <region dir> <out.png> [x0 z0 x1 z1] [scale] [zoom: pixels per cell]
 */
public class MapImage {
    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        Path out = Path.of(a[1]);
        int x0, z0, x1, z1;
        if (a.length >= 6) {
            x0 = Integer.parseInt(a[2]); z0 = Integer.parseInt(a[3]); x1 = Integer.parseInt(a[4]); z1 = Integer.parseInt(a[5]);
        } else {
            int[] b = extent(dir);
            x0 = b[0]; z0 = b[1]; x1 = b[2]; z1 = b[3];
        }
        int scale = a.length >= 7 ? Integer.parseInt(a[6]) : 1;
        int w = (x1 - x0) / scale + 1, h = (z1 - z0) / scale + 1;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[][] height = new int[h][w];
        String[][] name = new String[h][w];
        int[][] water = new int[h][w];
        for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
            for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) {
                Top[] tops = tops(dir, cx, cz);
                Surface.cache.clear();
                if (tops == null) continue;
                for (int lz = 0; lz < 16; lz++) for (int lx = 0; lx < 16; lx++) {
                    int x = cx * 16 + lx, z = cz * 16 + lz;
                    if (x < x0 || x > x1 || z < z0 || z > z1 || Math.floorMod(x - x0, scale) != 0 || Math.floorMod(z - z0, scale) != 0) continue;
                    Top t = tops[lz * 16 + lx];
                    int px = (x - x0) / scale, pz = (z - z0) / scale;
                    height[pz][px] = t == null ? Integer.MIN_VALUE : t.y;
                    name[pz][px] = t == null ? null : t.name;
                    water[pz][px] = t == null ? 0 : t.water;
                }
            }
        }
        for (int pz = 0; pz < h; pz++) for (int px = 0; px < w; px++) {
            if (name[pz][px] == null) { img.setRGB(px, pz, 0x101010); continue; }
            int c = color(name[pz][px]);
            if (water[pz][px] > 0) {
                c = mix(c, 0x3060c0, Math.min(0.9, 0.45 + water[pz][px] * 0.04));
            } else {
                int north = pz > 0 && name[pz - 1][px] != null ? height[pz - 1][px] : height[pz][px];
                int d = height[pz][px] - north;
                c = shade(c, d > 0 ? 1.15 : d < 0 ? 0.82 : 1.0);
            }
            int x = x0 + px * scale, z = z0 + pz * scale;
            if (Math.floorMod(x, 100) < scale || Math.floorMod(z, 100) < scale) c = mix(c, Math.floorMod(x, 500) < scale || Math.floorMod(z, 500) < scale ? 0xff0000 : 0xffffff, 0.35);
            img.setRGB(px, pz, c);
        }
        int zoom = a.length >= 8 ? Integer.parseInt(a[7]) : 1;
        if (zoom > 1) {
            BufferedImage big = new BufferedImage(w * zoom, h * zoom, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h * zoom; y++) for (int x = 0; x < w * zoom; x++) big.setRGB(x, y, img.getRGB(x / zoom, y / zoom));
            img = big;
        }
        ImageIO.write(img, "png", out.toFile());
        System.out.println("map " + w + "x" + h + " px, x " + x0 + ".." + x1 + ", z " + z0 + ".." + z1 + ", " + scale
                + " block(s) per px; px = (x - " + x0 + ") / " + scale + ", py = (z - " + z0 + ") / " + scale
                + "; grid every 100 blocks, red every 500");
    }

    record Top(String name, int y, int water) {}

    /** The extent of the chunks present in the region files. */
    static int[] extent(Path dir) throws IOException {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        try (var files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                String[] p = f.getFileName().toString().split("\\.");
                if (p.length != 4 || Files.size(f) < 8192) continue;
                int rx = Integer.parseInt(p[1]), rz = Integer.parseInt(p[2]);
                byte[] head = new byte[4096];
                try (InputStream in = Files.newInputStream(f)) { in.readNBytes(head, 0, 4096); }
                for (int i = 0; i < 1024; i++) {
                    if (head[i * 4] == 0 && head[i * 4 + 1] == 0 && head[i * 4 + 2] == 0) continue;
                    int cx = rx * 32 + (i % 32), cz = rz * 32 + (i / 32);
                    minX = Math.min(minX, cx * 16); maxX = Math.max(maxX, cx * 16 + 15);
                    minZ = Math.min(minZ, cz * 16); maxZ = Math.max(maxZ, cz * 16 + 15);
                }
            }
        }
        return new int[] {minX, minZ, maxX, maxZ};
    }

    /** The top block of each column of a chunk (index lz*16+lx), with the depth of water over it; null if no chunk. */
    @SuppressWarnings("unchecked")
    static Top[] tops(Path dir, int cx, int cz) throws IOException {
        Map<String, Object> chunk = Surface.chunk(dir, cx, cz);
        if (chunk == null) return null;
        Map<String, Object> root = chunk.containsKey("Level") ? (Map<String, Object>) chunk.get("Level") : chunk;
        List<Object> sections = new ArrayList<>((List<Object>) root.getOrDefault("sections", List.of()));
        sections.sort((p, q) -> Byte.compare((Byte) ((Map<String, Object>) q).get("Y"), (Byte) ((Map<String, Object>) p).get("Y")));
        Top[] tops = new Top[256];
        int[] water = new int[256];
        int left = 256;
        for (Object o : sections) {
            Map<String, Object> s = (Map<String, Object>) o;
            Map<String, Object> states = (Map<String, Object>) s.get("block_states");
            if (states == null) continue;
            List<Object> palette = (List<Object>) states.get("palette");
            long[] data = (long[]) states.get("data");
            int sy = (Byte) s.get("Y");
            int bits = palette.size() <= 1 ? 0 : Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            for (int ly = 15; ly >= 0 && left > 0; ly--) {
                for (int i = 0; i < 256; i++) {
                    if (tops[i] != null) continue;
                    int idx = ly * 256 + i;
                    int v = 0;
                    if (bits > 0) {
                        int perLong = 64 / bits;
                        v = (int) ((data[idx / perLong] >>> ((idx % perLong) * bits)) & ((1L << bits) - 1));
                    }
                    String n = (String) ((Map<String, Object>) palette.get(v)).get("Name");
                    if (n.endsWith(":air") || n.endsWith("cave_air") || n.endsWith("void_air") || n.endsWith("barrier")) continue;
                    if (n.endsWith(":water") || n.endsWith("seagrass") || n.endsWith("kelp") || n.endsWith("kelp_plant")) {
                        water[i]++;
                        continue;
                    }
                    tops[i] = new Top(n, sy * 16 + ly, water[i]);
                    left--;
                }
            }
        }
        return tops;
    }

    static final Map<String, Integer> COLORS = new LinkedHashMap<>();
    static {
        String[][] table = {
                {"grass_block", "5c9a3a"}, {"short_grass", "62a33e"}, {"tall_grass", "62a33e"}, {"fern", "4f8a35"},
                {"lime_terracotta", "6f8a36"}, {"green_concrete_powder", "4f8f2f"}, {"moss", "5a8a2e"},
                {"leaves", "2f6a22"}, {"_log", "6b5130"}, {"_wood", "6b5130"},
                {"dirt_path", "a08a55"}, {"coarse_dirt", "80603f"}, {"rooted_dirt", "8a6748"}, {"podzol", "6e4f2c"},
                {"dirt", "86603d"}, {"farmland", "5e3f22"}, {"wheat", "c9b44b"},
                {"sand", "dbd09a"}, {"gravel", "8c8682"}, {"clay", "a0a6b2"},
                {"stone_brick", "7c7c7c"}, {"smooth_stone", "a2a2a2"}, {"andesite", "8b8b88"}, {"cobblestone", "6f6f6f"},
                {"granite", "a06c58"}, {"diorite", "cfcfcf"}, {"deepslate", "4a4a50"}, {"stone", "7d7d7d"}, {"tuff", "6c6d66"},
                {"concrete", "999999"}, {"brick", "9a4f3e"}, {"terracotta", "985f45"}, {"planks", "a2824e"},
                {"glass", "c4e4f0"}, {"iron", "c8c8c8"}, {"asphalt", "38383c"}, {"black", "2a2a2e"}, {"snow", "f4f8f8"},
                {"lava", "e06010"}, {"water", "3060c0"}, {"lily_pad", "2f7a2a"}};
        for (String[] r : table) COLORS.put(r[0], Integer.parseInt(r[1], 16));
    }

    static int color(String name) {
        String n = name.substring(name.indexOf(':') + 1);
        for (var e : COLORS.entrySet()) if (n.contains(e.getKey())) return e.getValue();
        if (n.contains("white")) return 0xe0e0e0;
        if (n.contains("gray") || n.contains("grey")) return 0x707070;
        if (n.contains("red")) return 0xa03030;
        if (n.contains("yellow")) return 0xd0b030;
        if (n.contains("orange")) return 0xd07020;
        if (n.contains("blue")) return 0x3050a0;
        if (n.contains("green")) return 0x408030;
        if (n.contains("brown")) return 0x6b4a2a;
        int hsh = name.hashCode();
        return 0x606060 | (hsh & 0x3f3f3f);
    }

    static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static int shade(int c, double f) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * f));
        int g = Math.min(255, (int) (((c >> 8) & 255) * f));
        int b = Math.min(255, (int) ((c & 255) * f));
        return (r << 16) | (g << 8) | b;
    }
}
