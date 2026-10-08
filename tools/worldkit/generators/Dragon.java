import java.util.*;

/**
 * Ender dragon statue perched on an end-stone island (a test build). Organic shapes: the body, neck, tail and legs are
 * tubes swept along Catmull-Rom splines with a varying radius; wings are bone tubes with flat membranes between them.
 * The dragon faces east (+x); mirrored across z. Coordinates relative to the island centre; water surface WY.
 * Usage: java -cp <worldkit classes> Dragon <cx> <waterY> <cz> > dragon.build
 */
public class Dragon {
    static Canvas C;
    static int WY;

    // what a voxel is made of, higher wins
    static final int ISLAND = 1, MEMBRANE = 2, BONE = 3, BODY = 4, SPIKE = 5, CLAW = 6, EYE = 7;

    record Sample(double x, double y, double z, double r) {}

    static final Map<Long, Integer> kind = new HashMap<>();
    static final Map<Long, Boolean> belly = new HashMap<>();

    public static void main(String[] a) {
        int cx = Integer.parseInt(a[0]);
        WY = Integer.parseInt(a[1]);
        int cz = Integer.parseInt(a[2]);
        C = new Canvas(cx, cz);

        island();
        int top = WY + 2;

        // spine from the tail tip to the head; the tail curls around the island's south side
        List<Sample> spine = spline(new double[][] {
                {-30, top + 1, 14, 0.6}, {-24, top + 1, 17, 1.0}, {-16, top + 1, 15, 1.5}, {-10, top + 2, 9, 2.1},
                {-6, top + 3, 3, 2.9}, {-2, top + 5, 0, 3.7}, {3, top + 6, 0, 4.1}, {8, top + 7, 0, 3.8},
                {11, top + 10, 0, 2.8}, {13, top + 15, 0, 2.1}, {15, top + 19, -1, 1.8}, {18, top + 22, -1, 1.9}});
        tube(spine, BODY, true);
        // head: skull and snout, open lower jaw
        tube(spline(new double[][] {{17, top + 22, -1, 2.0}, {20, top + 22.5, -1, 2.3}, {24, top + 22, -1, 1.7}, {28, top + 21.5, -1, 1.2}}), BODY, false);
        tube(spline(new double[][] {{19, top + 20, -1, 1.2}, {23, top + 19.5, -1, 1.0}, {27, top + 18.5, -1, 0.8}}), BODY, false);
        for (int s : new int[] {-1, 1}) {
            // horns sweeping back
            tube(spline(new double[][] {{19, top + 24, -1 + 1.5 * s, 0.8}, {16, top + 26, -1 + 2.5 * s, 0.6}, {13, top + 27, -1 + 3 * s, 0.5}}), SPIKE, false);
            mark(21, top + 23, -1 + 2 * s, EYE);
            mark(22, top + 23, -1 + 2 * s, EYE);
            // teeth along the jaw
            for (int x = 22; x <= 27; x += 2) mark(x, top + 20, -1 + s, CLAW);

            // legs: front (shoulders) and hind (hips), each ending in three claws on the island
            leg(new double[][] {{7, top + 5, 3 * s, 1.6}, {9, top + 3, 4 * s, 1.2}, {10, top + 1, 4 * s, 1.0}}, s);
            leg(new double[][] {{-4, top + 4, 3 * s, 2.3}, {-6, top + 3, 5 * s, 1.8}, {-4, top + 1, 6 * s, 1.1}}, s);

            wing(s, top);
        }
        // spikes along the back from the neck to the tail
        for (int i = 0; i < spine.size(); i += 9) {
            Sample p = spine.get(i);
            if (p.r < 1.0) continue;
            double h = Math.min(2.5, p.r * 0.7);
            tube(List.of(new Sample(p.x, p.y + p.r * 0.8, p.z, 0.6), new Sample(p.x, p.y + p.r + h, p.z, 0.35)), SPIKE, false);
        }

        paint();
        C.print(a);
    }

    // irregular end-stone island; its base goes down under the water
    static void island() {
        for (int x = -16; x <= 16; x++)
            for (int z = -16; z <= 16; z++) {
                double angle = Math.atan2(z, x), d = Math.sqrt(x * x + z * z);
                double radius = 12 + 2 * Math.sin(3 * angle) + 1.5 * Math.cos(5 * angle + 1);
                if (d > radius) continue;
                double edge = 1 - d / radius;
                int surface = WY + 1 + (int) Math.round(2.5 * Math.sqrt(edge) + noise(x, 0, z) * 0.8);
                for (int y = WY - 8; y <= surface; y++) mark(x, y, z, ISLAND);
            }
    }

    static void leg(double[][] points, int s) {
        List<Sample> leg = spline(points);
        tube(leg, BODY, false);
        Sample foot = leg.get(leg.size() - 1);
        for (int c = -1; c <= 1; c++) {
            double dz = c * 1.0 * s, dx = 1.8;
            tube(List.of(new Sample(foot.x, foot.y - 0.5, foot.z + dz, 0.5), new Sample(foot.x + dx, foot.y - 1, foot.z + dz * 1.4, 0.4)), CLAW, false);
        }
    }

    // bat-like wing raised over the back: arm and four fingers, membrane between them and down to the body
    static void wing(int s, int top) {
        double[] shoulder = {4, top + 9, 3 * s}, elbow = {1, top + 18, 12 * s}, wrist = {-3, top + 26, 21 * s};
        double[][] tips = {{8, top + 31, 32 * s}, {-5, top + 27, 35 * s}, {-14, top + 19, 31 * s}, {-18, top + 12, 21 * s}};
        double[] flank = {-9, top + 6, 3 * s};
        tube(List.of(new Sample(shoulder[0], shoulder[1], shoulder[2], 1.4), new Sample(elbow[0], elbow[1], elbow[2], 1.1)), BONE, false);
        tube(List.of(new Sample(elbow[0], elbow[1], elbow[2], 1.1), new Sample(wrist[0], wrist[1], wrist[2], 0.9)), BONE, false);
        for (double[] tip : tips) {
            tube(List.of(new Sample(wrist[0], wrist[1], wrist[2], 0.8), new Sample(tip[0], tip[1], tip[2], 0.55)), BONE, false);
        }
        membrane(wrist, tips[0], tips[1]);
        membrane(wrist, tips[1], tips[2]);
        membrane(wrist, tips[2], tips[3]);
        membrane(wrist, tips[3], elbow);
        membrane(elbow, tips[3], flank);
        membrane(shoulder, elbow, flank);
        // claw at the wrist
        tube(List.of(new Sample(wrist[0], wrist[1], wrist[2], 0.6), new Sample(wrist[0] + 1.5, wrist[1] + 1.5, wrist[2], 0.4)), CLAW, false);
    }

    // a membrane triangle with a slight sag towards its middle
    static void membrane(double[] a, double[] b, double[] c) {
        double step = 0.3;
        for (double u = 0; u <= 1; u += step / 20) {
            for (double v = 0; u + v <= 1; v += step / 20) {
                double w = 1 - u - v;
                double sag = -1.2 * u * v * w * 27 / 4;   // 0 on the edges, up to ~1 block in the middle
                double x = a[0] * w + b[0] * u + c[0] * v;
                double y = a[1] * w + b[1] * u + c[1] * v + sag;
                double z = a[2] * w + b[2] * u + c[2] * v;
                mark((int) Math.round(x), (int) Math.round(y), (int) Math.round(z), MEMBRANE);
            }
        }
    }

    // Catmull-Rom through control points {x, y, z, r}, sampled every ~0.25 block
    static List<Sample> spline(double[][] p) {
        List<Sample> out = new ArrayList<>();
        for (int i = 0; i < p.length - 1; i++) {
            double[] p0 = p[Math.max(0, i - 1)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(p.length - 1, i + 2)];
            double len = Math.sqrt(sq(p2[0] - p1[0]) + sq(p2[1] - p1[1]) + sq(p2[2] - p1[2]));
            int n = Math.max(2, (int) (len / 0.25));
            for (int k = 0; k < n; k++) {
                double t = (double) k / n;
                double[] q = new double[4];
                for (int j = 0; j < 4; j++) {
                    q[j] = 0.5 * (2 * p1[j] + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t * t
                            + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t * t * t);
                }
                out.add(new Sample(q[0], q[1], q[2], q[3]));
            }
        }
        double[] last = p[p.length - 1];
        out.add(new Sample(last[0], last[1], last[2], last[3]));
        return out;
    }

    // spheres along the samples (linearly interpolated between them so thin parts stay connected)
    static void tube(List<Sample> samples, int k, boolean markBelly) {
        for (int i = 0; i < samples.size(); i++) {
            Sample a = samples.get(i), b = samples.get(Math.min(i + 1, samples.size() - 1));
            double len = Math.sqrt(sq(b.x - a.x) + sq(b.y - a.y) + sq(b.z - a.z));
            int n = Math.max(1, (int) Math.ceil(len / 0.2));
            for (int j = 0; j < n; j++) {
                double t = (double) j / n;
                Sample p = new Sample(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t, a.r + (b.r - a.r) * t);
                sphere(p, k, markBelly);
            }
        }
    }

    static void sphere(Sample p, int k, boolean markBelly) {
        int r = (int) Math.ceil(p.r);
        for (int x = (int) Math.floor(p.x) - r; x <= (int) Math.ceil(p.x) + r; x++)
            for (int y = (int) Math.floor(p.y) - r; y <= (int) Math.ceil(p.y) + r; y++)
                for (int z = (int) Math.floor(p.z) - r; z <= (int) Math.ceil(p.z) + r; z++) {
                    double d2 = sq(x - p.x) + sq(y - p.y) + sq(z - p.z);
                    if (d2 > p.r * p.r + 0.15) continue;
                    mark(x, y, z, k);
                    if (markBelly && k == BODY) belly.merge(Canvas.key(x, y, z), y < p.y - p.r * 0.35, (o, n) -> o && n);
                }
    }

    static void mark(int x, int y, int z, int k) {
        kind.merge(Canvas.key(x, y, z), k, Math::max);
    }

    // materials: speckled blackstone/obsidian hide, grey belly, polished bones, black membranes
    static void paint() {
        for (var e : kind.entrySet()) {
            long key = e.getKey();
            int x = Canvas.x(key), y = Canvas.y(key), z = Canvas.z(key);
            double n = noise(x, y, z);
            String block = switch (e.getValue()) {
                case ISLAND -> y < WY - 1 ? (n < 0.5 ? "stone" : "andesite") : n < 0.15 ? "obsidian" : "end_stone";
                case MEMBRANE -> n < 0.85 ? "black_concrete" : "gray_concrete";
                case BONE -> "polished_blackstone";
                case BODY -> Boolean.TRUE.equals(belly.get(key)) ? (n < 0.6 ? "polished_deepslate" : "deepslate_tiles")
                        : n < 0.55 ? "blackstone" : n < 0.85 ? "polished_blackstone" : "obsidian";
                case SPIKE -> "polished_blackstone_bricks";
                case CLAW -> "bone_block";
                case EYE -> "crying_obsidian";
                default -> throw new IllegalStateException();
            };
            C.set(x, y, z, block);
        }
    }

    static double noise(int x, int y, int z) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }

    static double sq(double v) {
        return v * v;
    }
}
