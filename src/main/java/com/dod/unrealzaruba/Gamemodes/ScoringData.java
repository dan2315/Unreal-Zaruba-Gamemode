package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Gamemodes.GamemodeData.AbstractGamemodeData;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;

/**
 * Per-map scoring rules (scoring.json in the gamemode's data folder). A map without the file gets the defaults written
 * out, and an existing file is written back so settings added later show up in it.
 */
public class ScoringData extends AbstractGamemodeData<ScoringData.Config> {
    private static final String DATA_NAME = "scoring";

    public static class Config {
        /** Score a team has to reach to win. */
        public int pointsToWin = 200;
        /** Capture points each team holds from the start; they earn nothing, only the points above this number do. */
        public int defaultPointsPerTeam = 2;
        /** Score a team earns per half second for every point it holds above defaultPointsPerTeam (the score ticks twice a second). */
        public float scorePerExtraPoint = 0.1f;
        /** How long players have to pick a team at the start of the match. */
        public int teamSelectionSeconds = 60;
        /** Seconds one player alone needs to capture a point (from the first tick of the capture to the end). */
        public float captureSeconds = 30f;
        /** Every extra player on the point multiplies the speed by this: 2 means two players take half the time, three a quarter, and so on. */
        public float extraCapturerSpeedFactor = 2f;
        /** The speed is never more than this many times that of one player. */
        public float maxCaptureSpeedMultiplier = 16f;
        /** The owner standing alone on the point takes back the capture at this share of the capture speed (the point itself is held anyway). */
        public float ownerRestoreShare = 0.5f;
        /** A point nobody stands on loses the capture progress at this share of the speed of one capturer. */
        public float emptyDecayShare = 0.25f;
        /** The shares of the score needed to win at which everybody is told which team got there first (once per mark). */
        public int[] announceScoreAtPercent = {50, 90};
    }

    public ScoringData(Class<? extends BaseGamemode> gamemodeClass) {
        super(Config.class, gamemodeClass, DATA_NAME, new Config());
    }

    @Override
    public Config loadData() {
        Config config = super.loadData();
        config.pointsToWin = Math.max(1, config.pointsToWin);
        config.defaultPointsPerTeam = Math.max(0, config.defaultPointsPerTeam);
        config.teamSelectionSeconds = Math.max(0, config.teamSelectionSeconds);
        config.captureSeconds = Math.max(1f, config.captureSeconds);
        config.extraCapturerSpeedFactor = Math.max(1f, config.extraCapturerSpeedFactor);
        config.maxCaptureSpeedMultiplier = Math.max(1f, config.maxCaptureSpeedMultiplier);
        saveData();
        return config;
    }

    /** The rules of a gamemode, or the defaults when it has none loaded. */
    public static Config of(Class<? extends BaseGamemode> gamemodeClass) {
        ScoringData handler = GamemodeDataManager.getHandler(gamemodeClass, ScoringData.class);
        return handler != null && handler.getData() != null ? handler.getData() : new Config();
    }
}
