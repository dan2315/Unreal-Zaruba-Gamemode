package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CaptureObjectivesMode {
    TeamManager teamManager;
    

    public static int StartPreparation(CommandContext<CommandSourceStack> context)
    {
        SetGamemodeAllExcludeOP(context.getSource().getServer().getPlayerList(), GameType.CREATIVE);

        return 1;
    }

    public static int StartGame(CommandContext<CommandSourceStack> context)
    {
        SetGamemodeAllExcludeOP(context.getSource().getServer().getPlayerList(), GameType.SURVIVAL);



        return 1;
    }

    public static void SetGamemodeAllExcludeOP(PlayerList playerList, GameType gameType)
    {
        for (ServerPlayer player : playerList.getPlayers()) {

            if (!playerList.isOp(player.getGameProfile())) {
                player.setGameMode(gameType);
            }
        }
    }

    public static void CleaInventoryAllPlayerExcludeOP(PlayerList playerList, GameType gameType)
    {
        for (ServerPlayer player : playerList.getPlayers()) {

            player.getInventory().clearContent();
        }
    }

}
