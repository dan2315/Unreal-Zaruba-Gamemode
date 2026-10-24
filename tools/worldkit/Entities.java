import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Entities saved in a dimension's entities/ region files (offline; the map must not be open in the server).
 *   list <entities dir>                     counts by entity id
 *   strip <entities dir> <out dir> id,id..  writes the region files that held any of those ids to out dir (same names),
 *                                           without them; "hostile" in the list stands for the usual hostile mobs.
 * Dropped items, xp orbs and mobs that spawned while a map was open for editing get saved with it and load into every
 * match; this is how to take them out. Copy the written files over the originals with the server stopped or the map
 * closed.
 * Usage: java Entities list <dir> | java Entities strip <dir> <out> <ids>
 */
public class Entities {
    static final List<String> HOSTILE = List.of("zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman", "witch", "slime",
            "drowned", "husk", "stray", "phantom", "zombie_villager", "pillager", "vindicator", "evoker", "ravager", "silverfish",
            "endermite", "bogged", "breeze", "magma_cube", "blaze", "ghast", "wither_skeleton", "piglin", "piglin_brute", "hoglin", "zoglin");

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[1]);
        Set<String> strip = new HashSet<>();
        Path out = null;
        if (a[0].equals("strip")) {
            out = Path.of(a[2]);
            Files.createDirectories(out);
            for (String id : a[3].split(",")) {
                if (id.equals("hostile")) HOSTILE.forEach(h -> strip.add("minecraft:" + h));
                else strip.add(id.contains(":") ? id : "minecraft:" + id);
            }
        }
        Map<String, Integer> counts = new TreeMap<>(), removed = new TreeMap<>();
        int files = 0;
        try (var ls = Files.list(dir)) {
            for (Path f : (Iterable<Path>) ls::iterator) {
                if (!f.toString().endsWith(".mca")) continue;
                if (rewrite(f, out == null ? null : out.resolve(f.getFileName()), strip, counts, removed)) files++;
            }
        }
        System.out.println("ENTITIES:");
        counts.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).forEach(e -> System.out.println("  " + e.getValue() + "  " + e.getKey()));
        if (out != null) {
            System.out.println("REMOVED (" + files + " region files written to " + out + "):");
            removed.forEach((k, v) -> System.out.println("  " + v + "  " + k));
        }
    }

    /** Reads one region file; with a destination, writes it without the stripped entities if any were found. */
    static boolean rewrite(Path src, Path dst, Set<String> strip, Map<String, Integer> counts, Map<String, Integer> removed) throws IOException {
        byte[] data = Files.readAllBytes(src);
        if (data.length < 8192) return false;
        byte[][] chunks = new byte[1024][];
        int changed = 0;
        for (int i = 0; i < 1024; i++) {
            int loc = ((data[i * 4] & 0xFF) << 16) | ((data[i * 4 + 1] & 0xFF) << 8) | (data[i * 4 + 2] & 0xFF);
            if (loc == 0) continue;
            int off = loc * 4096;
            if (off + 5 > data.length) continue;
            int len = ((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16) | ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
            if (len <= 1 || off + 4 + len > data.length) continue;
            int type = data[off + 4];
            if ((type & 0x80) != 0) throw new IOException("external chunk in " + src);
            chunks[i] = Arrays.copyOfRange(data, off, off + 4 + len);
            InputStream raw = new ByteArrayInputStream(data, off + 5, len - 1);
            InputStream in = type == 1 ? new GZIPInputStream(raw) : type == 2 ? new InflaterInputStream(raw) : raw;
            DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
            byte rootType = dis.readByte();
            String rootName = dis.readUTF();
            Migrate.Tag root = Migrate.read(dis, rootType);
            Migrate.Tag list = root.map().get("Entities");
            if (list == null) continue;
            int before = list.list().size();
            list.list().removeIf(e -> {
                Migrate.Tag id = e.map().get("id");
                String name = id == null ? "?" : id.str();
                counts.merge(name, 1, Integer::sum);
                boolean go = strip.contains(name);
                if (go) removed.merge(name, 1, Integer::sum);
                return go;
            });
            if (dst == null || list.list().size() == before) continue;
            ByteArrayOutputStream nbt = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(nbt))) {
                dos.writeByte(rootType); dos.writeUTF(rootName); Migrate.write(dos, root);
            }
            byte[] payload = nbt.toByteArray();
            ByteArrayOutputStream c = new ByteArrayOutputStream();
            DataOutputStream cd = new DataOutputStream(c);
            cd.writeInt(payload.length + 1); cd.writeByte(2); cd.write(payload);
            chunks[i] = c.toByteArray();
            changed++;
        }
        if (dst == null || changed == 0) return false;
        byte[] header = new byte[8192];
        System.arraycopy(data, 4096, header, 4096, 4096); // timestamps
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int sector = 2;
        for (int i = 0; i < 1024; i++) {
            if (chunks[i] == null) continue;
            int sectors = (chunks[i].length + 4095) / 4096;
            if (sectors > 255) throw new IOException("chunk too large in " + src);
            body.write(Arrays.copyOf(chunks[i], sectors * 4096));
            header[i * 4] = (byte) (sector >> 16); header[i * 4 + 1] = (byte) (sector >> 8); header[i * 4 + 2] = (byte) sector; header[i * 4 + 3] = (byte) sectors;
            sector += sectors;
        }
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(header); file.write(body.toByteArray());
        Files.write(dst, file.toByteArray());
        return true;
    }
}
