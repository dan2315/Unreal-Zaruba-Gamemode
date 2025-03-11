package com.dod.UnrealZaruba.TeamLogic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TeamManager {

    HashMap<TeamColor, TeamU> teams = new HashMap<>();

    public HashMap<TeamColor, StructureTemplate> tent_templates = new HashMap<>();

    /**
     * Get teams hash map.
     *
     * @return the hash map
     */
    public HashMap<TeamColor, TeamU> GetTeams() {
        return teams;
    }

    /**
     * Instantiates a new Team manager.
     */
    public TeamManager() {
        var teamData = Load();
        if (teamData != null) {
            for (var data : teamData.teamSpawns.entrySet()) {
                AddTeam(data.getKey(), data.getValue().blockPos, data.getValue().barrierVolumes);
                StructureTemplateManager structureManager = ServerLifecycleHooks.getCurrentServer().overworld().getStructureManager();

                StructureTemplate template_red = structureManager.getOrCreate(new ResourceLocation("unrealzaruba", "red_tent"));
                StructureTemplate template_blue = structureManager.getOrCreate(new ResourceLocation("unrealzaruba", "blue_tent"));

                tent_templates.put(TeamColor.RED, template_red);
                tent_templates.put(TeamColor.BLUE, template_blue);
                UnrealZaruba.LOGGER.warn("[Во, бля] " + data.getKey().toString());
            }
        }
    }

    /**
     * Add team.
     *
     * @param teamColor  the team color
     * @param spawn      the spawn
     * @param baseVolume the base volume
     */
    public void AddTeam(TeamColor teamColor, BlockPos spawn, List<BlockVolume> baseVolume) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        teams.put(teamColor, new TeamU(this ,spawn, teamColor, baseVolume));
    }

    /**
     * Add team.
     *
     * @deprecated
     *
     * @param teamColor the team color
     * @param spawn     the spawn
     */
    @Deprecated
    public void AddTeam(TeamColor teamColor, BlockPos spawn) {
        if (teams.containsKey(teamColor)) teams.remove(teamColor);
        teams.put(teamColor, new TeamU(this, spawn, teamColor));
    }

    /**
     * Set spawn.
     *
     * @param color the color
     * @param spawn the spawn
     */
    public void SetSpawn(TeamColor color, BlockPos spawn) {
        teams.get(color).SetSpawn(spawn);
    }

    /**
     * Checks if player in any team
     *
     * @param player the player
     * @return the boolean
     */
    public boolean IsInTeam(Player player) {
        for (TeamU team : teams.values()) {
            return team.members.contains(player.getUUID());
        }
        return false;
    }

    /**
     * Get players team {@link TeamU}
     *
     * @param player the player
     * @return the TeamU
     */
    public TeamU GetPlayersTeam(Player player) {
        for (TeamU team : teams.values()) {
            if (team.members.contains(player.getUUID())) {
                return team;
            }
        }
        return null;
    }

    /**
     * Get players opposite team
     *
     * @param player the player
     * @return {@link TeamU}
     */
    public TeamU GetPlayersOppositeTeam(Player player) {
        TeamColor color = GetPlayersTeam(player).Color();
        return GetOppositeTeamTo(color);
    }

    /**
     * Get opposite team to team u.
     *
     * @param teamColor the team color
     * @return {@link TeamU}
     */
    public TeamU GetOppositeTeamTo(TeamColor teamColor) {
        return switch (teamColor) {
            case RED -> teams.get(TeamColor.BLUE);
            case BLUE -> teams.get(TeamColor.RED);
            default -> teams.get(TeamColor.UNDEFINED);
        };
    }

    /**
     * Get {@link TeamU}
     *
     * @param color the color
     * @return {@link TeamU}
     */
    public TeamU Get(TeamColor color) {
        return teams.get(color);
    }

    /**
     * Delete barriers at spawn boolean.
     *
     * @return the boolean
     */
    public boolean DeleteBarriersAtSpawn() {
        for (TeamU team : teams.values()) {
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

    /**
     * Are teams balanced boolean.
     *
     * @param dyeColor the dye color
     * @return the boolean
     */
    public boolean AreTeamsBalanced(TeamColor dyeColor) {
        TeamU targetTeam = teams.get(dyeColor);
        if (targetTeam == null) {
            UnrealZaruba.LOGGER.warn("Команда [" + dyeColor.toString() + "] не проиницилизирована");
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

    /**
     * Assign to team.
     *
     * @param dyeColor the dye color
     * @param player   the player
     */
    public void AssignToTeam(TeamColor dyeColor, ServerPlayer player) {
        if (!AreTeamsBalanced(dyeColor)) {
            player.sendSystemMessage(
                    Component.literal(
                            "Команда " + dyeColor.toString().toUpperCase() + " содержит слишком много участников"));
            return;
        }

        for (TeamU team : teams.values()) {
            team.TryRemove(player);
        }

        teams.get(dyeColor).Assign(player);
    }

    /**
     * Give kit.
     */
    public void GiveKit() {
        for (TeamU team : teams.values()) {
            team.GiveKit();
        }
    }

    /**
     * Give armor kit to.
     *
     * @param server the server
     * @param player the player
     */
    public void GiveArmorKitTo(MinecraftServer server, ServerPlayer player) {
        ItemKits.GiveArmorKit(server, player, GetPlayersTeam(player));
    }

    /**
     * Give kit to.
     *
     * @param server the server
     * @param player the player
     */
    public void GiveKitTo(MinecraftServer server, ServerPlayer player) {
        TeamU team = GetPlayersTeam(player);
        if (team == null) return;
        ItemKits.GiveKit(server, player, team);
    }

    /**
     * Change game mode of all participants.
     *
     * @param gameType the game type
     */
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

    /**
     * Teleport to spawn.
     *
     * @param serverPlayer the server player
     */
    public void teleportToSpawn(ServerPlayer serverPlayer) {
        TeamU team = GetPlayersTeam(serverPlayer);
        if (team == null){
            serverPlayer.sendSystemMessage(Component.literal("Вы не присоединены ни к одной команде"));
            return;
        }
        
        BlockPos Spawn = team.Spawn();
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(x, y ,z);
    }

    /**
     * Teleport to tent.
     *
     * @param serverPlayer the server player
     */
    public void teleportToTent(ServerPlayer serverPlayer) {
        TeamU team = GetPlayersTeam(serverPlayer);
        if (team == null) {
            serverPlayer.sendSystemMessage(Component.literal("Вы не присоединены ни к одной команде"));
            return;
        }

        BlockPos Spawn = team.active_tent.spawn_point;
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(x, y, z);
    }

    /**
     * Respawn player.
     *
     * @param player     the player
     * @param tentChosen the tent chosen
     */
    public void RespawnPlayer(ServerPlayer player, boolean tentChosen) {
        if (!(GetPlayersTeam(player).active_tent == null)) {
            if (tentChosen) {
                teleportToTent(player);
            } else {
                teleportToSpawn(player);
            }
        } else {
            teleportToSpawn(player);
        }
    }

    /**
     * Save.
     */
    public void Save() {
        TeamData data = new TeamData(); 
        data.teamSpawns = new HashMap<>();
        for (Map.Entry<TeamColor, TeamU> team : teams.entrySet()) {

            data.teamSpawns.put(team.getKey(), new TeamDataEntry(team.getValue().Spawn(), team.getValue().BarrierVolumes()));
        }
        try {
            ConfigManager.saveConfig(ConfigManager.Teams, data);
            UnrealZaruba.LOGGER.warn("[Во, бля] Сохранил конфиг для TeamManager");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[Ай, бля] Unable to create config for TeamManager");
        }
    }

    /**
     * Load team data.
     *
     * @return the team data
     */
    public TeamData Load() {
        try {
            TeamData loadedData = ConfigManager.loadConfig(ConfigManager.Teams, TeamData.class);
            UnrealZaruba.LOGGER.warn("[Во, бля] Загрузил конфиг для TeamManager");
            return loadedData;
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[Ай, бля] Config file for TeamManager was not found");
            return null;
        }
    } 
}
