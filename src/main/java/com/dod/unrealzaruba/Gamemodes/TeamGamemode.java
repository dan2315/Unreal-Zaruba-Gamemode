package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.GameText.StartGameText;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.TeamLogic.TeamManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;

public class TeamGamemode extends BaseGamemode {
    protected TeamManager TeamManager;
    public HashMap<TeamColor, StartGameText> startGameTexts = new HashMap<>();
    
    public TeamManager GetTeamManager() { return TeamManager; }
    public void SetTeamManager(TeamManager teamManager) { TeamManager = teamManager; }
    
    public TeamGamemode() {
        super();
        TeamManager = new TeamManager();
    }

    @Override
    protected void Initialize() {
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
        if (playerContext.RespawnPointSelected()) {
            TeamManager.teleportToSelectedPoint(player);
        } else {
            TeamManager.teleportToSpawnByPriority(player);
        }
    }
    
    @Override
    public void HandleDeath(ServerPlayer player, LivingDeathEvent event) {
        super.HandleDeath(player, event);
        if (PlayerContext.Get(player.getUUID()) instanceof TeamPlayerContext teamPlayerContext) {
            var respawnPoints = teamPlayerContext.Team().RespawnPoints();
            NetworkHandler.Screens.openDeathScreen(player, respawnPoints);
        }
    }

    @Override
    public void Cleanup() {
        TeamManager.Cleanup();
    }

    @Override
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        super.onPlayerTick(event);
    }
}
