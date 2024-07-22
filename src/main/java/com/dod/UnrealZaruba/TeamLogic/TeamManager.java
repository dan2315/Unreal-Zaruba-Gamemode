package com.dod.UnrealZaruba.TeamLogic;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.valkyrienskies.core.impl.shadow.nu;

import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;

public class TeamManager {

    HashMap<TeamColor, Team> teams = new HashMap<>();

    public TeamManager() {
        var teamData = Load();
        if (teamData != null) {
            for (var data : teamData.teamSpawns.entrySet()) {
                AddTeam(data.getKey(), data.getValue());
                unrealzaruba.LOGGER.warn("[Во, бля] " + data.getKey().toString());
            }
        }
    }

    public void AddTeam(TeamColor teamColor, BlockPos spawn) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        teams.put(teamColor, new Team(spawn, teamColor));
    }

    public void SetSpawn(TeamColor color, BlockPos spawn) {
        teams.get(color).SetSpawn(spawn);
    }

    public boolean IsInTeam(Player player) {
        for (Team team : teams.values()) {
            return team.members.contains(player.getUUID());
        }
        return false;
    }

    public Team GetPlayersTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.members.contains(player.getUUID())) {
                return teams.get(team.color);
            }
        }
        return null;
    }

    public Team Get(TeamColor color) {
        return teams.get(color);
    }

    public boolean DeleteBarriersAtSpawn() {
        for (Team team : teams.values()) {
            if (team.spawn == null) {
                return false;
            }
            Utils.deleteBarriers(team.spawn, 1);
        }
        return true;
    }

    public boolean AreTeamsBalanced(TeamColor dyeColor) {
        Team targetTeam = teams.get(dyeColor);
        int targetTeamCount = targetTeam.MembersCount();

        int maxOtherTeamsCount = teams.values().stream()
                .filter(team -> !team.equals(targetTeam))
                .mapToInt(Team::MembersCount)
                .max()
                .orElse(0);

        return targetTeamCount <= maxOtherTeamsCount + 1;
    }

    public void AssignToTeam(TeamColor dyeColor, ServerPlayer player) {
        if (!AreTeamsBalanced(dyeColor)) {
            player.sendMessage(
                    new TextComponent(
                            "Команда " + dyeColor.toString().toUpperCase() + " содержит слишком много участников"),
                    player.getUUID());
            return;
        }

        for (Team team : teams.values()) {
            team.TryRemove(player);
        }

        teams.get(dyeColor).Assign(player);
    }

    public void setupTeams(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        createTeam(scoreboard, "RED", new TextComponent("Red Team"));
        createTeam(scoreboard, "BLUE", new TextComponent("Blue Team"));
    }

    public void createTeam(Scoreboard scoreboard, String teamName, Component displayName) {
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setDisplayName(displayName);
        }
    }

    public void GiveKit() {
        for (Team team : teams.values()) {
            team.GiveKit();
        }
    }

    public void GiveKitTo(MinecraftServer server, ServerPlayer player) {
        ItemKits.GiveKit(server, player, GetPlayersTeam(player));
    }

    public void ChangeGameModeOfAllParticipants(GameType gameType) {
        var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        for (Team team : teams.values()) {
            for (UUID playerId : team.members) {
                var player = playerList.getPlayer(playerId);
                if (player != null)
                    player.setGameMode(gameType);
            }
        }
    }

    public void teleportToSpawn(ServerPlayer serverPlayer) {
        Team team = GetPlayersTeam(serverPlayer);
        if (team == null){
            serverPlayer.sendMessage(new TextComponent("Вы не присоединены ни к одной команде"),
                serverPlayer.getUUID());
            return;
        }
        
        BlockPos Spawn = team.spawn;
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 0.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(x, y, z);
    }

    public void Save() {
        TeamData data = new TeamData(); 
        data.teamSpawns = new HashMap<>();
        for (var entry : teams.entrySet()) {
            data.teamSpawns.put(entry.getKey(), entry.getValue().spawn);
        }
        try {
            ConfigManager.saveConfig(ConfigManager.Teams, data);
            unrealzaruba.LOGGER.warn("[Во, бля] Сохранил конфиг для TeamManager");
        } catch (IOException e) {
            unrealzaruba.LOGGER.warn("[Ай, бля] Unable to create config for TeamManager");
        }
    } 

    public TeamData Load() {
        try {
            var loadedData = ConfigManager.loadConfig(ConfigManager.Teams, TeamData.class);
            unrealzaruba.LOGGER.warn("[Во, бля] Загрузил конфиг для TeamManager");
            return loadedData;
        } catch (IOException e) {
            unrealzaruba.LOGGER.warn("[Ай, бля] Config file for TeamManager was not found");
            return null;
        }
    } 
}
