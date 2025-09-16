package com.dod.UnrealZaruba.TeamLogic;

import java.util.*;
import java.util.function.BiConsumer;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.Objectives.GameObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.ObjectiveOwner;
import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.IRespawnPoint;
import com.dod.UnrealZaruba.Gamemodes.RespawnPoints.RespawnPoint;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import com.dod.UnrealZaruba.Utils.IResettable;
import com.dod.UnrealZaruba.WorldManager.WorldManager;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;  

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.sounds.SoundSource;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.sounds.SoundEvent;
/**
 * Team core data.
 */
public class TeamContext extends ObjectiveOwner implements IResettable {
    private final TeamManager batya;
    private final ResourceLocation tentTemplate;
    private final SoundEvent hornSound;
    
    private final TeamColor color;
    private final MinecraftServer server;
    
    private List<IRespawnPoint> respawnPoints = new ArrayList<>();
    private UUID commander;
    private String commanderName;
    private PlayerTeam minecraftTeam;
    
    private BiConsumer<GameObjective, ObjectiveOwner> onObjectiveChangedNotification;
    private BiConsumer<GameObjective, ObjectiveOwner> onObjectiveCompletedNotification;
    
    public final List<UUID> members = new ArrayList<>();
    public TeamColor Color() {return color;}
    public UUID Commander() {return commander;}
    public String CommanderName() {return commanderName;}
    public List<UUID> Members() {return members;}
    public BlockPos MainSpawn() {return respawnPoints.get(0).getSpawnPosition();}
    public List<IRespawnPoint> RespawnPoints() {return respawnPoints;}

    public TeamContext(TeamManager teamManager, BlockPos spawnPosition, TeamColor color) {
        this.batya = teamManager;
        AddRespawnPoint(new RespawnPoint(spawnPosition, "База", 0));
        this.color = color;
        server = ServerLifecycleHooks.getCurrentServer();
        
        TeamAssets assets = TeamAssets.getByTeamColor(color);
        this.tentTemplate = assets.getTentTemplate();
        this.hornSound = assets.getHornSound();
    }

    public int GetIntColor() {
        switch (color) {
            case RED -> {
                return 0xFF0000;
            }
            case BLUE -> {
                return 0x0000FF;
            }
            case YELLOW -> {
                return  0x00FFFF;
            }
            default -> {
                return 0xFFFFFF;
            }
        }
    }

    public void setOnObjectiveChangedNotification(BiConsumer<GameObjective, ObjectiveOwner> consumer) {
        if (consumer != null) {
            this.onObjectiveChangedNotification = consumer;
        }
    }

    public void setOnObjectiveCompletedNotification(BiConsumer<GameObjective, ObjectiveOwner> consumer) {
        if (consumer != null) {
            this.onObjectiveCompletedNotification = consumer;
        }
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

    public void AddRespawnPoint(IRespawnPoint respawnPoint) {
        respawnPoints.add(respawnPoint);
    }

    public void RemoveRespawnPointByBlockPos(BlockPos blockPos) {
        if (respawnPoints.removeIf(respawnPoint -> respawnPoint.getSpawnPosition().equals(blockPos))) {
            UnrealZaruba.LOGGER.info("Respawn point removed");
        } else {
            UnrealZaruba.LOGGER.info("Respawn point not found");
        }
    }

    public void setCommander(MinecraftServer server, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // ItemKits.GiveCommanderKit(server, serverPlayer, batya.GetPlayersTeam(serverPlayer));
            commander = serverPlayer.getUUID();
            commanderName = serverPlayer.getDisplayName().getString();
        }
    }

    public void SendMessage(String message) {
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
        if (minecraftTeam == null) {
            minecraftTeam = scoreboard.addPlayerTeam(color.getColorCode());
        }

        minecraftTeam.setColor(color.getChatFormatting());
        minecraftTeam.setDisplayName(Component.literal(color.getDisplayName()));
        minecraftTeam.setNameTagVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
        minecraftTeam.setDeathMessageVisibility(Visibility.HIDE_FOR_OTHER_TEAMS);
    }

    public void CleanMinecraftTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        scoreboard.removePlayerTeam(minecraftTeam);
    }

    public void Assign(ServerPlayer player) {
        UnrealZaruba.LOGGER.info("Assigning player to team: ");
        Scoreboard scoreboard = player.getServer().getScoreboard();

        if (respawnPoints.isEmpty()) {
            player.sendSystemMessage(Component.literal("Скажи Доду, что он забыл спавн поставить))"));
        } else {
            members.add(player.getUUID());
            ((TeamPlayerContext) PlayerContext.Get(player.getUUID())).SetTeam(this);
            scoreboard.addPlayerToTeam(player.getName().getString(), minecraftTeam);

            player.displayClientMessage(
                    Component.literal("Вы присоединились к команде " + color.getDisplayName() + "!")
                            .withStyle(color.getChatFormatting()),
                    true);
            player.setRespawnPosition(player.level().dimension(), MainSpawn(), 0, false, false);
            player.getInventory().clearContent();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            // batya.GiveArmorKitTo(server, player);
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

    public void TeleportAll(BlockPos pos) {
        for (UUID playerId : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
    public StructureTemplate GetTentTemplate(StructureTemplateManager structureManager) {
        return structureManager.getOrCreate(tentTemplate);
    }

    public void ProcessWin() {
        
    }

    public void ProcessLose() {
        
    }

    public void PlayBattleSound() {
        ServerLevel serverLevel = server.getLevel(WorldManager.GAME_DIMENSION);
        SoundHandler.playSoundFromPosition(serverLevel, respawnPoints.get(0).getSpawnPosition(), hornSound, SoundSource.BLOCKS, 5.0F, 1.0F);
    }

    @Override
    public void onObjectiveStateChanged(GameObjective objective) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || onObjectiveChangedNotification == null) return;
        
        onObjectiveChangedNotification.accept(objective, this);
    }

    @Override
    public void onObjectiveCompleted(GameObjective objective) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || onObjectiveCompletedNotification == null) return;
        
        onObjectiveCompletedNotification.accept(objective, this);
    }

    @Override
    public void reset() {
        for (UUID playerId : members) {
            ((TeamPlayerContext) PlayerContext.Get(playerId)).reset();
        }
        server.getScoreboard().removePlayerTeam(minecraftTeam); // I hope that would work
        SetupMinecraftTeam(server);
        members.clear();
        commander = null;
        commanderName = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TeamContext teamContext) {
            return this.Color() == teamContext.Color();
        }
        return super.equals(obj);
    }

}
