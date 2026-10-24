import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Imports a vanilla structure file (.nbt, from a structure block or a download) into the blueprint library.
 *   - Air and structure voids become "nothing". The ground layer is the lowest layer where at least half of the cells
 *     are natural ground (grass, dirt, stone...), else layer 0; natural ground blocks in it are dropped (unless
 *     --keep-ground), so the map's own ground stays where the piece is set.
 *   - Block entity data is kept, except for Create blocks (their kinetic network data points into the source world;
 *     Create rebuilds it).
 *   - Create bearing contraptions (windmill sails, rotating arms) are entities in the file; their blocks are added as
 *     plain blocks as assembled (angle 0): each contraption is matched to its bearing through ControllerRelative and
 *     its blocks are set relative to the block in front of the bearing. The blueprint grows to hold them. Other
 *     entities (item frames, armor stands...) are only listed.
 *   - With --known <file> (one ns:id per line) ids the server lacks are dropped and listed; --replace swaps them:
 *     old=new keeps all properties, old=new|p1,p2+k:v keeps only p1, p2 and adds k=v.
 * Usage: java ImportNbt <file.nbt> <out.bp> <name> [--tags a,b] [--known known.txt] [--replace spec,spec] [--keep-ground]
 */
public class ImportNbt {
    record Rep(String name, Set<String> keep, Map<String, Object> add) {}

    static final Map<String, int[]> DIR = Map.of("north", new int[] {0, 0, -1}, "south", new int[] {0, 0, 1}, "east", new int[] {1, 0, 0},
            "west", new int[] {-1, 0, 0}, "up", new int[] {0, 1, 0}, "down", new int[] {0, -1, 0});

    static Set<String> known;
    static Map<String, Rep> replace = new HashMap<>();
    static Map<String, Integer> unknown = new TreeMap<>();

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        Map<String, Object> root;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(a[0]))))) {
            in.readByte();
            in.readUTF();
            root = (Map<String, Object>) ScanWorld.read(in, 10);
        }
        List<String> args = Arrays.asList(a);
        int k = args.indexOf("--known");
        if (k >= 0) known = new HashSet<>(Files.readAllLines(Path.of(a[k + 1])));
        k = args.indexOf("--replace");
        if (k >= 0) for (String spec : a[k + 1].split(",(?=[a-z0-9_:]+=)")) {
            String old = id(spec.substring(0, spec.indexOf('='))), rest = spec.substring(spec.indexOf('=') + 1);
            String[] parts = rest.split("\\|", 2);
            Set<String> keep = null;
            Map<String, Object> add = new LinkedHashMap<>();
            if (parts.length > 1) {
                String[] p = parts[1].split("\\+");
                keep = p[0].isEmpty() ? Set.of() : Set.of(p[0].split(","));
                for (int i = 1; i < p.length; i++) add.put(p[i].split(":")[0], p[i].split(":")[1]);
            }
            replace.put(old, new Rep(id(parts[0]), keep, add));
        }
        boolean keepGround = args.contains("--keep-ground");

        // all blocks by position (contraption blocks may lie outside the structure's box)
        Map<List<Integer>, String> blocks = new HashMap<>();
        List<Object> palette = (List<Object>) root.get("palette");
        if (palette == null) palette = (List<Object>) ((List<Object>) root.get("palettes")).get(0);
        String[] states = new String[palette.size()];
        for (int i = 0; i < states.length; i++) states[i] = state((Map<String, Object>) palette.get(i));
        Map<String, List<int[]>> bearings = new HashMap<>(); // facing -> positions
        for (Object o : (List<Object>) root.get("blocks")) {
            Map<String, Object> b = (Map<String, Object>) o;
            List<Object> pos = (List<Object>) b.get("pos");
            int x = (Integer) pos.get(0), y = (Integer) pos.get(1), z = (Integer) pos.get(2);
            String s = states[(Integer) b.get("state")];
            if (s == null || Region.isAir(s) || s.startsWith("minecraft:structure_void")) continue;
            if (Region.name(s).endsWith("_bearing")) {
                String f = s.replaceAll(".*facing=([a-z]+).*", "$1");
                bearings.computeIfAbsent(f, q -> new ArrayList<>()).add(new int[] {x, y, z});
            }
            if (b.get("nbt") instanceof Map<?, ?> nbt && !nbt.isEmpty() && !s.startsWith("create:")) s = s + Region.snbt((Map<String, Object>) nbt);
            blocks.put(List.of(x, y, z), s);
        }
        // Create bearing contraptions
        Map<String, Integer> otherEntities = new TreeMap<>();
        int contraptionBlocks = 0;
        if (root.get("entities") instanceof List<?> ents) for (Object o : ents) {
            Map<String, Object> nbt = (Map<String, Object>) ((Map<String, Object>) o).get("nbt");
            String eid = String.valueOf(nbt.get("id"));
            Map<String, Object> con = (Map<String, Object>) nbt.get("Contraption");
            if (con == null || !(nbt.get("ControllerRelative") instanceof List<?> rel)) { otherEntities.merge(eid, 1, Integer::sum); continue; }
            int[] cr = {(Integer) rel.get(0), (Integer) rel.get(1), (Integer) rel.get(2)};
            String facing = null;
            for (var e : DIR.entrySet()) if (e.getValue()[0] == -cr[0] && e.getValue()[1] == -cr[1] && e.getValue()[2] == -cr[2]) facing = e.getKey();
            List<int[]> cands = bearings.getOrDefault(facing, List.of());
            if (cands.isEmpty()) { System.err.println("contraption " + con.get("Type") + ": no bearing facing " + facing + ", skipped"); continue; }
            if (cands.size() > 1) System.err.println("contraption " + con.get("Type") + ": " + cands.size() + " bearings facing " + facing + ", using the first");
            int[] bp = cands.remove(0);
            int ax = bp[0] - cr[0], ay = bp[1] - cr[1], az = bp[2] - cr[2];
            Map<String, Object> bl = (Map<String, Object>) con.get("Blocks");
            List<Object> pal = (List<Object>) bl.get("Palette");
            for (Object bo : (List<Object>) bl.get("BlockList")) {
                Map<String, Object> b = (Map<String, Object>) bo;
                long l = ((Number) b.get("Pos")).longValue();
                int x = (int) (l >> 38), y = (int) (l << 52 >> 52), z = (int) (l << 26 >> 38);
                String s = state((Map<String, Object>) pal.get(((Number) b.get("State")).intValue()));
                if (s == null || Region.isAir(s)) continue;
                blocks.put(List.of(ax + x, ay + y, az + z), s);
                contraptionBlocks++;
            }
            System.err.println("contraption " + con.get("Type") + " on the bearing at " + Arrays.toString(bp) + ": " + bl.get("BlockList").getClass().getSimpleName()
                    + " of " + ((List<?>) bl.get("BlockList")).size() + " blocks added as placed (angle 0)");
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (List<Integer> p : blocks.keySet()) {
            x0 = Math.min(x0, p.get(0)); y0 = Math.min(y0, p.get(1)); z0 = Math.min(z0, p.get(2));
            x1 = Math.max(x1, p.get(0)); y1 = Math.max(y1, p.get(1)); z1 = Math.max(z1, p.get(2));
        }
        int w = x1 - x0 + 1, h = y1 - y0 + 1, d = z1 - z0 + 1;
        // ground layer: lowest layer that is half natural ground
        int ground = 0;
        for (int y = 0; y < Math.min(h, 4); y++) {
            int nat = 0;
            for (var e : blocks.entrySet()) if (e.getKey().get(1) - y0 == y && groundBlock(e.getValue())) nat++;
            if (nat * 2 >= w * d) { ground = y; break; }
        }
        Blueprint out = new Blueprint(w, h, d);
        out.name = a[2];
        out.ground = ground;
        k = args.indexOf("--tags");
        if (k >= 0) out.tags.addAll(List.of(a[k + 1].split(",")));
        out.tags.add("imported");
        int dropped = 0;
        for (var e : blocks.entrySet()) {
            int x = e.getKey().get(0) - x0, y = e.getKey().get(1) - y0, z = e.getKey().get(2) - z0;
            if (!keepGround && y <= ground && groundBlock(e.getValue())) { dropped++; continue; }
            out.cells[y][z][x] = out.index(e.getValue());
        }
        out.save(Path.of(a[1]));
        System.err.printf("%s: %dx%dx%d, %d blocks (%d from contraptions), ground layer %d (%d natural ground blocks dropped), DataVersion %s, author %s%n",
                out.name, w, h, d, out.count(), contraptionBlocks, ground, dropped, root.get("DataVersion"), root.get("author"));
        if (!otherEntities.isEmpty()) System.err.println("entities not imported: " + otherEntities);
        if (!unknown.isEmpty()) System.err.println("UNKNOWN ids (dropped; give --replace): " + unknown);
    }

    /** "ns:id[props]" for a palette entry, after replacements; null when the server does not know the block. */
    @SuppressWarnings("unchecked")
    static String state(Map<String, Object> p) {
        String name = (String) p.get("Name");
        Map<String, Object> props = new LinkedHashMap<>((Map<String, Object>) p.getOrDefault("Properties", Map.of()));
        Rep rep = replace.get(name);
        if (rep != null) {
            name = rep.name();
            if (rep.keep() != null) props.keySet().retainAll(rep.keep());
            props.putAll(rep.add());
        }
        if (known != null && !known.contains(name) && !name.equals("minecraft:air") && !name.equals("minecraft:structure_void")) {
            unknown.merge(name, 1, Integer::sum);
            return null;
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("Name", name);
        if (!props.isEmpty()) entry.put("Properties", props);
        return Region.stateString(entry);
    }

    static boolean groundBlock(String s) {
        String n = Region.name(s).replace("minecraft:", "");
        return Set.of("grass_block", "dirt", "coarse_dirt", "podzol", "stone", "rooted_dirt", "moss_block", "sand", "gravel", "mud", "dirt_path").contains(n);
    }

    static String id(String s) { return s.contains(":") ? s : "minecraft:" + s; }
}
