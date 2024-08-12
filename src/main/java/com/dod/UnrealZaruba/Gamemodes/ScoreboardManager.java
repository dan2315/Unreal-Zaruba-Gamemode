package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.UnrealZaruba;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;

public class ScoreboardManager {

    static final String OBJECTIVE_NAME = "gameTime";
    static Score minutes;
    static Score seconds;

    public static void setupScoreboard(MinecraftServer server, int gameDuration) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, new TextComponent("Game Time"), ObjectiveCriteria.RenderType.INTEGER);
        }

        minutes = scoreboard.getOrCreatePlayerScore("Minutes", objective);
        seconds = scoreboard.getOrCreatePlayerScore("Seconds", objective);
        setDisplaySlot(scoreboard, objective);
    }

    private static void setDisplaySlot(Scoreboard scoreboard, Objective objective) {
        scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
    }

    public static void UpdateScoreboardTimerMinutes(Scoreboard scoreboard, Objective objective, int timeleft){
        minutes.setScore(timeleft);
    }

    public static void UpdateScoreboardTimerSeconds(Scoreboard scoreboard, Objective objective, int timeleft){
        seconds.setScore(timeleft);
    }

    public static void clearScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective != null) {
            scoreboard.resetPlayerScore("Minutes", objective);
            scoreboard.resetPlayerScore("Seconds", objective);
            scoreboard.removeObjective(objective);
        }
    }
}
