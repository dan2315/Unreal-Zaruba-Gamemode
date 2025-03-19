package com.dod.UnrealZaruba.TeamLogic;

import java.util.*;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.ModBlocks.Tent.Tent;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.TeamItemKits.ItemKits;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;

/**
 * Team core data.
 */
public class TeamContext {
    private BlockPos spawn;
    public BlockPos tentSpawn;
    private List<BlockVolume> barrierVolumes = new ArrayList<BlockVolume>();
    private UUID commander;
    private String commanderName;
    List<UUID> members = new ArrayList<>();
    public TeamColor color;
    MinecraftServer server;
    TeamManager batya;
    public Tent active_tent;

    public PlayerTeam minecraftTeam;

    public TeamColor Color() {return color;}
    public BlockPos Spawn() {return spawn;}
    public UUID Commander() {return commander;}
    public String CommanderName() {return commanderName;}
    public List<UUID> Members() {return members;}
    public List<BlockVolume> BarrierVolumes() {return barrierVolumes;}

    /**
     * Instantiates a new Team u.
     *
     * @param teamManager    the team manager
     * @param spawn          the spawn
     * @param color          the color
     * @param barrierVolumes the barrier volumes
     */
    public TeamContext(TeamManager teamManager, BlockPos spawn, TeamColor color, List<BlockVolume> barrierVolumes) {
        this.batya = teamManager;
        this.spawn = spawn;
        this.color = color;
        this.barrierVolumes = barrierVolumes;
        server = ServerLifecycleHooks.getCurrentServer();
    }

    public TeamContext(TeamManager teamManager, BlockPos spawn, TeamColor color) {
        this.batya = teamManager;
        this.spawn = spawn;
        this.color = color;
        server = ServerLifecycleHooks.getCurrentServer();
    }

    public void AddBarrierVolume(BlockVolume barrierVolume) {
        if (barrierVolumes == null) barrierVolumes = new ArrayList<BlockVolume>();
        barrierVolumes.add(barrierVolume);
    }

    public void setActiveTent(Tent active_tent) {
        this.active_tent = active_tent;
    }


    public void GiveVote(Player player ,PlayerContext playerContext) {

        playerContext.AddVote();
    }

    public List<UUID> VoteList() {

        Collections.sort(members, new Comparator<UUID>() {
            @Override
            public int compare(UUID o1, UUID o2) {
                int votes1 = PlayerContext.Get(o1).Votes();
                int votes2 = PlayerContext.Get(o2).Votes();
                return Integer.compare(votes2, votes1); // Descending order
            }
        });

        return members;
    }

    public void setCommander(MinecraftServer server, Player player) {

        if (player instanceof ServerPlayer serverPlayer) {
            ItemKits.GiveCommanderKit(server, serverPlayer, batya.GetPlayersTeam(serverPlayer));
            commander = serverPlayer.getUUID();
            commanderName = serverPlayer.getDisplayName().getString();
        }

    }

    public void SendMessage(MinecraftServer server, String message) {
        this.members.forEach(element -> {
            ServerPlayer player = server.getPlayerList().getPlayer(element);
            if (player != null) {
                player.sendSystemMessage(Component.literal(message));
            }
        });
    }

    public void SetupMinecraftTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        minecraftTeam = scoreboard.getPlayerTeam(color.getColorCode());

        minecraftTeam.setColor(color.getChatFormatting());
        minecraftTeam.setDisplayName(Component.literal(color.getDisplayName()));
        minecraftTeam.setNameTagVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
        minecraftTeam.setDeathMessageVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
    }

    public void CleanMinecraftTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        scoreboard.removePlayerTeam(minecraftTeam);
    }

    /**
     * Assign player into a scoreboard team
     *
     * @param player the player
     */
    public void Assign(ServerPlayer player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();

        if (spawn == null) {
            player.sendSystemMessage(Component.literal("Скажи Доду, что он забыл спавн поставить))"));
        } else {
            members.add(player.getUUID());
            scoreboard.addPlayerToTeam(player.getName().getString(), minecraftTeam);

            player.displayClientMessage(
                    Component.literal("Вы присоединились к команде " + color.getDisplayName() + "!")
                            .withStyle(color.getChatFormatting()),
                    true);
            player.setRespawnPosition(player.level().dimension(), spawn, 0, false, false);
            player.getInventory().clearContent();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            batya.GiveArmorKitTo(server, player);
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

    public StructureTemplate GetTentTemplate(StructureTemplateManager structureManager) {
        return structureManager.getOrCreate(new ResourceLocation("unrealzaruba", "blue_tent"));
        
    }

    public void ProcessWin() {
        
    }

    public void ProcessLose() {
        
    }

    public void TeleportToSpawn()
    {
        for (UUID playerId : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) return;
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        }
    }

    public void GiveKit() {
        for (UUID playerId : members) {
            ItemKits.GiveKit(server, server.getPlayerList().getPlayer(playerId), this);
        }
    }
}
