package com.dod.UnrealZaruba.TeamLogic;

import java.util.HashMap;
import java.util.UUID;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.CaptureObjectivesMode;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.server.ServerLifecycleHooks;


public class TeamManager{
    static HashMap<TeamColor, Team> teams = new HashMap<>();

    private static Team defenders = new Team(null, TeamColor.BLUE);
    private static Team attackers = new Team(null, TeamColor.RED);

    public static void Initialize() {
        teams.put(TeamColor.RED, attackers);
        teams.put(TeamColor.BLUE, defenders);
    }

    static {
        Initialize();
    }

    public static void SetSpawn(TeamColor color, BlockPos spawn) {
        teams.get(color).SetSpawn(spawn);

    }

    public static boolean IsInTeam(Player player) {
        return defenders.members.contains(player.getUUID()) || attackers.members.contains(player.getUUID());
    }

    public static Team GetPlayersTeam(Player player) {
        if (defenders.members.contains(player.getUUID()))
            return teams.get(defenders.color);
        if (attackers.members.contains(player.getUUID()))
            return teams.get(attackers.color);
        else
            return null;
    }

    public static void DeleteBarriersAtSpawn() {
        CaptureObjectivesMode.deleteBarriers(attackers.spawn, 1);
        CaptureObjectivesMode.deleteBarriers(defenders.spawn, 1);
    }

    public static void AssignTo(TeamColor dyeColor, ServerPlayer player) {
        boolean areTeamsBalanced = true;

        if (dyeColor == TeamColor.RED) {
            if (attackers.MembersCount() - 1 > defenders.MembersCount()) {
                areTeamsBalanced = false;
            }
        } else if (dyeColor == TeamColor.BLUE) {
            if (defenders.MembersCount() - 1 > attackers.MembersCount()) {
                areTeamsBalanced = false;
            }
        }

        if (!areTeamsBalanced) {
            player.sendMessage(
                    new TextComponent(
                            "Команда " + dyeColor.toString().toUpperCase() + " содержит слишком много участников"),
                    player.getUUID());
            return;
        }

        if (dyeColor == TeamColor.RED) {
            defenders.TryRemove(player);
            attackers.TryRemove(player);
            attackers.Assign(player);
        }
        if (dyeColor == TeamColor.BLUE) {
            defenders.TryRemove(player);
            attackers.TryRemove(player);
            defenders.Assign(player);
        }

    }

    public static void GiveKit(MinecraftServer server) {
        defenders.GiveKit(server);
        attackers.GiveKit(server);
    }

    public static void GiveKitTo(MinecraftServer server, ServerPlayer player) {
        ItemKits.GiveKit(server, player, GetPlayersTeam(player));
    }

    public static void ChangeGameModeOfAllParticipants(GameType gameType) {
        var playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        for (UUID playerId : defenders.members) {
            var player = playerList.getPlayer(playerId);
            if (player != null) player.setGameMode(gameType);
        }
        for (UUID playerId : attackers.members) {
            var player = playerList.getPlayer(playerId);
            if (player != null) player.setGameMode(gameType);
        }
    }

    public static void teleportToSpawn(Player player) {
        BlockPos spawn = teams.get(GetPlayersTeam(player)).spawn;
        player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
    }
}
