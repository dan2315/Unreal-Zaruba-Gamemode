package com.dod.UnrealZaruba.Commands.CommandHandlers;

import com.dod.UnrealZaruba.Services.GameStatisticsService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestLeaderboardCommand implements ICommandHandler {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testlb")
                .requires(cs -> cs.hasPermission(3)).executes(context -> {
                    List<UUID> won = new ArrayList<UUID>();
                    List<UUID> lost = new ArrayList<UUID>();
                    List<ServerPlayer> players = ServerLifecycleHooks.getCurrentServer()
                            .getPlayerList().getPlayers();

                    for (int i = 0; i < players.size(); i++) {
                        if (i % 2 == 0) {
                            won.add(players.get(i).getUUID());
                        } else {
                            lost.add(players.get(i).getUUID());
                        }
                    }

                    // gameStatisticsService.UpdatePlayerRanking(won, lost);

                    return 1;
                }));
    }

    @Override
    public String getCommandName() {
        return "testlb";
    }
} 