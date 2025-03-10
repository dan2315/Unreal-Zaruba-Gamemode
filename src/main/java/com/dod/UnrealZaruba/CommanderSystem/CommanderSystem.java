package com.dod.UnrealZaruba.CommanderSystem;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GameStage;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Player.PlayerContext;

import net.minecraft.network.chat.Component;
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
            invokerPlayer.sendSystemMessage(Component.literal("Выбранный игрок не найден"));
            return;
        }

        UnrealZaruba.LOGGER.warn("Invoker :" + invokerPlayer.getUUID() + "         Target: " + targetPlayer.getUUID().toString());

        PlayerContext invokerPlayerContext = PlayerContext.Get(invokerPlayerUUID);
        PlayerContext targetPlayerContext = PlayerContext.Get(targetPlayerUUID);

        TeamGamemode teamGamemode = invokerPlayerContext.Gamemode(TeamGamemode.class);
        TeamColor invokerTeamId = teamGamemode.GetTeamManager().GetPlayersTeam(invokerPlayer).Color();
        TeamColor targetTeamId = teamGamemode.GetTeamManager().GetPlayersTeam(targetPlayer).Color();

        if (teamGamemode.gameStage != GameStage.CommanderVoting) { 
            invokerPlayer.sendSystemMessage(Component.literal("Еще рано или уже поздно"));
            return;
        }

        if (invokerTeamId != targetTeamId) { 
            invokerPlayer.sendSystemMessage(Component.literal("Нахуй ты за противника голосуешь"));
            return;
        }

        if (invokerPlayerUUID.equals(targetPlayerUUID)) {
            invokerPlayer.sendSystemMessage(Component.literal("Ты мне блять за себя поголосуй"));
            return;
        }

        if (invokerPlayerContext.AlreadyVoted()) {
            invokerPlayer.sendSystemMessage(Component.literal("Ты не можешь проголосовать дважды"));
            return;
        }

        invokerPlayer.sendSystemMessage(Component.literal("Голос отправлен за " + targetPlayer.getName().getString() + ", ожидайте!"));
        teamGamemode.GetTeamManager().GetPlayersTeam(targetPlayer).GiveVote(targetPlayer, targetPlayerContext);
        invokerPlayerContext.SetVoted();
    }
}
