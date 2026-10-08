import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Renames blocks in region files: block palettes, copycat materials/items inside block entities, and block entity
 * ids. Rules come from a mapping file, one per line:
 *   old:id -> new:id [set k=v,...] [drop k,...] [axisFromFacing]
 *   be old:id -> new:id        (block entity id rename)
 *   be old:id -> REMOVE        (drop the block entity; the block makes a fresh one)
 * Usage: java Migrate <mapping file> <region dir> <out dir>
 * Writes each changed region file to the out dir (same name) and prints counts. Chunks it changes get
 * isLightOn=0 so the server recomputes their light.
 */
public class Migrate {
    // ---------- lossless NBT ----------
    static final class Tag {
        final byte type; Object v; byte listType;
        Tag(byte type, Object v) { this.type = type; this.v = v; }
        @SuppressWarnings("unchecked") LinkedHashMap<String, Tag> map() { return (LinkedHashMap<String, Tag>) v; }
        @SuppressWarnings("unchecked") List<Tag> list() { return (List<Tag>) v; }
        String str() { return (String) v; }
    }

    static Tag read(DataInputStream in, byte t) throws IOException {
        switch (t) {
            case 1: return new Tag(t, in.readByte());
            case 2: return new Tag(t, in.readShort());
            case 3: return new Tag(t, in.readInt());
            case 4: return new Tag(t, in.readLong());
            case 5: return new Tag(t, in.readFloat());
            case 6: return new Tag(t, in.readDouble());
            case 7: { byte[] b = new byte[in.readInt()]; in.readFully(b); return new Tag(t, b); }
            case 8: return new Tag(t, in.readUTF());
            case 9: {
                byte et = in.readByte(); int n = in.readInt();
                List<Tag> l = new ArrayList<>(Math.max(0, n));
                for (int i = 0; i < n; i++) l.add(read(in, et));
                Tag tag = new Tag(t, l); tag.listType = et; return tag;
            }
            case 10: {
                LinkedHashMap<String, Tag> m = new LinkedHashMap<>();
                for (byte ct; (ct = in.readByte()) != 0; ) { String k = in.readUTF(); m.put(k, read(in, ct)); }
                return new Tag(t, m);
            }
            case 11: { int[] a = new int[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readInt(); return new Tag(t, a); }
            case 12: { long[] a = new long[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readLong(); return new Tag(t, a); }
            default: throw new IOException("bad tag " + t);
        }
    }

    static void write(DataOutputStream out, Tag tag) throws IOException {
        switch (tag.type) {
            case 1 -> out.writeByte((Byte) tag.v);
            case 2 -> out.writeShort((Short) tag.v);
            case 3 -> out.writeInt((Integer) tag.v);
            case 4 -> out.writeLong((Long) tag.v);
            case 5 -> out.writeFloat((Float) tag.v);
            case 6 -> out.writeDouble((Double) tag.v);
            case 7 -> { byte[] b = (byte[]) tag.v; out.writeInt(b.length); out.write(b); }
            case 8 -> out.writeUTF(tag.str());
            case 9 -> { out.writeByte(tag.list().isEmpty() ? 0 : tag.listType); out.writeInt(tag.list().size()); for (Tag e : tag.list()) write(out, e); }
            case 10 -> { for (var e : tag.map().entrySet()) { out.writeByte(e.getValue().type); out.writeUTF(e.getKey()); write(out, e.getValue()); } out.writeByte(0); }
            case 11 -> { int[] a = (int[]) tag.v; out.writeInt(a.length); for (int x : a) out.writeInt(x); }
            case 12 -> { long[] a = (long[]) tag.v; out.writeInt(a.length); for (long x : a) out.writeLong(x); }
            default -> throw new IOException("bad tag " + tag.type);
        }
    }

    // ---------- rules ----------
    record Rule(String to, Map<String, String> set, Set<String> drop, boolean axisFromFacing) {}
    static final Map<String, Rule> blocks = new HashMap<>();
    static final Map<String, String> beIds = new HashMap<>();
    static final Map<String, Long> blockHits = new TreeMap<>(), beHits = new TreeMap<>(), refHits = new TreeMap<>();

    static void loadRules(Path file) throws IOException {
        for (String raw : Files.readAllLines(file)) {
            String line = raw.replaceAll("#.*", "").trim();
            if (line.isEmpty()) continue;
            if (line.equals("copycatfix")) { copycatFix = true; continue; }
            boolean be = line.startsWith("be ");
            if (be) line = line.substring(3).trim();
            String[] lr = line.split("->");
            String from = lr[0].trim();
            String[] right = lr[1].trim().split("\\s+");
            if (be) { beIds.put(from, right[0]); continue; }
            Map<String, String> set = new LinkedHashMap<>(); Set<String> drop = new HashSet<>(); boolean axis = false;
            for (int i = 1; i < right.length; i++) {
                switch (right[i]) {
                    case "set" -> { for (String kv : right[++i].split(",")) { String[] p = kv.split("="); set.put(p[0], p[1]); } }
                    case "drop" -> drop.addAll(Arrays.asList(right[++i].split(",")));
                    case "axisFromFacing" -> axis = true;
                    default -> throw new IOException("bad rule token " + right[i] + " in: " + raw);
                }
            }
            blocks.put(from, new Rule(right[0], set, drop, axis));
        }
    }

    /** Applies a block rule to a {Name, Properties} compound in place. Returns true if it changed. */
    static boolean applyState(Tag state) {
        Tag name = state.map().get("Name");
        if (name == null || name.type != 8) return false;
        Rule r = blocks.get(name.str());
        if (r == null) return false;
        name.v = r.to();
        Tag props = state.map().get("Properties");
        if (props == null) { props = new Tag((byte) 10, new LinkedHashMap<String, Tag>()); }
        LinkedHashMap<String, Tag> pm = props.map();
        if (r.axisFromFacing() && pm.containsKey("facing")) {
            String f = pm.remove("facing").str();
            pm.put("axis", new Tag((byte) 8, f.equals("up") || f.equals("down") ? "y" : f.equals("north") || f.equals("south") ? "z" : "x"));
        }
        for (String d : r.drop()) pm.remove(d);
        r.set().forEach((k, v) -> pm.put(k, new Tag((byte) 8, v)));
        if (r.to().equals("minecraft:air") || pm.isEmpty()) state.map().remove("Properties");
        else state.map().put("Properties", props);
        return true;
    }

    /** Copycat materials ({Name, Properties} compounds) and item ids inside block entity NBT. */
    static boolean applyRefs(Tag t, String key) {
        boolean changed = false;
        if (t.type == 10) {
            LinkedHashMap<String, Tag> m = t.map();
            if (m.containsKey("Name") && m.get("Name").type == 8 && blocks.containsKey(m.get("Name").str())) {
                refHits.merge("material " + m.get("Name").str(), 1L, Long::sum);
                changed |= applyState(t);
            }
            if (m.containsKey("id") && m.get("id").type == 8 && !key.isEmpty() && blocks.containsKey(m.get("id").str())) {
                String old = m.get("id").str();
                String to = blocks.get(old).to();
                m.get("id").v = to.equals("minecraft:air") ? "minecraft:air" : to;
                refHits.merge("item " + old, 1L, Long::sum);
                changed = true;
            }
            for (var e : m.entrySet()) if (e.getValue().type == 10 || e.getValue().type == 9) changed |= applyRefs(e.getValue(), e.getKey());
        } else if (t.type == 9) {
            for (Tag e : t.list()) changed |= applyRefs(e, key);
        }
        return changed;
    }

    // ---------- copycats saved with Create's block entity id ----------
    static boolean copycatFix;
    static final Map<String, List<String>> MULTI_PARTS = Map.of(
            "copycats:copycat_slab", List.of("bottom", "top"),
            "copycats:copycat_byte", List.of("bottom_northeast", "bottom_northwest", "bottom_southeast", "bottom_southwest",
                    "top_northeast", "top_northwest", "top_southeast", "top_southwest"),
            "copycats:copycat_board", List.of("down", "east", "north", "south", "up", "west"),
            "copycats:copycat_half_layer", List.of("negative_layers", "positive_layers"));

    /**
     * Old Copycats+ saved some blocks with Create's "create:copycat" block entity; 1.21 Create refuses it on a
     * Copycats+ block. Single-material blocks get "copycats:copycat" (same data). Multi-material blocks get
     * "copycats:multistate_copycat" with the old material in every filled part and the copycat base elsewhere.
     */
    static boolean fixCopycat(Tag be, Tag sections) {
        LinkedHashMap<String, Tag> m = be.map();
        Tag state = stateAt(sections, (Integer) m.get("x").v, (Integer) m.get("y").v, (Integer) m.get("z").v);
        if (state == null) return false;
        String block = state.map().get("Name").str();
        if (!block.startsWith("copycats:")) return false;
        List<String> parts = MULTI_PARTS.get(block);
        if (parts == null) {
            m.get("id").v = "copycats:copycat";
            beHits.merge("create:copycat -> copycats:copycat (" + block + ")", 1L, Long::sum);
            return true;
        }
        Map<String, String> props = new HashMap<>();
        Tag p = state.map().get("Properties");
        if (p != null) p.map().forEach((k, v) -> props.put(k, v.str()));
        Tag material = m.get("Material");
        Tag item = m.get("Item");
        byte enableCT = m.get("EnableCT") != null ? (Byte) m.get("EnableCT").v : 1;
        LinkedHashMap<String, Tag> data = new LinkedHashMap<>();
        for (String part : parts) {
            boolean filled = switch (block) {
                case "copycats:copycat_slab" -> {
                    String type = props.getOrDefault("type", "bottom");
                    yield type.equals("double") || type.equals(part);
                }
                case "copycats:copycat_half_layer" -> !props.getOrDefault(part, "0").equals("0");
                default -> "true".equals(props.get(part));
            };
            LinkedHashMap<String, Tag> partTag = new LinkedHashMap<>();
            LinkedHashMap<String, Tag> consumed = new LinkedHashMap<>();
            consumed.put("Count", new Tag((byte) 1, (byte) 1));
            consumed.put("id", new Tag((byte) 8, filled && item != null && item.map().get("id") != null ? item.map().get("id").str() : "minecraft:air"));
            consumed.put("tag", new Tag((byte) 10, new LinkedHashMap<String, Tag>()));
            partTag.put("consumedItem", new Tag((byte) 10, consumed));
            partTag.put("enableCT", new Tag((byte) 1, enableCT));
            if (filled && material != null) {
                partTag.put("material", material);
            } else {
                LinkedHashMap<String, Tag> base = new LinkedHashMap<>();
                base.put("Name", new Tag((byte) 8, "create:copycat_base"));
                partTag.put("material", new Tag((byte) 10, base));
            }
            data.put(part, new Tag((byte) 10, partTag));
        }
        m.remove("Material");
        m.remove("Item");
        m.remove("EnableCT");
        m.get("id").v = "copycats:multistate_copycat";
        m.put("material_data", new Tag((byte) 10, data));
        beHits.merge("create:copycat -> copycats:multistate_copycat (" + block + ")", 1L, Long::sum);
        return true;
    }

    static Tag stateAt(Tag sections, int x, int y, int z) {
        if (sections == null) return null;
        int sy = Math.floorDiv(y, 16);
        for (Tag s : sections.list()) {
            Tag yt = s.map().get("Y");
            if (yt == null || (Byte) yt.v != sy) continue;
            Tag bs = s.map().get("block_states");
            if (bs == null) return null;
            List<Tag> palette = bs.map().get("palette").list();
            int v = 0;
            if (palette.size() > 1) {
                long[] data = (long[]) bs.map().get("data").v;
                int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                int per = 64 / bits;
                int idx = (Math.floorMod(y, 16) * 16 + Math.floorMod(z, 16)) * 16 + Math.floorMod(x, 16);
                v = (int) ((data[idx / per] >>> ((idx % per) * bits)) & ((1L << bits) - 1));
            }
            return v < palette.size() ? palette.get(v) : null;
        }
        return null;
    }

    static boolean migrateChunk(Tag chunk) {
        LinkedHashMap<String, Tag> root = chunk.map().containsKey("Level") ? chunk.map().get("Level").map() : chunk.map();
        boolean changed = false;
        Tag sections = root.get("sections");
        if (sections != null) for (Tag s : sections.list()) {
            Tag bs = s.map().get("block_states");
            if (bs == null || !bs.map().containsKey("palette")) continue;
            for (Tag state : bs.map().get("palette").list()) {
                String before = state.map().containsKey("Name") ? state.map().get("Name").str() : null;
                if (applyState(state)) { changed = true; blockHits.merge(before, 1L, Long::sum); }
            }
        }
        Tag bes = root.get("block_entities");
        if (bes != null) {
            Iterator<Tag> it = bes.list().iterator();
            while (it.hasNext()) {
                Tag be = it.next();
                Tag id = be.map().get("id");
                if (id != null && beIds.containsKey(id.str())) {
                    String to = beIds.get(id.str());
                    beHits.merge(id.str() + " -> " + to, 1L, Long::sum);
                    if (to.equals("REMOVE")) it.remove(); else id.v = to;
                    changed = true;
                    if (to.equals("REMOVE")) continue;
                }
                changed |= applyRefs(be, "");
                if (copycatFix && id != null && "create:copycat".equals(id.str())) changed |= fixCopycat(be, sections);
            }
        }
        if (changed && root.containsKey("isLightOn")) root.put("isLightOn", new Tag((byte) 1, (byte) 0));
        return changed;
    }

    public static void main(String[] a) throws Exception {
        loadRules(Path.of(a[0]));
        Path dir = Path.of(a[1]), out = Path.of(a[2]);
        Files.createDirectories(out);
        int files = 0, chunks = 0;
        try (var list = Files.list(dir)) {
            for (Path mca : list.filter(p -> p.toString().endsWith(".mca")).sorted().toList()) {
                int n = migrateRegion(mca, out.resolve(mca.getFileName()));
                if (n > 0) { files++; chunks += n; }
            }
        }
        System.out.println("region files changed: " + files + ", chunks changed: " + chunks);
        System.out.println("PALETTE ENTRIES RENAMED (per section):"); blockHits.forEach((k, v) -> System.out.println("  " + v + "  " + k));
        System.out.println("BLOCK ENTITIES:"); beHits.forEach((k, v) -> System.out.println("  " + v + "  " + k));
        System.out.println("REFERENCES IN BLOCK ENTITIES:"); refHits.forEach((k, v) -> System.out.println("  " + v + "  " + k));
    }

    /** Rewrites a region file with migrated chunks; returns how many chunks changed (0 = file not written). */
    static int migrateRegion(Path src, Path dst) throws IOException {
        byte[] data = Files.readAllBytes(src);
        if (data.length < 8192) return 0;
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
            Tag root = read(dis, rootType);
            if (!migrateChunk(root)) continue;
            ByteArrayOutputStream nbt = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(nbt))) {
                dos.writeByte(rootType); dos.writeUTF(rootName); write(dos, root);
            }
            byte[] payload = nbt.toByteArray();
            ByteArrayOutputStream c = new ByteArrayOutputStream();
            DataOutputStream cd = new DataOutputStream(c);
            cd.writeInt(payload.length + 1); cd.writeByte(2); cd.write(payload);
            chunks[i] = c.toByteArray();
            changed++;
        }
        if (changed == 0) return 0;
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        byte[] header = new byte[8192];
        System.arraycopy(data, 4096, header, 4096, 4096); // timestamps
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int sector = 2;
        for (int i = 0; i < 1024; i++) {
            if (chunks[i] == null) continue;
            int sectors = (chunks[i].length + 4095) / 4096;
            if (sectors > 255) throw new IOException("chunk too large in " + src);
            byte[] padded = Arrays.copyOf(chunks[i], sectors * 4096);
            body.write(padded);
            header[i * 4] = (byte) (sector >> 16); header[i * 4 + 1] = (byte) (sector >> 8); header[i * 4 + 2] = (byte) sector; header[i * 4 + 3] = (byte) sectors;
            sector += sectors;
        }
        file.write(header); file.write(body.toByteArray());
        Files.write(dst, file.toByteArray());
        return changed;
    }
}
