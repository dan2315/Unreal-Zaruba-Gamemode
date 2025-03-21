package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.GameText.StartGameText;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;

public class TeamGamemode extends BaseGamemode {
    
    protected TeamManager TeamManager;
    public HashMap<TeamColor, StartGameText> startGameTexts = new HashMap<>();
    
    public TeamManager GetTeamManager() { return TeamManager; }
    public void SetTeamManager(TeamManager teamManager) { TeamManager = teamManager; }
    
    
    @Override
    protected void Initialize() {
        TeamManager.Initialize();
    }
    @Override
    public void HandleConnectedPlayer(Player player) {}

    @Override
    public void CheckObjectives() {}

    @Override
    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException { return 0; }



    @Override
    public void HandleRespawn(ServerPlayer player) {
        super.HandleRespawn(player);
        TeamPlayerContext playerContext = (TeamPlayerContext)PlayerContext.Get(player.getUUID());
        if (!(playerContext.Team().active_tent == null)) {
            if (playerContext.TentChosen()) {
                TeamManager.teleportToTent(player);
            } else {
                TeamManager.teleportToSpawn(player);
            }
        } else {
            TeamManager.teleportToSpawn(player);
        }
    }
    
    @Override
    public void HandleDeath(ServerPlayer player) {
        super.HandleDeath(player);
    }

    @Override
    public void Cleanup() {
        TeamManager.Cleanup();
    }
    @Override
    public void onServerTick(TickEvent.ServerTickEvent event) {
    }

    @Override
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    }
}
