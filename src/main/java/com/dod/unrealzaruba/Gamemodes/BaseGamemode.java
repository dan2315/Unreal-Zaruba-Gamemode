package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Gamemodes.Barriers.BarrierVolumesData;
import com.dod.unrealzaruba.Gamemodes.GamePhases.AbstractGamePhase;
import com.dod.unrealzaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.Gamemodes.GamePhases.IPhaseHolder;
import com.dod.unrealzaruba.SoundHandler.ModSounds;
import com.dod.unrealzaruba.SoundHandler.SoundHandler;
import com.dod.unrealzaruba.TeamLogic.TeamData;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.NBT;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import com.dod.unrealzaruba.ModItems.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.network.PacketDistributor;
import com.dod.unrealzaruba.NetworkPackets.UpdateDeathTimerPacket;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectivesData;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectivesHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;


public abstract class BaseGamemode implements IPhaseHolder {
    private static final int RESPAWN_DURATION_SECONDS = 10;
    protected ObjectivesHandler objectivesHandler;
    protected AbstractGamePhase currentPhase;
    // Smotri, kakuyu huinyu ya pridumal
    protected ConditionalPhase conditionalPhase;
    protected List<AbstractGamePhase> phases = new ArrayList<>();
    protected int currentPhaseIndex = 0;
    protected MinecraftServer server;
    private boolean isConditionalPhase = false;

    public BaseGamemode() {
        GamemodeManager.instance.SetActiveGamemode(this);
        new TeamData(this.getClass());
        new VehicleSpawnData(this.getClass());
        new BarrierVolumesData(this.getClass());
        this.server = ServerLifecycleHooks.getCurrentServer();
        WorldManager.ReloadGameWorldDelayed(this);
    }

    public void LateInitialize() {
        new ObjectivesData(this.getClass());
        objectivesHandler = new ObjectivesHandler();
    }

    public AbstractGamePhase GetCurrentPhase() {
        return currentPhase;
    }

    protected abstract void Initialize();
    public abstract void Cleanup();

    public abstract void HandleConnectedPlayer(Player player);
    public abstract void CheckObjectives();

    public IPhaseHolder AddPhase(AbstractGamePhase phase) {
        phases.add(phase);
        return this;
    }

    public AbstractGamePhase Build() {
        return null;
    }

    public void BeginPhase(AbstractGamePhase phase) {
        UnrealZaruba.LOGGER.info("Beginning phase " + phase.GetPhaseId());
        currentPhase = phase;
        if (phase instanceof ConditionalPhase conditionalPhase) {
            conditionalPhase.SetOnConditionMet(() -> {
                CompletePhase();
            });
            isConditionalPhase = true;
            this.conditionalPhase = conditionalPhase;
        }
        else {
            isConditionalPhase = false;
            conditionalPhase = null;
        }

        currentPhase.OnStart();
    }

    public void TransitToNextPhase() {
        currentPhaseIndex++;
        if (currentPhaseIndex < phases.size())
        {
            BeginPhase(phases.get(currentPhaseIndex));
        }
        else
        {
            UnrealZaruba.LOGGER.warn("NO MORE PHASES LEFT");
        }
    }

    public void CompletePhase() {
        currentPhase.OnCompleted();
        TransitToNextPhase();
    }

    public void CompletePhase(PhaseId phaseId) {
        if (currentPhase.GetPhaseId() == phaseId) {
            CompletePhase();
        } else {
            throw new RuntimeException("Expected phase " + phaseId + " but got " + currentPhase.GetPhaseId());
        }
    }
    
    public void ProceedToPhaseForced(PhaseId phaseId) {
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).GetPhaseId() == phaseId) {
                currentPhaseIndex = i;
                BeginPhase(phases.get(i));
                return;
            }
        }
    }
    
    public Optional<AbstractGamePhase> GetPhaseById(PhaseId phaseId) {
        return phases.stream()
                .filter(phase -> phase.GetPhaseId() == phaseId)
                .findFirst();
    }
    
    public PhaseId GetCurrentPhaseId() {
        return currentPhase != null ? currentPhase.GetPhaseId() : PhaseId.UNDEFINED;
    }

    public void StartGame() {
        CompletePhase(PhaseId.TEAM_SELECTION);
    }

    public void EndGame() {
        if (currentPhase.GetPhaseId() == PhaseId.GAME) {
            CompletePhase();
        }
    }

    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        StartGame();
        return 1;
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (objectivesHandler != null) objectivesHandler.onServerTick();
        if (isConditionalPhase) { // По сути тикают помимо этой фазы ещё ТаймедФазы, но по внутреннему таймеру
            conditionalPhase.OnTick(0);
        }
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (objectivesHandler != null) objectivesHandler.onPlayerTick(event);
    }

    public void HandleRespawn(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(false);
        CharacterClassEquipper.equipPlayerWithSelectedClass(player);
        int skulls = NBT.readEntityTag(player, "skulls");
        player.getInventory().add(new ItemStack(ModItems.SKULL.get(), skulls));
        NBT.addEntityTag(player, "isPlayerDead", 0);
        SoundHandler.playSoundToPlayer(player, ModSounds.RESPAWN2.get(), 1.0f, 1.0f);
    }

    // Помянем усопших
    public void HandleDeath(ServerPlayer player, LivingDeathEvent event) {
        if (currentPhase.GetPhaseId() == PhaseId.TEAM_SELECTION) {
            player.setHealth(20);
            player.getFoodData().setFoodLevel(20);
            event.setCanceled(true);
            return;
        }

        player.setGameMode(GameType.SPECTATOR);

        player.setHealth(20);
        player.getFoodData().setFoodLevel(20);
        NBT.addEntityTag(player, "skulls", player.getInventory().countItem(ModItems.SKULL.get()));
        player.setInvulnerable(true);
        player.getInventory().clearContent();
        event.setCanceled(true);

        var killingEntity = event.getSource().getEntity();
        if (killingEntity instanceof ServerPlayer killer) {
            Inventory killerInventory = killer.getInventory();
            killerInventory.add(new ItemStack(ModItems.SKULL.get()));
            killer.displayClientMessage(Component.literal("You have killed " + player.getDisplayName().getString() + ", got 1 SKULL"), true);
            player.displayClientMessage(Component.literal("You have been killed by " + killer.getDisplayName().getString()), true);
        }

        NBT.addEntityTag(player, "isPlayerDead", 1);
        SoundHandler.playSoundToPlayer(player, ModSounds.DEATH.get(), 1.0f, 1.0f);

        startRespawnTimer(player);
    }
    
    private void startRespawnTimer(ServerPlayer player) {
        TimerManager.createRealTimeTimer(
            RESPAWN_DURATION_SECONDS * 1000, 
            () -> HandleRespawn(player),
            ticks -> updateRespawnTimer(player, ticks)
        );
    }
    
    private void updateRespawnTimer(ServerPlayer player, int ticks) {
        if (ticks % 20 != 0) {
            return;
        }
        
        int remainingSeconds = RESPAWN_DURATION_SECONDS - (ticks / 20);
        
        NetworkHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player), 
            new UpdateDeathTimerPacket(remainingSeconds)
        );
    }
}
