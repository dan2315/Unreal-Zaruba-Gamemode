package com.dod.UnrealZaruba.TeamLogic;

import java.util.HashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class TeamManager {
    static HashMap<DyeColor, Team> teams = new HashMap<>();

    private static Team defenders = new Team(null);
    private static Team attackers = new Team(null);

    public static void Initialize() {
        teams.put(DyeColor.RED, attackers);
        teams.put(DyeColor.BLUE, defenders);
    }

    static {
        Initialize();
    }

    public static void SetSpawn(DyeColor color, BlockPos spawn) {
        teams.get(color).SetSpawn(spawn);
    }

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

    public static void AssignTo(DyeColor dyeColor, Player player) {
        player.displayClientMessage(
                new TextComponent("Вы присоединились к команде " + dyeColor.toString().toUpperCase() + "!")
                        .withStyle(dyeColor == DyeColor.RED ? ChatFormatting.RED : ChatFormatting.BLUE),
                true);

        boolean areTeamsBalanced = true;
        // Add to Attacking Team
        if (dyeColor == DyeColor.RED) {
            if (attackers.MembersCount() - 1 > defenders.MembersCount()) {
                areTeamsBalanced = false;
            }
        } else if (dyeColor == DyeColor.BLUE) {
            if (defenders.MembersCount() - 1 > attackers.MembersCount()) {
                areTeamsBalanced = false;
            }
        }

        if (areTeamsBalanced) 
        {
            attackers.TryRemove(player);
            defenders.Assign(player);
        }
        else
        {
            player.sendMessage(
                new TextComponent(
                        "Команда " + dyeColor.toString().toUpperCase() + "содержит слишком много участников"),
                player.getUUID());
            
        }
    }

}
