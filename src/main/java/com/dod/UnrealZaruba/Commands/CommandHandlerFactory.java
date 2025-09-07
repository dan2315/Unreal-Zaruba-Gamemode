package com.dod.UnrealZaruba.Commands;

import java.util.ArrayList;
import java.util.List;

import com.dod.UnrealZaruba.Commands.CommandHandlers.*;

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
        handlers.add(new ResetGameWorldCommand());
        handlers.add(new SetReadyCommand());
        handlers.add(new EndGameCommand());
        
        // Add team data management commands
        handlers.add(new SetGamemodeCommand());
        handlers.add(new AddTeamDataCommand());
        
        // Add the vehicle spawn commands
        handlers.add(new RegisterVehicleSpawnCommand());
        handlers.add(new ListVehicleSpawnsCommand());
        handlers.add(new RemoveVehicleSpawnCommand());
        handlers.add(new TriggerVehicleSpawnsCommand());
        
        return handlers;
    }
} 