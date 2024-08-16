package com.dod.UnrealZaruba.TeamLogic;

import java.util.*;


import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ModBlocks.Teams.Tent;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;

public class TeamU {
    private BlockPos spawn;
    public BlockPos tentSpawn;
    private List<BlockVolume> barrierVolumes = new ArrayList<BlockVolume>();
    List<UUID> members = new ArrayList<>();
    public TeamColor color;
    MinecraftServer server;
    TeamManager batya;

    public Tent active_tent;

    public HashMap<Player, Integer> votes = new HashMap<>();
    public HashSet<Player> has_voted = new HashSet<>();

    public static PlayerTeam redTeam;
    public static PlayerTeam blueTeam;

    public TeamColor Color() {return color;}
    public BlockPos Spawn() {return spawn;}
    public List<UUID> Members() {return members;}
    public List<BlockVolume> BarrierVolumes() {return barrierVolumes;}
    
    public TeamU(TeamManager teamManager, BlockPos spawn, TeamColor color, List<BlockVolume> barrierVolumes) {
        this.batya = teamManager;
        this.spawn = spawn;
        this.color = color;
        this.barrierVolumes = barrierVolumes;
        server = ServerLifecycleHooks.getCurrentServer();
    }

    public TeamU(TeamManager teamManager, BlockPos spawn, TeamColor color) {
        this.batya = teamManager;
        this.spawn = spawn;
        this.color = color;
        server = ServerLifecycleHooks.getCurrentServer();
    }
    
    public void AddBarrierVolume(BlockVolume barrierVolume) {
        if (barrierVolumes == null) barrierVolumes = new ArrayList<BlockVolume>();
        barrierVolumes.add(barrierVolume);
    }

    public void setActiveTent(Tent active_tent) {
        this.active_tent = active_tent;
    }

    public void SetupVotes(MinecraftServer server) {
        List<ServerPlayer> playerList = server.getPlayerList().getPlayers();

        for (Player player : playerList) {
            votes.put(player, 0);
        }
    }

    public void GiveVote(Player player) {
        if (!this.has_voted.contains(player)) {
            this.votes.put(player, votes.get(player) + 1);
            this.has_voted.add(player);
        } else {
            player.sendMessage(new TextComponent("Ты не можешь проголосовать дважды"), player.getUUID());
        }
    }

    public Player MostVoted() {
        Integer most_votes = 0;
        Player most_voted_player = null;

        for (Map.Entry<Player, Integer> entry : this.votes.entrySet()) {
            if (entry.getValue() > most_votes) {
                most_votes = entry.getValue();
                most_voted_player = entry.getKey();
            }
        }
        return most_voted_player;
    }

    public void setCommander(MinecraftServer server, Player player) {
        HashMap<TeamColor, TeamU> teams = this.batya.teams;


        if (player instanceof ServerPlayer serverPlayer) { // TODO Впихать в файл конфига для команд
            if (batya.GetPlayersTeam(serverPlayer).color == TeamColor.RED) {
                ItemKits.GiveItem(server, serverPlayer, "unrealzaruba:tent");
                ItemKits.GiveItem(server, serverPlayer, "unrealzaruba:tent");
                ItemKits.GiveItem(server, serverPlayer, "unrealzaruba:tent");
            } else {
                ItemKits.GiveItem(server, serverPlayer, "unrealzaruba:tent");
                ItemKits.GiveItem(server, serverPlayer, "unrealzaruba:tent");
            }
            SendMessageToTeam(server, "Командиром команды становится" + player.getName().getString());
        }
    }

    public void SendMessageToTeam(MinecraftServer server, String message) {
        this.members.forEach(element -> {
            server.getPlayerList().getPlayer(element).sendMessage(new TextComponent(message), element);
        });
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
                if (color == TeamColor.RED) {
                    scoreboard.addPlayerToTeam(player.getName().getString(), redTeam);
                }
                if (color == TeamColor.BLUE) {
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
            batya.GiveArmorKitTo(server, player);
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
