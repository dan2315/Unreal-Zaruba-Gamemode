package com.dod.UnrealZaruba.TeamLogic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Config.TeamsConfig;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.dod.UnrealZaruba.WorldManager.WorldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerLevel;
import com.dod.UnrealZaruba.Utils.IResettable;

public class TeamManager implements IResettable {

    HashMap<TeamColor, TeamContext> teams = new HashMap<>();

    public HashMap<TeamColor, StructureTemplate> tent_templates = new HashMap<>();

    public HashMap<TeamColor, TeamContext> GetTeams() {
        return teams;
    }

    public TeamManager() {
        var teamData = Load();
        if (teamData != null) {
            for (Map.Entry<TeamColor, TeamDataEntry> data : teamData.getTeamSpawns().entrySet()) {
                UnrealZaruba.LOGGER.info("[UnrealZaruba] Loading team: " + data.getKey());
                var teamContext = AddTeam(data.getKey(), data.getValue().getBlockPos(), data.getValue().getBarrierVolumes());
                StructureTemplateManager structureManager = ServerLifecycleHooks.getCurrentServer().overworld().getStructureManager();
                tent_templates.put(data.getKey(), teamContext.GetTentTemplate(structureManager));
            }
        }
    }


    public void Initialize() {
        for (TeamContext team : teams.values()) {
            team.SetupMinecraftTeam(ServerLifecycleHooks.getCurrentServer());
        }
    }

    public void Cleanup() {
        for (TeamContext team : teams.values()) {
            //TODO: team.CleanMinecraftTeam(ServerLifecycleHooks.getCurrentServer());
        }
    } 

    public TeamContext AddTeam(TeamColor teamColor, BlockPos spawn, List<BlockVolume> baseVolume) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        var teamContext = new TeamContext(this ,spawn, teamColor, baseVolume);
        teams.put(teamColor, teamContext);
        return teamContext;
    }

    @Deprecated
    public void AddTeam(TeamColor teamColor, BlockPos spawn) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        teams.put(teamColor, new TeamContext(this, spawn, teamColor));
    }

    public void SetSpawn(TeamColor color, BlockPos spawn) {
        teams.get(color).SetSpawn(spawn);
    }

    public boolean IsInTeam(Player player) {
        for (TeamContext team : teams.values()) {
            return team.members.contains(player.getUUID());
        }
        return false;
    }

    public TeamContext GetPlayersTeam(Player player) {
        for (TeamContext team : teams.values()) {
            if (team.members.contains(player.getUUID())) {
                return team;
            }
        }
        return null;
    }

    public TeamContext GetPlayersOppositeTeam(ServerPlayer player) {
        TeamColor color = GetPlayersTeam(player).Color();
        return GetOppositeTeamTo(color);
    }

    public TeamContext GetOppositeTeamTo(TeamColor teamColor) {
        return switch (teamColor) {
            case RED -> teams.get(TeamColor.BLUE);
            case BLUE -> teams.get(TeamColor.RED);
            default -> teams.get(TeamColor.UNDEFINED);
        };
    }

    public TeamContext Get(TeamColor color) {
        return teams.get(color);
    }

    public TeamContext GetWithMinimumMembers() {
        return teams.values().stream()
            .min((a, b) -> a.MembersCount() - b.MembersCount())
            .orElse(null);
    }

    public boolean DeleteBarriersAtSpawn() {
        for (TeamContext team : teams.values()) {
            if (team.Spawn() == null) {
                return false;
            }
            UnrealZaruba.LOGGER.warn("[Во, бля] Во бля");

            List<BlockVolume> barriers = team.BarrierVolumes();
            if (barriers == null) return false;
            for (BlockVolume volume : barriers) {
                Utils.deleteBarriers(volume);
            }
        }
        return true;
    }

    public boolean AreTeamsBalanced(TeamColor dyeColor) {
        TeamContext targetTeam = teams.get(dyeColor);
        if (targetTeam == null) {
            UnrealZaruba.LOGGER.warn("Команда [" + dyeColor.toString() + "] не проиницилизирована");
            return false;
        }
        int targetTeamCount = targetTeam.MembersCount();

        teams.forEach((id, team) -> {
            UnrealZaruba.LOGGER.info("Команда [" + id.toString() + "] содержит " + team.MembersCount() + " участников");
        });

        int maxOtherTeamsCount = teams.values().stream()
                .filter(team -> !team.equals(targetTeam))
                .mapToInt(TeamContext::MembersCount)
                .max()
                .orElse(0);

        return targetTeamCount <= maxOtherTeamsCount;
    }

    public void AssignToTeam(TeamColor dyeColor, ServerPlayer player) {
        if (!AreTeamsBalanced(dyeColor)) {
            player.sendSystemMessage(
                    Component.literal(
                            "Команда " + dyeColor.toString().toUpperCase() + " содержит слишком много участников"
                            ),
                    true);
            return;
        }

        for (TeamContext team : teams.values()) {
            team.TryRemove(player);
        }

        teams.get(dyeColor).Assign(player);
    }

    public void ChangeGameModeOfAllParticipants(GameType gameType) {
        var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        for (TeamContext team : teams.values()) {
            for (UUID playerId : team.members) {
                var player = playerList.getPlayer(playerId);
                if (player != null)
                    player.setGameMode(gameType);
            }
        }
    }

    public void PlayBattleSound() {
        for (TeamContext team : teams.values()) {
            team.PlayBattleSound();
        }
    }

    public void teleportToSpawn(ServerPlayer serverPlayer) {
        TeamContext team = GetPlayersTeam(serverPlayer);
        if (team == null) {
            serverPlayer.sendSystemMessage(Component.literal("Вы не присоединены ни к одной команде"));
            return;
        }
        
        BlockPos Spawn = team.Spawn();
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(WorldManager.gameLevel, x, y, z, 0, 0);
    }

    public void teleportToTent(ServerPlayer serverPlayer) {
        TeamContext team = GetPlayersTeam(serverPlayer);
        if (team == null) {
            serverPlayer.sendSystemMessage(Component.literal("Вы не присоединены ни к одной команде"));
            return;
        }

        BlockPos Spawn = team.Tent().spawn_point;
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(WorldManager.gameLevel, x, y, z, 0, 0);
    }

    @Override
    public void reset() {
        for (TeamContext team : teams.values()) {
            team.reset();
        }
    }

    public void Save() {
        TeamData data = new TeamData(); 
        HashMap<TeamColor, TeamDataEntry> teamSpawns = new HashMap<>();
        for (Map.Entry<TeamColor, TeamContext> team : teams.entrySet()) {
            teamSpawns.put(team.getKey(), new TeamDataEntry(team.getValue().Spawn(), team.getValue().BarrierVolumes()));
        }
        data.setTeamSpawns(teamSpawns);
        
        TeamsConfig.getInstance().saveTeamData(data);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved teams configuration");
    }

    public TeamData Load() {
        TeamData loadedData = TeamsConfig.getInstance().loadTeamData();
        if (loadedData != null) {
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Loaded teams configuration");
        } else {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] No team data found, using empty team data");
            loadedData = new TeamData();
        }
        return loadedData;
    }

    public void disableLevelSaving(ServerLevel level) {
        if (level != null) {
            level.noSave = true;
        }
    }

    public void enableLevelSaving(ServerLevel level) {
        if (level != null) {
            level.noSave = false;
        }
    }
}
