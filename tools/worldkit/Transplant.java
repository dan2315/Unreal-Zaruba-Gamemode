import java.io.*;
import java.nio.file.*;

/**
 * Copies the chunks in a rectangle (absolute chunk coords, inclusive) from one region file into another, keeping
 * every other chunk of the target. The target is rewritten with its sectors packed; a missing source chunk
 * removes that chunk from the target.
 * Usage: java Transplant <source.mca> <target.mca> <cx0> <cz0> <cx1> <cz1>
 */
public class Transplant {
    public static void main(String[] a) throws IOException {
        Path src = Path.of(a[0]), dst = Path.of(a[1]);
        int cx0 = Integer.parseInt(a[2]), cz0 = Integer.parseInt(a[3]), cx1 = Integer.parseInt(a[4]), cz1 = Integer.parseInt(a[5]);
        byte[] s = Files.readAllBytes(src);
        byte[] d = Files.exists(dst) ? Files.readAllBytes(dst) : new byte[8192];
        String name = dst.getFileName().toString(); // r.<rx>.<rz>.mca
        String[] parts = name.split("\\.");
        int rx = Integer.parseInt(parts[1]), rz = Integer.parseInt(parts[2]);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] header = new byte[8192];
        int sector = 2, copied = 0, kept = 0;
        for (int i = 0; i < 1024; i++) {
            int cx = rx * 32 + (i % 32), cz = rz * 32 + (i / 32);
            boolean take = cx >= cx0 && cx <= cx1 && cz >= cz0 && cz <= cz1;
            byte[] from = take ? s : d;
            byte[] chunk = chunkBytes(from, i);
            if (chunk == null) continue;
            if ((chunk[4] & 0x80) != 0) throw new IOException("chunk " + cx + "," + cz + " is stored externally (.mcc); not supported");
            int sectors = (chunk.length + 4095) / 4096;
            byte[] padded = new byte[sectors * 4096];
            System.arraycopy(chunk, 0, padded, 0, chunk.length);
            body.write(padded);
            header[i * 4] = (byte) (sector >> 16);
            header[i * 4 + 1] = (byte) (sector >> 8);
            header[i * 4 + 2] = (byte) sector;
            header[i * 4 + 3] = (byte) sectors;
            System.arraycopy(from, 4096 + i * 4, header, 4096 + i * 4, 4); // timestamp
            sector += sectors;
            if (take) copied++; else kept++;
        }
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(header);
        file.write(body.toByteArray());
        Files.write(dst, file.toByteArray());
        System.out.println(dst + ": copied " + copied + " chunks, kept " + kept);
    }

    /** Length-prefixed chunk data (4-byte length, compression byte, payload), or null if absent. */
    static byte[] chunkBytes(byte[] region, int i) {
        if (region.length < 8192) return null;
        int loc = ((region[i * 4] & 0xFF) << 16) | ((region[i * 4 + 1] & 0xFF) << 8) | (region[i * 4 + 2] & 0xFF);
        if (loc == 0) return null;
        int off = loc * 4096;
        if (off + 5 > region.length) return null;
        int len = ((region[off] & 0xFF) << 24) | ((region[off + 1] & 0xFF) << 16) | ((region[off + 2] & 0xFF) << 8) | (region[off + 3] & 0xFF);
        if (len <= 0 || off + 4 + len > region.length) return null;
        byte[] out = new byte[4 + len];
        System.arraycopy(region, off, out, 0, 4 + len);
        return out;
    }
}
