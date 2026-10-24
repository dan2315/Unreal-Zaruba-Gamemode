import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/**
 * Where the map is empty: each column is classed as open natural ground, tree (trunk or canopy), built, road (road-like
 * tops in 8 of the 5x5 around) or water; the distance from open ground to the nearest non-open column shows how bare a
 * spot is. Writes a PNG (dark = busy, bright green = bare; trees dark green, builds grey, roads brown, water blue, grid
 * every 100) and lists the largest bare disks found greedily (centre, radius, ground height), biggest first.
 * Usage: java Empty <region> x0 z0 x1 z1 <out.png> [minRadius] [maxDisks]
 */
public class Empty {
    public static void main(String[] a) throws Exception {
        Region r = new Region(Path.of(a[0]));
        int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), z1 = Integer.parseInt(a[4]);
        int minR = a.length > 6 ? Integer.parseInt(a[6]) : 25, maxDisks = a.length > 7 ? Integer.parseInt(a[7]) : 60;
        int w = x1 - x0 + 1, d = z1 - z0 + 1;
        byte[][] k = new byte[w][d]; // 0 open, 1 tree, 2 built, 3 water, 4 road
        int[][] g = new int[w][d];
        boolean[][] roadlike = new boolean[w][d];
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            int x = x0 + i, z = z0 + j;
            byte c = 0;
            int gy = Integer.MIN_VALUE;
            for (int y = 100; y >= 0; y--) {
                String s = r.state(x, y, z);
                if (Region.isAir(s)) continue;
                String n = Region.name(s).replace("minecraft:", "");
                if (n.endsWith("leaves") || n.endsWith("_log") || n.endsWith("_wood") && !n.startsWith("stripped")) { c = 1; continue; }
                if (BorderHills.plant(n)) continue;
                if (n.equals("water")) { c = 3; gy = y; break; }
                if (!Export.natural(s)) { if (c == 0) c = 2; gy = y; break; }
                gy = y;
                roadlike[i][j] = Forest.ROADLIKE.contains(n);
                break;
            }
            k[i][j] = c;
            g[i][j] = gy;
        }
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            if (k[i][j] != 0) continue;
            int count = 0;
            for (int di = -2; di <= 2; di++) for (int dj = -2; dj <= 2; dj++) {
                int p = i + di, q = j + dj;
                if (p >= 0 && q >= 0 && p < w && q < d && roadlike[p][q]) count++;
            }
            if (count >= 8) k[i][j] = 4;
        }
        // chamfer distance to anything not open
        double[][] dist = new double[w][d];
        double s2 = Math.sqrt(2), cap = 200;
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) dist[i][j] = k[i][j] == 0 ? cap : 0;
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) {
            if (i > 0) dist[i][j] = Math.min(dist[i][j], dist[i - 1][j] + 1);
            if (j > 0) dist[i][j] = Math.min(dist[i][j], dist[i][j - 1] + 1);
            if (i > 0 && j > 0) dist[i][j] = Math.min(dist[i][j], dist[i - 1][j - 1] + s2);
            if (i > 0 && j < d - 1) dist[i][j] = Math.min(dist[i][j], dist[i - 1][j + 1] + s2);
        }
        for (int i = w - 1; i >= 0; i--) for (int j = d - 1; j >= 0; j--) {
            if (i < w - 1) dist[i][j] = Math.min(dist[i][j], dist[i + 1][j] + 1);
            if (j < d - 1) dist[i][j] = Math.min(dist[i][j], dist[i][j + 1] + 1);
            if (i < w - 1 && j < d - 1) dist[i][j] = Math.min(dist[i][j], dist[i + 1][j + 1] + s2);
            if (i < w - 1 && j > 0) dist[i][j] = Math.min(dist[i][j], dist[i + 1][j - 1] + s2);
        }
        int scale = Math.max(1, Math.max(w, d) / 1024);
        BufferedImage img = new BufferedImage(w / scale, d / scale, BufferedImage.TYPE_INT_RGB);
        for (int px = 0; px < w / scale; px++) for (int py = 0; py < d / scale; py++) {
            int i = px * scale, j = py * scale, rgb;
            switch (k[i][j]) {
                case 1 -> rgb = 0x1e5a1e;
                case 2 -> rgb = 0x808080;
                case 3 -> rgb = 0x3050c0;
                case 4 -> rgb = 0x8a5a30;
                default -> { int v = (int) Math.min(255, 40 + dist[i][j] * 3); rgb = (v / 3) << 16 | v << 8 | (v / 3); }
            }
            if (Math.floorMod(x0 + i, 100) < scale || Math.floorMod(z0 + j, 100) < scale) rgb = (rgb >> 1) & 0x7f7f7f;
            img.setRGB(px, py, rgb);
        }
        ImageIO_write(img, a[5]);
        // greedy largest disks
        double[][] left = new double[w][d];
        for (int i = 0; i < w; i++) left[i] = dist[i].clone();
        System.out.println("bare disks (x z radius groundY):");
        for (int n = 0; n < maxDisks; n++) {
            int bi = -1, bj = -1;
            double best = 0;
            for (int i = 0; i < w; i += 2) for (int j = 0; j < d; j += 2) if (left[i][j] > best) { best = left[i][j]; bi = i; bj = j; }
            if (best < minR) break;
            System.out.printf("%d %d %d %d%n", x0 + bi, z0 + bj, (int) best, g[bi][bj]);
            int rr = (int) best;
            for (int i = Math.max(0, bi - 2 * rr); i < Math.min(w, bi + 2 * rr); i++)
                for (int j = Math.max(0, bj - 2 * rr); j < Math.min(d, bj + 2 * rr); j++)
                    if (Math.hypot(i - bi, j - bj) < rr * 1.6) left[i][j] = 0;
        }
    }

    static void ImageIO_write(BufferedImage img, String out) throws Exception {
        javax.imageio.ImageIO.write(img, "png", new File(out));
    }
}
