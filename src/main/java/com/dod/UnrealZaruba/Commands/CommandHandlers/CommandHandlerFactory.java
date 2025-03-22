package com.dod.UnrealZaruba.Commands.CommandHandlers;

import java.util.ArrayList;
import java.util.List;

public class CommandHandlerFactory {
 
    public static List<ICommandHandler> createAllHandlers() {
        List<ICommandHandler> handlers = new ArrayList<>();
        
        handlers.add(new TestLeaderboardCommand());
        handlers.add(new TeleportToDimensionCommand());
        handlers.add(new GetRPGCommand());
        handlers.add(new GetWoolCommand());
        handlers.add(new SetPrefixCommand());
        handlers.add(new DoLinksSafeCommand());
        handlers.add(new SendTestMessageCommand());
        handlers.add(new VoteCommand());
        handlers.add(new CreateNPCCommand());
        // handlers.add(new SetTeamBaseCommand());
        handlers.add(new StartBattleCommand());
        handlers.add(new CreateObjectiveCommand());
        
        return handlers;
    }
} 