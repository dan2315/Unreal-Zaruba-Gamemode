package com.dod.UnrealZaruba.TeamLogic;

import java.util.HashMap;
import java.util.UUID;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.MesilovoGamemode;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.server.ServerLifecycleHooks;


public class TeamManager{
    HashMap<TeamColor, Team> teams = new HashMap<>();

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
            if (team.members.contains(player.getUUID())){
                return teams.get(team.color);
            }
        }
        return null;
    }

    public boolean DeleteBarriersAtSpawn() {
        for (Team team : teams.values()) {
            MesilovoGamemode.deleteBarriers(team.spawn, 1);
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
                new TextComponent("Команда " + dyeColor.toString().toUpperCase() + " содержит слишком много участников"),
                player.getUUID());
            return;
        }

        for (Team team : teams.values()) {
            team.TryRemove(player);
        }

        teams.get(dyeColor).Assign(player);
    }

    public void GiveKit(MinecraftServer server) {
        for (Team team : teams.values()) {
            team.GiveKit(server);
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
                if (player != null) player.setGameMode(gameType);
            }
        }
    }

    public void teleportToSpawn(ServerPlayer serverPlayer) {
        BlockPos Spawn = GetPlayersTeam(serverPlayer).spawn;
        serverPlayer.teleportTo(Spawn.getX(), Spawn.getY(), Spawn.getZ());
    }
}
