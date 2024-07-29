package com.dod.UnrealZaruba.TeamLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;

public class TeamU {
    private BlockPos spawn;
    private List<BlockVolume> barrierVolumes = new ArrayList<BlockVolume>();
    List<UUID> members = new ArrayList<>();
    private TeamColor color;
    MinecraftServer server;

    public static PlayerTeam redTeam;
    public static PlayerTeam blueTeam;

    public TeamColor Color() {return color;}
    public BlockPos Spawn() {return spawn;}
    public List<BlockVolume> BarrierVolumes() {return barrierVolumes;}
    
    public TeamU(BlockPos spawn, TeamColor color, List<BlockVolume> barrierVolumes) {
        this.spawn = spawn;
        this.color = color;
        this.barrierVolumes = barrierVolumes;
        server = ServerLifecycleHooks.getCurrentServer();
    }

    public TeamU(BlockPos spawn, TeamColor color) {
        this.spawn = spawn;
        this.color = color;
        server = ServerLifecycleHooks.getCurrentServer();
    }
    
    public void AddBarrierVolume(BlockVolume barrierVolume) {
        if (barrierVolumes == null) barrierVolumes = new ArrayList<BlockVolume>();
        barrierVolumes.add(barrierVolume);
    }

    public static void SetupMinecraftTeams(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        // Create Red Team
        redTeam = scoreboard.getPlayerTeam("RED");
        if (redTeam == null) {
            redTeam = scoreboard.addPlayerTeam("RED");
        }
        redTeam.setColor(ChatFormatting.RED);
        redTeam.setDisplayName(new TextComponent("Red Team"));
        redTeam.setNameTagVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
        redTeam.setDeathMessageVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);

        // Create Blue Team
        blueTeam = scoreboard.getPlayerTeam("BLUE");
        if (blueTeam == null) {
            blueTeam = scoreboard.addPlayerTeam("BLUE");
        }
        blueTeam.setColor(ChatFormatting.BLUE);
        blueTeam.setDisplayName(new TextComponent("Blue Team"));
        blueTeam.setNameTagVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
        blueTeam.setDeathMessageVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
    }

    public void Assign(ServerPlayer player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(player.getName().getString());


        if (spawn == null) {
            player.sendMessage(new TextComponent("Скажи Доду, что он забыл спавн поставить))"), player.getUUID());
        } else {
            members.add(player.getUUID());
            if (team == null) {
                if (BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(player).color == TeamColor.RED) {
                    scoreboard.addPlayerToTeam(player.getName().getString(), redTeam);
                }
                if (BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(player).color == TeamColor.BLUE) {
                    scoreboard.addPlayerToTeam(player.getName().getString(), blueTeam);
                } else {
                    System.out.println("[PIZDA RYLU] ");
                }
            } else {
                System.out.println("XUINUA");
            }
            player.displayClientMessage(
                    new TextComponent("Вы присоединились к команде " + color.toString().toUpperCase() + "!")
                            .withStyle(color == TeamColor.RED ? ChatFormatting.RED : ChatFormatting.BLUE),
                    true);
            player.setRespawnPosition(player.getLevel().dimension(), spawn, 0, false, false);
            // Utils.setSpawnPoint(player, spawn);
            player.getInventory().clearContent();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            BaseGamemode.currentGamemode.TeamManager.GiveArmorKitTo(server, player);
        }
    }

    public void TryRemove(ServerPlayer player) {
        if (members.contains(player.getUUID())) {
            members.remove(player.getUUID());
        }
    }

    public int MembersCount() {
        return members.size();
    }

    public void SetSpawn(BlockPos pos) {
        spawn = pos;
    }

    public void ProcessWin() {
        
    }

    public void ProcessLose() {
        
    }

    public void TeleportToSpawn() 
    {
        for (UUID playerId : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) return;
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        }
    }

    public void GiveKit() {
        for (UUID playerId : members) {
            ItemKits.GiveKit(server, server.getPlayerList().getPlayer(playerId), this);
        }
    }
}
