package com.dod.UnrealZaruba.CommanderSystem;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GameStage;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Player.PlayerContext;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class CommanderSystem {

    public static void ProcessCommanderVote(UUID invokerPlayerUUID, UUID targetPlayerUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer invokerPlayer = server.getPlayerList().getPlayer(invokerPlayerUUID);
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetPlayerUUID);
        
        if (invokerPlayer == null) return;
        if (targetPlayer == null) {
            invokerPlayer.sendMessage(new TextComponent("Выбранный игрок не найден"), invokerPlayer.getUUID());
            return;
        }

        PlayerContext invokerPlayerContext = PlayerContext.Get(invokerPlayerUUID);
        PlayerContext targetPlayerContext = PlayerContext.Get(targetPlayerUUID);

        TeamGamemode teamGamemode = invokerPlayerContext.Gamemode(TeamGamemode.class);
        TeamColor invokerTeamId = teamGamemode.GetTeamManager().GetPlayersTeam(invokerPlayer).Color();
        TeamColor targetTeamId = teamGamemode.GetTeamManager().GetPlayersTeam(targetPlayer).Color();

        if (teamGamemode.gameStage != GameStage.CommanderVoting) { 
            invokerPlayer.sendMessage(new TextComponent("Еще рано или уже поздно"), invokerPlayer.getUUID());
            return;
        }

        if (invokerTeamId != targetTeamId) { 
            invokerPlayer.sendMessage(new TextComponent("Нахуй ты за противника голосуешь"), invokerPlayer.getUUID());
            return;
        }

        if (invokerPlayerUUID == targetPlayerUUID) { 
            invokerPlayer.sendMessage(new TextComponent("Ты мне блять за себя поголосуй"), invokerPlayer.getUUID());
            return;
        }

        if (invokerPlayerContext.AlreadyVoted()) {
            invokerPlayer.sendMessage(new TextComponent("Ты не можешь проголосовать дважды"), invokerPlayer.getUUID());
            return;
        }

        invokerPlayer.sendMessage(new TextComponent("Голос отправлен за " + targetPlayer.getName().toString() + ", ожидайте!"), invokerPlayer.getUUID());
        teamGamemode.GetTeamManager().GetPlayersTeam(targetPlayer).GiveVote(targetPlayer, targetPlayerContext);
        invokerPlayerContext.SetVoted();
    }
}
