package com.dod.UnrealZaruba.TeamLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.DestroyObjectivesGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class Team {
    public BlockPos spawn;
    List<UUID> members = new ArrayList<>();
    TeamColor color;
    private List<GameObjective> objectives;
    MinecraftServer server;

    public TeamColor Color() {return color;}

    public Team(BlockPos spawn, TeamColor color) {
        this.spawn = spawn;
        this.color = color;
        server = ServerLifecycleHooks.getCurrentServer();
    }

    public void Assign(ServerPlayer player) {
        if (spawn == null) {
            player.sendMessage(new TextComponent("Скажи Доду, что он забыл спавн поставить))"), player.getUUID());
        } else {
            members.add(player.getUUID());
            player.displayClientMessage(
                    new TextComponent("Вы присоединились к команде " + color.toString().toUpperCase() + "!")
                            .withStyle(color == TeamColor.RED ? ChatFormatting.RED : ChatFormatting.BLUE),
                    true);
            player.setRespawnPosition(player.getLevel().dimension(), spawn, 0, false, false);
            // Utils.setSpawnPoint(player, spawn);
            player.getInventory().clearContent();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            BaseGamemode.currentGamemode.TeamManager.GiveKitTo(server, player);
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

    public void addObjective(GameObjective objective) {
        objectives.add(objective);
    }

    public List<GameObjective> getObjectives() {
        return objectives;
    }

    public void ProcessWin() {
        
    }

    public void ProcessLose() {
        
    }

    public BlockPos GetSpawn()
    {
        return spawn;
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
