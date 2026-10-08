import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.google.gson.*;

/**
 * Property names and values of blocks, read from blockstate JSON in mod jars (variants keys and multipart "when").
 * Usage: java StateProps <mods dir> <id> [<id> ...]   (ids like dndecor:green_container)
 */
public class StateProps {
    public static void main(String[] a) throws Exception {
        Map<String, String> wanted = new LinkedHashMap<>();
        for (int i = 1; i < a.length; i++) {
            String[] p = a[i].split(":");
            wanted.put("assets/" + p[0] + "/blockstates/" + p[1] + ".json", a[i]);
        }
        Map<String, String> found = new HashMap<>();
        try (var files = Files.list(Path.of(a[0]))) {
            for (Path jar : files.filter(p -> p.toString().endsWith(".jar")).toList()) scanJar(new FileInputStream(jar.toFile()), wanted, found, 0);
        }
        wanted.forEach((path, id) -> System.out.println(id + "  " + found.getOrDefault(path, "<no blockstate>")));
    }

    static void scanJar(InputStream in, Map<String, String> wanted, Map<String, String> found, int depth) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
            String n = e.getName();
            if (wanted.containsKey(n)) found.put(n, props(new String(zip.readAllBytes())));
            else if (depth == 0 && n.startsWith("META-INF/jarjar/") && n.endsWith(".jar")) scanJar(new ByteArrayInputStream(zip.readAllBytes()), wanted, found, 1);
        }
    }

    static String props(String json) {
        Map<String, Set<String>> props = new TreeMap<>();
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        if (o.has("variants")) {
            for (String key : o.getAsJsonObject("variants").keySet()) add(props, key);
        }
        if (o.has("multipart")) {
            for (JsonElement part : o.getAsJsonArray("multipart")) {
                JsonObject p = part.getAsJsonObject();
                if (p.has("when")) when(props, p.get("when"));
            }
        }
        return props.toString();
    }

    static void when(Map<String, Set<String>> props, JsonElement w) {
        JsonObject o = w.getAsJsonObject();
        for (String k : o.keySet()) {
            if (k.equals("OR") || k.equals("AND")) { for (JsonElement x : o.getAsJsonArray(k)) when(props, x); continue; }
            for (String v : o.get(k).getAsString().split("\\|")) props.computeIfAbsent(k, x -> new TreeSet<>()).add(v);
        }
    }

    static void add(Map<String, Set<String>> props, String key) {
        if (key.isEmpty()) return;
        for (String kv : key.split(",")) {
            String[] p = kv.split("=");
            if (p.length == 2) props.computeIfAbsent(p[0], x -> new TreeSet<>()).add(p[1]);
        }
    }
}
