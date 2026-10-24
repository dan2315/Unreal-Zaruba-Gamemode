import java.nio.file.*;

/**
 * Positions of blocks whose state contains a text, in a box of the map (y range too). Prints up to `limit` hits.
 * Usage: java Find <region dir> <text> x0 y0 z0 x1 y1 z1 [limit]
 */
public class Find {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        String text = a[1];
        int x0 = Integer.parseInt(a[2]), y0 = Integer.parseInt(a[3]), z0 = Integer.parseInt(a[4]);
        int x1 = Integer.parseInt(a[5]), y1 = Integer.parseInt(a[6]), z1 = Integer.parseInt(a[7]);
        int limit = a.length > 8 ? Integer.parseInt(a[8]) : 200;
        int n = 0;
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    String s = r.state(x, y, z);
                    if (s.contains(text)) {
                        if (n++ < limit) System.out.println(x + " " + y + " " + z + "  " + s);
                    }
                }
        System.out.println(n + " found");
    }
}
