package com.dod.UnrealZaruba.Gamemodes;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Objective;

public class ScoreboardManager {

    static final String OBJECTIVE_NAME = "gameTime";

    public static void setupScoreboard(MinecraftServer server, int gameDuration) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, new TextComponent("Game Time"), ObjectiveCriteria.RenderType.INTEGER);
        }

        scoreboard.getOrCreatePlayerScore("Minutes", objective).setScore(gameDuration);
        scoreboard.getOrCreatePlayerScore("Seconds", objective).setScore(gameDuration);
        setDisplaySlot(scoreboard, objective);
    }

    private static void setDisplaySlot(Scoreboard scoreboard, Objective objective) {
        scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
    }

    public static void UpdateScoreboardTimerMinutes(Scoreboard scoreboard, Objective objective, int timeleft){
        scoreboard.getOrCreatePlayerScore("Minutes", objective).setScore(timeleft);
    }

    public static void UpdateScoreboardTimerSeconds(Scoreboard scoreboard, Objective objective, int timeleft){
        scoreboard.getOrCreatePlayerScore("Seconds", objective).setScore(timeleft);
    }
}
