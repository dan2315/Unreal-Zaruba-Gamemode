package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;

public class GameTimer {

    private final String OBJECTIVE_NAME = "gameTime";
    private Score minutes;
    private Score seconds;
    private MinecraftServer server;
    private Scoreboard scoreboard;
    private Objective objective;

    public GameTimer(MinecraftServer server) {
        this.server = server;
        this.scoreboard = server.getScoreboard();
        UnrealZaruba.LOGGER.info("Scoreboard: " + scoreboard);
    }

    public void setupScoreboard() {
        objective = scoreboard.getObjective(OBJECTIVE_NAME);
        
        if (objective == null) {
            objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal("Game Time"), ObjectiveCriteria.RenderType.INTEGER);
        }

        minutes = scoreboard.getOrCreatePlayerScore("Minutes", objective);
        seconds = scoreboard.getOrCreatePlayerScore("Seconds", objective);
        setDisplaySlot();
    }

    private void setDisplaySlot() {
        scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
    }

    public void updateScoreboardTimerMinutes(int timeLeft) {
        minutes.setScore(timeLeft);
    }

    public void updateScoreboardTimerSeconds(int timeLeft) {
        seconds.setScore(timeLeft);
    }

    public void resetScoreboard() {
        if (objective != null) {
            scoreboard.resetPlayerScore("Minutes", objective);
            scoreboard.resetPlayerScore("Seconds", objective);
            scoreboard.removeObjective(objective);
            objective = null;
        }
    }
}
