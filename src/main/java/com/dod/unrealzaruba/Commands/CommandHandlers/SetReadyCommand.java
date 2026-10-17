package com.dod.unrealzaruba.Commands.CommandHandlers;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Title.TitleMessage;
import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


public class SetReadyCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(getCommandName())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("You must be a player to use this command"));
                        return 0;
                    }
                    
                    PlayerContext playerContext = PlayerContext.Get(player.getUUID());
                    com.dod.unrealzaruba.Lobby.LobbyService.toggleReady(player);
                    context.getSource().sendSuccess(() -> Component.literal("Set ready to " + playerContext.IsReady()), true);

                    var players = UnrealZaruba.server.getPlayerList().getPlayers();
                    int playersReady = 0;
                    for (ServerPlayer p : players) {
                        PlayerContext other = PlayerContext.Get(p.getUUID());
                        if (other != null && other.IsReady()) playersReady++;
                    }
                    if (players.size() >= ConfigManager.getMainConfig().getMinPlayersToStart()) {
                        for (ServerPlayer pl : players) {
                            TitleMessage.sendActionbar(pl, Component.literal("§6Игроков готово [" + playersReady + "/" + players.size() + "]"));
                        }
                    }
                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "ready";
    }
}
