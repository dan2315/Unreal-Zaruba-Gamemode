package com.dod.unrealzaruba.WorldManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.CapturePointsGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeFactory;
import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * The maps matches are played on. A map is a world template in game_worlds/&lt;key&gt;/ played in one mode; several maps
 * may share a mode. Its own settings (objectives, team bases, barriers, vehicle spawns, supply drop area) are in its
 * data folder, the mode's (economy, scoring) stay in unrealzaruba/gamemodedata/&lt;ModeClass&gt;/.
 *
 * <p>Maps made with /newmap have unrealzaruba/maps/&lt;key&gt;/map.json (mode, name, description) and keep their settings
 * next to it. The first map of each mode is its old template, named after the mode class (capturepointsgamemode): its
 * settings stay in gamemodedata/&lt;ModeClass&gt;/, where the admin panel edits them, and a map.json in maps/&lt;key&gt;/ only
 * gives it a name.
 */
public final class GameMaps {
    public static final Path TEMPLATES = Path.of("game_worlds");
    public static final Path MAPS = Path.of("unrealzaruba", "maps");
    private static final Path MODE_DATA = Path.of("unrealzaruba", "gamemodedata");
    public static final Pattern KEY = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** map.json */
    public static class Info {
        public String mode;
        public String name = "";
        public String description = "";
    }

    /** @param legacy the mode's old template: its settings are in the mode's gamemodedata folder */
    public record GameMap(String key, String mode, String name, String description, boolean legacy) {
        public Path dataDir() {
            Class<? extends BaseGamemode> modeClass = GamemodeFactory.classes.get(mode);
            return legacy && modeClass != null ? MODE_DATA.resolve(modeClass.getSimpleName()) : MAPS.resolve(key);
        }
    }

    private static volatile String selected;

    private GameMaps() {}

    /** The key of a mode's old template (the mode class name in lowercase). */
    public static String legacyKey(String mode) {
        Class<? extends BaseGamemode> modeClass = GamemodeFactory.classes.get(mode);
        return modeClass == null ? null : modeClass.getSimpleName().toLowerCase();
    }

    /** All maps there are templates for, sorted by mode and key. */
    public static List<GameMap> all() {
        List<GameMap> maps = new ArrayList<>();
        if (!Files.isDirectory(TEMPLATES)) return maps;
        try (Stream<Path> dirs = Files.list(TEMPLATES)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                GameMap map = read(dir.getFileName().toString());
                if (map != null) maps.add(map);
            }
        } catch (IOException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not list the maps in {}", TEMPLATES.toAbsolutePath(), e);
        }
        maps.sort(Comparator.comparing(GameMap::mode).thenComparing(map -> !map.legacy()).thenComparing(GameMap::key));
        return maps;
    }

    public static GameMap get(String key) {
        if (key == null || !KEY.matcher(key).matches() || !Files.isDirectory(TEMPLATES.resolve(key))) return null;
        return read(key);
    }

    /** A map by its key, or by a mode id for that mode's first map (so /openmap capturepoints keeps working). */
    public static GameMap find(String keyOrMode) {
        if (keyOrMode == null) return null;
        String key = keyOrMode.toLowerCase();
        GameMap map = get(key);
        return map != null ? map : firstOf(key);
    }

    /** A mode's first map: its old template when there is one. */
    public static GameMap firstOf(String mode) {
        return all().stream().filter(map -> map.mode().equals(mode)).findFirst().orElse(null);
    }

    private static GameMap read(String key) {
        if (!KEY.matcher(key).matches()) return null;
        Info info = readInfo(key);
        String legacyMode = GamemodeFactory.classes.keySet().stream().filter(mode -> key.equals(legacyKey(mode))).findFirst().orElse(null);
        if (legacyMode != null) {
            return new GameMap(key, legacyMode, info == null ? "" : info.name, info == null ? "" : info.description, true);
        }
        if (info == null || info.mode == null || !GamemodeFactory.classes.containsKey(info.mode)) return null;
        return new GameMap(key, info.mode, info.name == null ? "" : info.name, info.description == null ? "" : info.description, false);
    }

    private static Info readInfo(String key) {
        Path file = MAPS.resolve(key).resolve("map.json");
        if (!Files.exists(file)) return null;
        try {
            return GSON.fromJson(Files.readString(file), Info.class);
        } catch (IOException | JsonParseException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Could not read {}", file.toAbsolutePath(), e);
            return null;
        }
    }

    public static void writeInfo(String key, Info info) throws IOException {
        Files.createDirectories(MAPS.resolve(key));
        Files.writeString(MAPS.resolve(key).resolve("map.json"), GSON.toJson(info));
    }

    /** The map the next or running match is on, or null before one was chosen. */
    public static GameMap selected() {
        return get(selected);
    }

    public static void select(String key) {
        selected = key;
    }

    /**
     * The map a new match of this mode is on: the selected one when it is of this mode, else the mode's first map
     * (which then becomes the selected one). Null when the mode has no map at all.
     */
    public static GameMap selectFor(String mode) {
        GameMap current = selected();
        if (current != null && current.mode().equals(mode)) return current;
        GameMap first = firstOf(mode);
        selected = first == null ? legacyKey(mode) : first.key();
        return first;
    }

    /**
     * Makes a new map: a copy of {@code from}'s world and, when it is of the same mode, its map settings (not the mode's
     * economy and scoring), or an empty world. Blocking file work: run it off the server thread, after saving
     * {@code from} if it is open in DEV mode.
     */
    public static GameMap create(String key, String mode, GameMap from) throws IOException {
        Path world = TEMPLATES.resolve(key);
        Files.createDirectories(MAPS.resolve(key));
        if (from == null) {
            Files.createDirectories(world);
        } else {
            WorldManager.copyRecursively(TEMPLATES.resolve(from.key()), world);
            if (from.mode().equals(mode) && Files.isDirectory(from.dataDir())) {
                try (Stream<Path> files = Files.list(from.dataDir())) {
                    for (Path file : files.filter(GameMaps::isMapSetting).toList()) {
                        Files.copy(file, MAPS.resolve(key).resolve(file.getFileName().toString()));
                    }
                }
            }
        }
        Info info = new Info();
        info.mode = mode;
        info.name = key;
        writeInfo(key, info);
        return get(key);
    }

    /** A settings file that belongs to the map: not the mode's economy or scoring, not map.json, not a backup. */
    private static boolean isMapSetting(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".json") && !name.equals("economy.json") && !name.equals("scoring.json") && !name.equals("map.json");
    }

    /** The map voting falls back to when nobody voted. */
    public static GameMap fallback() {
        GameMap map = firstOf(CapturePointsGamemode.GAMEMODE_NAME);
        if (map != null) return map;
        List<GameMap> maps = all();
        return maps.isEmpty() ? null : maps.get(0);
    }
}
