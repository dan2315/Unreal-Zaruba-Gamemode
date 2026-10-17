package com.dod.unrealzaruba.Keybinds;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

class KeybindConfigTests {
    private static final String PRESET = "{\"version\":2,\"configs\":{\"jade\":{\"file\":\"jade/jade.json\",\"path\":\"overlay\","
            + "\"values\":{\"overlayPosX\":1.0,\"overlayPosY\":0.0},\"defaults\":{\"overlayPosX\":0.5,\"overlayPosY\":1.0},\"force\":1}}}";
    private static final String JADE = "{\"general\":{\"a\":true},\"overlay\":{\"alpha\":0.7,\"overlayPosX\":0.5,\"overlayPosY\":1.0}}";

    private static Path jade(Path dir, String content) throws Exception {
        Path file = dir.resolve("jade").resolve("jade.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    void untouchedFileGetsThePreset(@TempDir Path dir) throws Exception {
        Path file = jade(dir, JADE);
        var preset = new Gson().fromJson(PRESET, KeybindManager.Preset.class);
        String report = KeybindManager.applyConfigs(preset, new KeybindManager.State(), false, dir);
        String written = Files.readString(file);
        assertTrue(written.contains("\"overlayPosX\": 1.0"), report + " / " + written);
        assertTrue(written.contains("\"alpha\": 0.7"), written);
    }

    @Test
    void changedByThePlayerIsKeptUnlessForced(@TempDir Path dir) throws Exception {
        Path file = jade(dir, JADE.replace("0.5", "0.25"));
        var preset = new Gson().fromJson(PRESET.replace(",\"force\":1", ""), KeybindManager.Preset.class);
        KeybindManager.applyConfigs(preset, new KeybindManager.State(), false, dir);
        assertTrue(Files.readString(file).contains("0.25"));
        var forced = new Gson().fromJson(PRESET, KeybindManager.Preset.class);
        KeybindManager.applyConfigs(forced, new KeybindManager.State(), false, dir);
        assertTrue(Files.readString(file).contains("\"overlayPosX\": 1.0"));
    }
}
