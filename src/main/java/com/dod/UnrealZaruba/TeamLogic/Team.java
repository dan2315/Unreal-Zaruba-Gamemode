package com.dod.UnrealZaruba.TeamLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.CaptureObjectivesMode;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class Team {
    BlockPos spawn;
    List<UUID> members = new ArrayList<>();
    TeamColor color;
    public TeamColor Color() {return color;}

    public Team(BlockPos spawn, TeamColor color) {
        this.spawn = spawn;
        this.color = color;
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
            // player.setRespawnPosition(player.getLevel().dimension(), spawn, 0, false, false);
            CaptureObjectivesMode.setSpawnPoint(player, spawn);
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
            player.getInventory().clearContent();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            TeamManager.GiveKitTo(server, player);
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

    public void GiveKit(MinecraftServer server) {
        for (UUID playerId : members) {
            ItemKits.GiveKit(server, server.getPlayerList().getPlayer(playerId), this);
        }
    }
}
