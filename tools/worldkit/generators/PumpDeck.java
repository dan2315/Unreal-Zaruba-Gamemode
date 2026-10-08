import java.util.*;

/**
 * Second floor of the pumping station (run after Complex): an industrial iron catwalk ring around the capture yard at
 * F+4 (players stand at F+5, above the capture area), joining the pipe racks, the control building's upper floor and
 * the pump house roof. Access: catwalk stairs from the north and south paths, two ladders from the yard, the control
 * building's ladder, and a stair from the west bridge onto the pump house roof. Mirrored north/south.
 * Usage: java PumpDeck.java <cx> <groundY> <cz>      (same centre as Complex)
 */
public class PumpDeck {
    static int CX, G, CZ, F, D;
    static final List<String> out = new ArrayList<>();
    static final Set<Long> deck = new HashSet<>();      // cells with a catwalk (or a rack column inside the ring)
    static final Set<Long> openings = new HashSet<>();  // cells next to the deck where the railing stays open

    static final String CATWALK = "createdeco:industrial_iron_catwalk[bottom=false]";
    static final String IRON = "create:industrial_iron_block";

    public static void main(String[] a) {
        CX = Integer.parseInt(a[0]); G = Integer.parseInt(a[1]); CZ = Integer.parseInt(a[2]);
        F = G + 1;
        D = F + 4;
        out.add("forceload add " + (CX - 40) + " " + (CZ - 30) + " " + (CX + 30) + " " + (CZ + 30));

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

        out.add("forceload remove " + (CX - 40) + " " + (CZ - 30) + " " + (CX + 30) + " " + (CZ + 30));
        out.forEach(c -> System.out.println("execute in unrealzaruba:game_dim run " + c));
    }

    // catwalk strips under the pipe racks (z = +-10..12) and bridges west (x = -13..-11) and east (x = 10..12)
    static void ring() {
        for (int sz : new int[] {-1, 1}) area(-13, 12, Math.min(10 * sz, 12 * sz), Math.max(10 * sz, 12 * sz));
        area(-13, -11, -9, 9);
        area(10, 12, -9, 9);
        // lamp posts at (+-11, +-10) end under the deck now: their lanterns make way for the catwalk
        for (int x : new int[] {-11, 11}) for (int z : new int[] {-10, 10}) set(x, D, z, CATWALK);
        for (long c : deck) {
            int x = (int) (c >> 32), z = (int) c;
            fillReplace(x, D, z, x, D, z, CATWALK, "air");   // keeps the rack columns that pass through
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
        String facing = sz < 0 ? "north" : "south";      // createdeco stairs face their low end; climbs towards the yard
        for (int i = 0; i <= 4; i++) {
            int z = (17 - i) * sz;
            if (i > 0) fill(3, F, z, 4, F + i - 1, z, IRON);
            fill(3, F + i, z, 4, F + i, z, "createdeco:industrial_iron_catwalk_stairs[facing=" + facing + ",railing_left=true,railing_right=true]");
            fill(3, F + i + 1, z, 4, F + i + 3, z, "air");
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

    // from the west bridge onto the pump house roof (roof surface F+7, parapet opened)
    static void roofAccess() {
        fill(-14, D, -1, -14, D, 1, CATWALK);
        fill(-14, D + 1, -1, -14, D + 1, 1, "createdeco:industrial_iron_catwalk_stairs[facing=east,railing_left=true,railing_right=true]");
        fill(-15, D + 2, -1, -15, D + 2, 1, "createdeco:industrial_iron_catwalk_stairs[facing=east,railing_left=true,railing_right=true]");
        fill(-15, D + 1, -1, -15, D + 1, 1, IRON);
        fill(-16, F + 7, -1, -16, F + 8, 1, "air");
        fill(-14, D + 2, -1, -14, D + 4, 1, "air");
        for (int z = -1; z <= 1; z++) deck.add(key(-14, z));
        for (int z = -1; z <= 1; z++) openings.add(key(-15, z));
    }

    // control building upper floor opens onto the east bridge
    static void controlBuildingDoor() {
        fill(13, D + 1, -1, 13, D + 2, 1, "air");
        for (int z = -1; z <= 1; z++) openings.add(key(13, z));
    }

    // railing on every deck edge that isn't an access opening
    static void railings() {
        for (long c : deck) {
            int x = (int) (c >> 32), z = (int) c;
            boolean n = edge(x, z - 1), s = edge(x, z + 1), w = edge(x - 1, z), e = edge(x + 1, z);
            if (!(n || s || w || e)) continue;
            fillReplace(x, D + 1, z, x, D + 1, z,
                    "createdeco:industrial_iron_catwalk_railing[north=" + n + ",south=" + s + ",west=" + w + ",east=" + e + "]", "air");
        }
    }

    static boolean edge(int x, int z) {
        return !deck.contains(key(x, z)) && !openings.contains(key(x, z));
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // ---------------- helpers (as in Complex) ----------------
    static void fill(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        out.add("fill " + (CX + x0) + " " + y0 + " " + (CZ + z0) + " " + (CX + x1) + " " + y1 + " " + (CZ + z1) + " " + id(block));
    }

    static void fillReplace(int x0, int y0, int z0, int x1, int y1, int z1, String block, String replace) {
        out.add("fill " + (CX + x0) + " " + y0 + " " + (CZ + z0) + " " + (CX + x1) + " " + y1 + " " + (CZ + z1) + " " + id(block) + " replace " + id(replace));
    }

    static void set(int x, int y, int z, String block) {
        out.add("setblock " + (CX + x) + " " + y + " " + (CZ + z) + " " + id(block));
    }

    static String id(String block) {
        int end = block.length();
        for (char c : new char[] {'[', '{'}) if (block.indexOf(c) >= 0) end = Math.min(end, block.indexOf(c));
        return block.substring(0, end).contains(":") ? block : "minecraft:" + block;
    }
}
