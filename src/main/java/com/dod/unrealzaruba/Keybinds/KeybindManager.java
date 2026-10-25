package com.dod.unrealzaruba.Keybinds;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * Hands a key binding preset to players without taking their own choices away. The preset is
 * config/unrealfrontiers/keybinds.json (published with the modpack); what was applied to this player is remembered in
 * unrealzaruba/keybinds-applied.json next to the game, and never published.
 *
 * <p>A binding of the preset is applied when the player has not touched it: it still has its default key (and we have
 * not set it before), or it still has the key we set last time. A binding the player changed is left alone, unless
 * the preset marks it {@code "force": N} with an N this player has not had applied yet.
 */
public final class KeybindManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** One binding of the preset. {@code modifier}: NONE, CONTROL, SHIFT or ALT. {@code force}: see the class comment. */
    public static final class Bind {
        public String key;
        public String modifier = "NONE";
        public Integer force;
    }

    /**
     * Values of a mod's JSON config file (config/{file}, object at {path}, e.g. Jade's overlay position), handed out by
     * the same rules as the binds. {@code defaults} are the values that count as untouched when we have not set them yet.
     */
    public static final class ConfigEdit {
        public String file;
        public String path;
        public JsonObject values;
        public JsonObject defaults;
        public Integer force;
    }

    public static final class Preset {
        public int version = 1;
        public Map<String, Bind> binds = new LinkedHashMap<>();
        public Map<String, ConfigEdit> configs = new LinkedHashMap<>();
    }

    /** What this player was given: the key signature last applied and the highest force number applied. */
    static final class Applied {
        String signature;
        int force;
        /** What it was before we set it: if the game did not save our change, it is still this at the next start. */
        String previous;
    }

    static final class State {
        Map<String, Applied> binds = new HashMap<>();
        Map<String, Applied> configs = new HashMap<>();
        Map<String, Applied> options = new HashMap<>();
    }

    private KeybindManager() {}

    public static Path presetPath() {
        return FMLPaths.CONFIGDIR.get().resolve("unrealfrontiers").resolve("keybinds.json");
    }

    private static Path statePath() {
        return FMLPaths.GAMEDIR.get().resolve("unrealzaruba").resolve("keybinds-applied.json");
    }

    /** Only the settings of mod config files (Jade and the like), for when those mods have surely loaded their files. */
    public static String applyConfigsOnly() {
        Preset preset = readPreset();
        if (preset == null) return "no preset";
        State state = readState();
        String report = applyConfigs(preset, state, false, FMLPaths.CONFIGDIR.get());
        writeState(state);
        return report;
    }

    /** Applies the preset the way the class comment describes. Returns a short report for the player. */
    public static String apply(boolean forceAll) {
        Preset preset = readPreset();
        if (preset == null) return "No key binding preset found at " + presetPath();
        KeyMapping[] mappings = Minecraft.getInstance().options.keyMappings;
        State state = readState();

        Map<String, KeyMapping> byName = new HashMap<>();
        for (KeyMapping mapping : mappings) byName.put(mapping.getName(), mapping);

        int applied = 0, kept = 0, missing = 0, conflicts = 0;
        for (var entry : preset.binds.entrySet()) {
            KeyMapping mapping = byName.get(entry.getKey());
            Bind bind = entry.getValue();
            if (mapping == null || bind == null || bind.key == null) {
                missing++;
                continue;
            }
            InputConstants.Key key;
            KeyModifier modifier;
            try {
                key = InputConstants.getKey(bind.key);
                modifier = KeyModifier.valueOf(bind.modifier == null ? "NONE" : bind.modifier.toUpperCase());
            } catch (RuntimeException e) {
                UnrealZaruba.LOGGER.warn("[Keybinds] {} has an unreadable key '{}' in the preset", entry.getKey(), bind.key);
                missing++;
                continue;
            }
            String wanted = signature(key, modifier);
            String current = signature(mapping.getKey(), mapping.getKeyModifier());
            Applied record = state.binds.get(entry.getKey());

            boolean forced = forceAll || (bind.force != null && (record == null || record.force < bind.force));
            boolean untouched = record != null ? (current.equals(record.signature) || current.equals(record.previous)) : mapping.isDefault();

            if (current.equals(wanted)) {
                remember(state, entry.getKey(), wanted, bind.force);
                continue;
            }
            if (!forced && !untouched) {
                kept++; // the player picked this one
                continue;
            }
            if (!forced && ownedByPlayer(mappings, mapping, key, modifier, preset)) {
                conflicts++;
                UnrealZaruba.LOGGER.info("[Keybinds] {} not set to {}: the player uses that key for something else", entry.getKey(), wanted);
                continue;
            }
            mapping.setKeyModifierAndCode(modifier, key);
            remember(state, entry.getKey(), wanted, bind.force);
            state.binds.get(entry.getKey()).previous = current;
            applied++;
        }

        KeyMapping.resetMapping();
        applyOptionDefaults(state, forceAll);
        Minecraft.getInstance().options.save();
        String configReport = applyConfigs(preset, state, forceAll, FMLPaths.CONFIGDIR.get());
        writeState(state);
        return configReport + "; key bindings: " + applied + " set, " + kept + " kept as the player has them, " + conflicts + " skipped for a conflict, "
                + missing + " not found";
    }

    /**
     * Video options the modpack starts a player with, set once per game folder: a player who changes one back keeps it. VSync is off,
     * as with this many mods it stutters whenever a frame misses the screen's refresh. The game's options are saved right after.
     */
    private static void applyOptionDefaults(State state, boolean forceAll) {
        if (state.options == null) state.options = new HashMap<>();
        if (forceAll || !state.options.containsKey("enableVsync")) {
            Minecraft.getInstance().options.enableVsync().set(false);
            Applied record = new Applied();
            record.signature = "false";
            state.options.put("enableVsync", record);
            UnrealZaruba.LOGGER.info("[Keybinds] VSync turned off (the modpack's default)");
        }
    }

    static String applyConfigs(Preset preset, State state, boolean forceAll, Path configDir) {
        int applied = 0, kept = 0, missing = 0;
        if (preset.configs == null) return "configs: none";
        for (var entry : preset.configs.entrySet()) {
            ConfigEdit edit = entry.getValue();
            if (edit == null || edit.file == null || edit.values == null) continue;
            Path file = configDir.resolve(edit.file);
            UnrealZaruba.LOGGER.info("[Keybinds] settings {}: {}", entry.getKey(), file);
            if (!Files.isRegularFile(file)) {
                missing++; // the mod has not written its config yet: it is handled on a later start
                continue;
            }
            try {
                JsonObject root;
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    root = GSON.fromJson(reader, JsonObject.class);
                }
                JsonObject target = root;
                if (edit.path != null && !edit.path.isEmpty()) {
                    for (String part : edit.path.split("[.]")) {
                        JsonElement child = target.get(part);
                        if (child == null || !child.isJsonObject()) {
                            child = new JsonObject();
                            target.add(part, child);
                        }
                        target = child.getAsJsonObject();
                    }
                }
                String wanted = valuesSignature(edit.values, edit.values);
                String current = valuesSignature(edit.values, target);
                Applied record = state.configs.get(entry.getKey());
                boolean forced = forceAll || (edit.force != null && (record == null || record.force < edit.force));
                boolean untouched = record != null ? current.equals(record.signature)
                        : edit.defaults != null && current.equals(valuesSignature(edit.values, edit.defaults));
                if (record != null && current.equals(record.previous)) untouched = true;
                UnrealZaruba.LOGGER.info("[Keybinds] settings {}: now {} wanted {} forced {} untouched {}", entry.getKey(), current, wanted, forced, untouched);
                if (current.equals(wanted)) {
                    rememberConfig(state, entry.getKey(), wanted, edit.force);
                    continue;
                }
                if (!forced && !untouched) {
                    kept++;
                    continue;
                }
                for (var value : edit.values.entrySet()) target.add(value.getKey(), value.getValue());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(root, writer);
                }
                JadeLive.apply(edit.file, edit.values);
                rememberConfig(state, entry.getKey(), wanted, edit.force);
                state.configs.get(entry.getKey()).previous = current;
                applied++;
            } catch (IOException | RuntimeException e) {
                UnrealZaruba.LOGGER.warn("[Keybinds] Could not apply the settings {} to {}: {}", entry.getKey(), edit.file, e.toString(), e);
            }
        }
        return "configs: " + applied + " set, " + kept + " kept, " + missing + " waiting for the mod's file";
    }

    private static void rememberConfig(State state, String name, String signature, Integer force) {
        Applied record = state.configs.computeIfAbsent(name, key -> new Applied());
        record.signature = signature;
        if (force != null) record.force = Math.max(record.force, force);
    }

    /** The keys of {@code keys} as read from {@code source}, in a form that compares numbers by value. */
    private static String valuesSignature(JsonObject keys, JsonObject source) {
        StringBuilder builder = new StringBuilder();
        for (String key : keys.keySet()) {
            JsonElement value = source.get(key);
            builder.append(key).append('=');
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                builder.append(String.format(java.util.Locale.ROOT, "%.3f", value.getAsDouble()));
            } else {
                builder.append(value);
            }
            builder.append(';');
        }
        return builder.toString();
    }

    /** Writes the current key bindings of this game as a preset; force marks of an existing preset are kept. */
    public static String export() {
        Preset previous = readPreset();
        Preset preset = new Preset();
        preset.version = previous == null ? 1 : previous.version + 1;
        if (previous != null && previous.configs != null) preset.configs = previous.configs; // settings are written by hand
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            Bind bind = new Bind();
            bind.key = mapping.getKey().getName();
            bind.modifier = mapping.getKeyModifier().name();
            Bind old = previous == null ? null : previous.binds.get(mapping.getName());
            if (old != null) bind.force = old.force;
            preset.binds.put(mapping.getName(), bind);
        }
        try {
            Path path = presetPath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(preset, writer);
            }
            return "Saved " + preset.binds.size() + " key bindings (preset version " + preset.version + ") to " + path;
        } catch (IOException e) {
            return "Could not write the preset: " + e.getMessage();
        }
    }

    /** Another binding that holds this key and belongs to the player: it is not in the preset, or the preset leaves it. */
    private static boolean ownedByPlayer(KeyMapping[] mappings, KeyMapping target, InputConstants.Key key, KeyModifier modifier,
                                         Preset preset) {
        for (KeyMapping other : mappings) {
            if (other == target || preset.binds.containsKey(other.getName())) continue;
            if (other.getKey().equals(key) && other.getKeyModifier() == modifier && !other.isUnbound()) return true;
        }
        return false;
    }

    private static void remember(State state, String name, String signature, Integer force) {
        Applied record = state.binds.computeIfAbsent(name, key -> new Applied());
        record.signature = signature;
        if (force != null) record.force = Math.max(record.force, force);
    }

    private static String signature(InputConstants.Key key, KeyModifier modifier) {
        return key.getName() + "|" + modifier.name();
    }

    private static Preset readPreset() {
        Path path = presetPath();
        if (!Files.isRegularFile(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Preset preset = GSON.fromJson(reader, Preset.class);
            return preset != null && preset.binds != null ? preset : null;
        } catch (IOException | RuntimeException e) {
            UnrealZaruba.LOGGER.warn("[Keybinds] Could not read {}: {}", path, e.toString());
            return null;
        }
    }

    private static State readState() {
        Path path = statePath();
        if (!Files.isRegularFile(path)) return new State();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            State state = GSON.fromJson(reader, State.class);
            return state != null && state.binds != null ? state : new State();
        } catch (IOException | RuntimeException e) {
            return new State();
        }
    }

    private static void writeState(State state) {
        try {
            Path path = statePath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[Keybinds] Could not remember what was applied: {}", e.toString());
        }
    }
}
