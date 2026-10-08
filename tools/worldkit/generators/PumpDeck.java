import java.util.*;

/**
 * Second floor of the pumping station (run after Complex): an industrial iron catwalk ring around the capture yard at
 * F+4 (players stand at F+5, above the capture area), joining the pipe racks, the control building's upper floor and
 * the pump house roof. Access: catwalk stairs from the north and south paths, two ladders from the yard, the control
 * building's ladder, and a stair from the west bridge onto the pump house roof. Mirrored north/south.
 * Additive: the rack columns that pass through the deck stay. The openings it needs in existing blocks (parapet,
 * control building wall, lamp-post lanterns) are clears, run only with --clear.
 * Usage: java -cp <worldkit classes> PumpDeck <cx> <groundY> <cz> > deck.build   (or --commands [--clear] for RCON fill commands)      (same centre as Complex)
 */
public class PumpDeck {
    static int CX, G, CZ, F, D;
    static Canvas C;
    static final Set<Long> deck = new HashSet<>();      // cells with a catwalk (or a rack column inside the ring)
    static final Set<Long> openings = new HashSet<>();  // cells next to the deck where the railing stays open

    static final String CATWALK = "createdeco:industrial_iron_catwalk[bottom=false]";
    static final String IRON = "create:industrial_iron_block";
    // createdeco catwalk stairs face their low end (the opposite of vanilla stairs)
    static final String STAIRS = "createdeco:industrial_iron_catwalk_stairs[railing_left=true,railing_right=true,facing=";

    public static void main(String[] a) {
        CX = Integer.parseInt(a[0]); G = Integer.parseInt(a[1]); CZ = Integer.parseInt(a[2]);
        F = G + 1;
        D = F + 4;
        C = new Canvas(CX, CZ);

        ring();
        supports();
        for (int sz : new int[] {-1, 1}) {
            stairs(sz);
            ladder(sz);
            cover(sz);
        }
        roofAccess();
        controlBuildingDoor();
        railings();

        C.print(a);
    }

    // catwalk strips under the pipe racks (z = +-10..12) and bridges west (x = -13..-11) and east (x = 10..12)
    static void ring() {
        for (int sz : new int[] {-1, 1}) area(-13, 12, Math.min(10 * sz, 12 * sz), Math.max(10 * sz, 12 * sz));
        area(-13, -11, -9, 9);
        area(10, 12, -9, 9);
        // lamp posts at (+-11, +-10) end under the deck: their lanterns are in the way
        for (int x : new int[] {-11, 11}) for (int z : new int[] {-10, 10}) C.clear(x, D, z, x, D, z, "lamp-post lantern where the deck goes");
        for (long c : deck) {
            int x = (int) (c >> 32), z = (int) c;
            set(x, D, z, CATWALK);
        }
    }

    static void area(int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) deck.add(key(x, z));
    }

    // girder columns under the bridges; the north/south strips sit on the existing rack columns
    static void supports() {
        for (int sz : new int[] {-1, 1}) {
            fill(-13, F, 6 * sz, -13, D - 1, 6 * sz, "create:metal_girder");
            fill(-13, F, 10 * sz, -13, D - 1, 10 * sz, "create:metal_girder");
            fill(12, F, 4 * sz, 12, D - 1, 4 * sz, "create:metal_girder");
            fill(-6, F, 12 * sz, -6, D - 1, 12 * sz, "create:metal_girder");
            fill(6, F, 12 * sz, 6, D - 1, 12 * sz, "create:metal_girder");
        }
    }

    // catwalk stairs from the tank path up to the strip, on a solid iron wedge (cover on the approach)
    static void stairs(int sz) {
        String lowEnd = sz < 0 ? "north" : "south";      // climbs towards the yard
        for (int i = 0; i <= 4; i++) {
            int z = (17 - i) * sz;
            if (i > 0) fill(3, F, z, 4, F + i - 1, z, IRON);
            fill(3, F + i, z, 4, F + i, z, STAIRS + lowEnd + "]");
            C.clear(3, F + i + 1, z, 4, F + i + 3, z, "headroom over the stairs");
        }
        openings.add(key(3, 13 * sz));
        openings.add(key(4, 13 * sz));
    }

    // ladder from the yard up the inner side of the west bridge
    static void ladder(int sz) {
        int z = 6 * sz;
        fill(-11, F, z, -11, D - 1, z, IRON);
        fill(-10, F, z, -10, D, z, "createdeco:industrial_iron_ladder[facing=east]");
        openings.add(key(-10, z));
    }

    // 2-high crates at the outer corners of the strips, single crates on the west bridge
    static void cover(int sz) {
        for (int x : new int[] {-9, 9}) {
            set(x, D + 1, 12 * sz, "barrel[facing=up]");
            set(x, D + 2, 12 * sz, "barrel[facing=up]");
        }
        set(-12, D + 1, 3 * sz, "barrel[facing=up]");
        set(11, D + 1, 7 * sz, "barrel[facing=up]");
    }

    // from the west bridge onto the pump house roof (roof surface F+7, through an opening in the parapet)
    static void roofAccess() {
        fill(-14, D, -1, -14, D, 1, CATWALK);
        fill(-14, D + 1, -1, -14, D + 1, 1, STAIRS + "east]");
        fill(-15, D + 2, -1, -15, D + 2, 1, STAIRS + "east]");
        fill(-15, D + 1, -1, -15, D + 1, 1, IRON);
        C.clear(-16, F + 7, -1, -16, F + 8, 1, "opening in the pump house parapet");
        C.clear(-14, D + 2, -1, -14, D + 4, 1, "headroom over the roof stair");
        for (int z = -1; z <= 1; z++) deck.add(key(-14, z));
        for (int z = -1; z <= 1; z++) openings.add(key(-15, z));
    }

    // control building upper floor opens onto the east bridge
    static void controlBuildingDoor() {
        C.clear(13, D + 1, -1, 13, D + 2, 1, "doorway in the control building wall");
        for (int z = -1; z <= 1; z++) openings.add(key(13, z));
    }

    // railing on every deck edge that isn't an access opening (not where a stair or crate already stands)
    static void railings() {
        for (long c : deck) {
            int x = (int) (c >> 32), z = (int) c;
            boolean n = edge(x, z - 1), s = edge(x, z + 1), w = edge(x - 1, z), e = edge(x + 1, z);
            if (!(n || s || w || e) || C.cells.containsKey(Canvas.key(x, D + 1, z))) continue;
            set(x, D + 1, z, "createdeco:industrial_iron_catwalk_railing[north=" + n + ",south=" + s + ",west=" + w + ",east=" + e + "]");
        }
    }

    static boolean edge(int x, int z) {
        return !deck.contains(key(x, z)) && !openings.contains(key(x, z));
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // ---------------- helpers: draw into the additive canvas ----------------
    static void fill(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        C.fill(x0, y0, z0, x1, y1, z1, block);
    }

    static void set(int x, int y, int z, String block) {
        C.fill(x, y, z, x, y, z, block);
    }
}
