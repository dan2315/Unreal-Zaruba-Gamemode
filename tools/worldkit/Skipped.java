import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * What an additive build (generators/Canvas) didn't place: compares each fill's volume with the "Successfully filled N"
 * replies of all its filter passes, and lists the boxes that came up short, biggest first, with a total per block.
 * A block counts as skipped when something non-replaceable was already there (a tree, an earlier build, the same
 * block from a previous run). Probe the listed boxes to see what's in the way; --clear on the generator only removes
 * what it lists as clears.
 * Usage: java Skipped <commands.txt> <out.txt from runcmds.sh>
 */
public class Skipped {
    static final Pattern FILL = Pattern.compile("fill (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (\\S+?)(?: replace \\S+)?$");
    static final Pattern FILLED = Pattern.compile("Successfully filled (\\d+)");

    public static void main(String[] a) throws Exception {
        List<String> cmds = Files.readAllLines(Path.of(a[0]));
        Map<Integer, Integer> filled = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(a[1]))) {
            int bar = line.indexOf('|');
            if (bar < 0) continue;
            Matcher m = FILLED.matcher(line);
            filled.put(Integer.parseInt(line.substring(0, bar).trim()), m.find() ? Integer.parseInt(m.group(1)) : 0);
        }
        // fill boxes in command order, summing the passes with different filters
        LinkedHashMap<String, int[]> boxes = new LinkedHashMap<>();   // key -> {volume, filled}
        for (int i = 0; i < cmds.size(); i++) {
            Matcher m = FILL.matcher(cmds.get(i).trim());
            if (!m.find() || m.group(7).equals("minecraft:air")) continue;
            int[] v = new int[6];
            for (int j = 0; j < 6; j++) v[j] = Integer.parseInt(m.group(j + 1));
            int volume = (Math.abs(v[3] - v[0]) + 1) * (Math.abs(v[4] - v[1]) + 1) * (Math.abs(v[5] - v[2]) + 1);
            String key = v[0] + " " + v[1] + " " + v[2] + " .. " + v[3] + " " + v[4] + " " + v[5] + "  " + m.group(7);
            int[] box = boxes.computeIfAbsent(key, k -> new int[] {volume, 0});
            box[1] += filled.getOrDefault(i + 1, 0);
        }
        int total = 0, missing = 0;
        Map<String, Integer> perBlock = new TreeMap<>();
        List<Map.Entry<String, int[]>> short_ = new ArrayList<>();
        for (var e : boxes.entrySet()) {
            total += e.getValue()[0];
            int miss = e.getValue()[0] - e.getValue()[1];
            if (miss <= 0) continue;
            missing += miss;
            short_.add(e);
            String block = e.getKey().substring(e.getKey().lastIndexOf(' ') + 1).replaceAll("[\\[{].*", "");
            perBlock.merge(block, miss, Integer::sum);
        }
        System.out.println("placed " + (total - missing) + " of " + total + " blocks, skipped " + missing);
        perBlock.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue())
                .forEach(e -> System.out.println("  " + e.getValue() + "  " + e.getKey()));
        short_.sort((x, y) -> (y.getValue()[0] - y.getValue()[1]) - (x.getValue()[0] - x.getValue()[1]));
        System.out.println("boxes with skipped blocks (missing/volume):");
        short_.stream().limit(40).forEach(e -> System.out.println("  " + (e.getValue()[0] - e.getValue()[1]) + "/" + e.getValue()[0] + "  " + e.getKey()));
        if (short_.size() > 40) System.out.println("  ... " + (short_.size() - 40) + " more");
    }
}
