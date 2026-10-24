import java.nio.file.*;
import java.util.*;

/**
 * Re-lays the floor of a square respawn plaza in the BLUE base's palette, around the respawn block at its centre
 * (cx, g, cz); the border ring (|i| or |j| = r) and the respawn block itself stay as they are. By distance d = |i|+|j|:
 *   d 1-2: polished cut limestone (a cream diamond round the respawn spot), d 3: blue concrete (the colour of the BLUE flag),
 *   d 4: polished cut limestone, d 5+: smooth stone / polished andesite checker, with cut limestone brick paths
 *   along the two axes out to the gates. The four inner corners become planters (moss with a flowering azalea).
 * Floor cells are swaps of exactly the block read from the region (built floors are not ground), so anything changed
 * since is left. Supply hay bales on the plaza become ammo containers (it is the ammo depot's point).
 * Usage: java RespawnPlaza <region> cx g cz r
 */
public class RespawnPlaza {
    public static void main(String[] a) throws Exception {
        Region reg = new Region(Path.of(a[0]));
        int cx = Integer.parseInt(a[1]), g = Integer.parseInt(a[2]), cz = Integer.parseInt(a[3]), r = Integer.parseInt(a[4]);
        Canvas c = new Canvas(0, 0);
        int floor = 0, planters = 0, crates = 0;
        for (int i = -(r - 1); i <= r - 1; i++) for (int j = -(r - 1); j <= r - 1; j++) {
            int x = cx + i, z = cz + j, d = Math.abs(i) + Math.abs(j);
            if (d == 0) continue;
            String cur = Region.name(reg.state(x, g, z));
            boolean corner = Math.abs(i) == r - 1 && Math.abs(j) == r - 1;
            String want;
            if (corner) want = "minecraft:moss_block";
            else if (d <= 2 || d == 4) want = "create:polished_cut_limestone";
            else if (d == 3) want = "minecraft:blue_concrete";
            else if (i == 0 || j == 0) want = "create:cut_limestone_bricks";
            else want = Math.floorMod(i + j, 2) == 0 ? "minecraft:smooth_stone" : "minecraft:polished_andesite";
            if (!cur.equals(want)) { c.swap(x, g, z, cur, want); floor++; }
            String above = reg.state(x, g + 1, z);
            if (corner && Region.isAir(above)) { c.set(x, g + 1, z, "flowering_azalea"); planters++; }
            if (Region.name(above).equals("minecraft:hay_block")) {
                c.swap(x, g + 1, z, "minecraft:hay_block", "createbigcannons:autocannon_ammo_container[axis=x,state=closed,waterlogged=false]");
                crates++;
            }
        }
        System.err.println("respawn plaza: floor " + floor + " cells, planters " + planters + ", ammo crates " + crates);
        c.print(new String[0]);
    }
}
