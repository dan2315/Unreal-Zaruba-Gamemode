package com.dod.UnrealZaruba.Gamemodes;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber
public class MesilovoGamemode {

    public static void setupScoreboard(MinecraftServer server) {

        Scoreboard scoreboard = server.getScoreboard();

        Objective defendersObjective = null;
        Objective attackersObjective = null;

        if (scoreboard.getObjective("defendersTimer") == null) {
            defendersObjective = scoreboard.addObjective("defendersTimer", ObjectiveCriteria.AIR,
                    new TextComponent("Защищай цели"), ObjectiveCriteria.RenderType.INTEGER);
        }
        if (scoreboard.getObjective("defendersTimer") == null) {
            attackersObjective = scoreboard.addObjective("attackersScore", ObjectiveCriteria.AIR,
                    new TextComponent("Набери 100 очков"), ObjectiveCriteria.RenderType.INTEGER);
        }

        scoreboard.getOrCreatePlayerScore("Villager", attackersObjective).setScore(30);
        scoreboard.getOrCreatePlayerScore("Container", attackersObjective).setScore(50);
        scoreboard.getOrCreatePlayerScore("Infrastructure", attackersObjective).setScore(10);
    }


}

