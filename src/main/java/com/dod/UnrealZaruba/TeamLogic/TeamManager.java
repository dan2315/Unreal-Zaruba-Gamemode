package com.dod.UnrealZaruba.TeamLogic;

import java.util.HashMap;
import java.util.UUID;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TeamManager {

    HashMap<TeamColor, TeamU> teams = new HashMap<>();

//    HashMap<TeamColor, PlayerTeam> scoreboard_team_color = new HashMap<>();

    public void AddTeam(TeamColor teamColor) {
        teams.put(teamColor, new TeamU(null, teamColor));
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
                return teams.get(team.color);
            }
        }
        return null;
    }

    public TeamU Get(TeamColor color) {
        return teams.get(color);
    }

    public boolean DeleteBarriersAtSpawn() {
        for (TeamU team : teams.values()) {
            if (team.spawn == null) {
                return false;
            }
            Utils.deleteBarriers(team.spawn, 1);
        }
        return true;
    }

    public boolean AreTeamsBalanced(TeamColor dyeColor) {
        TeamU targetTeam = teams.get(dyeColor);
        int targetTeamCount = targetTeam.MembersCount();

        int maxOtherTeamsCount = teams.values().stream()
                .filter(team -> !team.equals(targetTeam))
                .mapToInt(TeamU::MembersCount)
                .max()
                .orElse(0);

        return targetTeamCount <= maxOtherTeamsCount + 1;
    }

    public void AssignToTeam(TeamColor dyeColor, ServerPlayer player) {
//        if (IsInTeam(player)) return;
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

    public void GiveKit(MinecraftServer server) {
        for (TeamU team : teams.values()) {
            team.GiveKit(server);
        }
    }

    public void GiveKitTo(MinecraftServer server, ServerPlayer player) {
        ItemKits.GiveKit(server, player, GetPlayersTeam(player));
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
        
        BlockPos Spawn = team.spawn;
        double x = Spawn.getX() + 0.5d;
        double y = Spawn.getY() + 1.1d;
        double z = Spawn.getZ() + 0.5d;
        serverPlayer.teleportTo(x, y ,z);
    }
}
