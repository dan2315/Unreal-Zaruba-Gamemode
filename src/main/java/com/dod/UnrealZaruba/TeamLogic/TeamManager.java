package com.dod.UnrealZaruba.TeamLogic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;


import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.unrealzaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import com.fasterxml.jackson.databind.ser.Serializers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TeamManager {

    HashMap<TeamColor, TeamU> teams = new HashMap<>();
    HashMap<TeamColor, PlayerTeam> scoreboard_team_color = new HashMap<>();

    public HashMap<TeamColor, StructureTemplate> tent_templates = new HashMap<>();
    

    public TeamManager() {
        var teamData = Load();
        if (teamData != null) {
            for (var data : teamData.teamSpawns.entrySet()) {
                AddTeam(data.getKey(), data.getValue().blockPos, data.getValue().barrierVolumes);
                StructureManager structureManager = ServerLifecycleHooks.getCurrentServer().overworld().getStructureManager();

                StructureTemplate template_red = structureManager.getOrCreate(new ResourceLocation("unrealzaruba", "red_tent"));
                StructureTemplate template_blue = structureManager.getOrCreate(new ResourceLocation("unrealzaruba", "blue_tent"));

                tent_templates.put(TeamColor.RED, template_red);
                tent_templates.put(TeamColor.BLUE, template_blue);
                unrealzaruba.LOGGER.warn("[Во, бля] " + data.getKey().toString());
            }
        }
    }

    public void AddTeam(TeamColor teamColor, BlockPos spawn, List<BlockVolume> baseVolume) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        teams.put(teamColor, new TeamU(spawn, teamColor, baseVolume));
    }

    public void AddTeam(TeamColor teamColor, BlockPos spawn) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        teams.put(teamColor, new TeamU(spawn, teamColor));
    }

    public void SetSpawn(TeamColor color, BlockPos spawn) {
        teams.get(color).SetSpawn(spawn);
    }

    public boolean IsInTeam(Player player) {
        for (TeamU team : teams.values()) {
            return team.members.contains(player.getUUID());
        }
        return false;
    }

    public TeamU GetPlayersTeam(Player player) {
        for (TeamU team : teams.values()) {
            if (team.members.contains(player.getUUID())) {
                return teams.get(team.Color());
            }
        }
        return null;
    }

    public TeamU GetPlayersOppositeTeam(Player player) {
        TeamColor color = GetPlayersTeam(player).Color();
        return GetOppositeTeamTo(color);
    }

    public TeamU GetOppositeTeamTo(TeamColor teamColor) {
        switch (teamColor) {
            case RED:
                return teams.get(TeamColor.BLUE);
            case BLUE:
                return teams.get(TeamColor.RED);
            default:
                return teams.get(TeamColor.UNDEFINED);
        }
    }

    public TeamU Get(TeamColor color) {
        return teams.get(color);
    }

    public boolean DeleteBarriersAtSpawn() {
        for (TeamU team : teams.values()) {
            if (team.Spawn() == null) {
                return false;
            }
            unrealzaruba.LOGGER.warn("[Во, бля] Во бля");

            List<BlockVolume> barriers = team.BarrierVolumes();
            if (barriers == null) return false;
            for (BlockVolume volume : barriers) {
                Utils.deleteBarriers(volume);
            }
        }
        return true;
    }

    public boolean AreTeamsBalanced(TeamColor dyeColor) {
        TeamU targetTeam = teams.get(dyeColor);
        if (targetTeam == null) {
            unrealzaruba.LOGGER.warn("Команда [" + dyeColor.toString() + "] не проиницилизирована");
            return false;
        }
        int targetTeamCount = targetTeam.MembersCount();

        int maxOtherTeamsCount = teams.values().stream()
                .filter(team -> !team.equals(targetTeam))
                .mapToInt(TeamU::MembersCount)
                .max()
                .orElse(0);

        return targetTeamCount <= maxOtherTeamsCount;
    }

    public void AssignToTeam(TeamColor dyeColor, ServerPlayer player) {
        if (!AreTeamsBalanced(dyeColor)) {
            player.sendMessage(
                    new TextComponent(
                            "Команда " + dyeColor.toString().toUpperCase() + " содержит слишком много участников"),
                    player.getUUID());
            return;
        }

        for (TeamU team : teams.values()) {
            team.TryRemove(player);
        }

        teams.get(dyeColor).Assign(player);
    }

    public void GiveKit() {
        for (TeamU team : teams.values()) {
            team.GiveKit();
        }
    }

    public void GiveArmorKitTo(MinecraftServer server, ServerPlayer player) {
        ItemKits.GiveArmorKit(server, player, GetPlayersTeam(player));
    }

    public void GiveKitTo(MinecraftServer server, ServerPlayer player) {
        TeamU team = GetPlayersTeam(player);
        if (team == null) return;
        ItemKits.GiveKit(server, player, team);
    }

    public void ChangeGameModeOfAllParticipants(GameType gameType) {
        var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        for (TeamU team : teams.values()) {
            for (UUID playerId : team.members) {
                var player = playerList.getPlayer(playerId);
                if (player != null)
                    player.setGameMode(gameType);
            }
        }
    }

    public void teleportToSpawn(ServerPlayer serverPlayer) {
        TeamU team = GetPlayersTeam(serverPlayer);
        if (team == null){
            serverPlayer.sendMessage(new TextComponent("Вы не присоединены ни к одной команде"),
                serverPlayer.getUUID());
            return;
        }
        
        BlockPos Spawn = team.Spawn();
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(x, y ,z);
    }

    public void teleportToTent(ServerPlayer serverPlayer) {
        TeamU team = GetPlayersTeam(serverPlayer);
        if (team == null) {
            serverPlayer.sendMessage(new TextComponent("Вы не присоединены ни к одной команде"),
                    serverPlayer.getUUID());
            return;
        }

        BlockPos Spawn = team.active_tent.spawn_point;
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(x, y, z);
        serverPlayer.sendMessage(new TextComponent("Щелкнуло!"), serverPlayer.getUUID());
    }

    public void RespawnPlayer(ServerPlayer player, boolean tentChosen) {
        if (!(BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(player).active_tent == null)) {
            if (tentChosen) {
                teleportToTent(player);
            } else {
                teleportToSpawn(player);
            }
        } else {
            teleportToSpawn(player);
        }
    }

    public void Save() {
        TeamData data = new TeamData(); 
        data.teamSpawns = new HashMap<>();
        for (Map.Entry<TeamColor, TeamU> team : teams.entrySet()) {

            data.teamSpawns.put(team.getKey(), new TeamDataEntry(team.getValue().Spawn(), team.getValue().BarrierVolumes()));
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
            TeamData loadedData = ConfigManager.loadConfig(ConfigManager.Teams, TeamData.class);
            unrealzaruba.LOGGER.warn("[Во, бля] Загрузил конфиг для TeamManager");
            return loadedData;
        } catch (IOException e) {
            unrealzaruba.LOGGER.warn("[Ай, бля] Config file for TeamManager was not found");
            return null;
        }
    } 
}
