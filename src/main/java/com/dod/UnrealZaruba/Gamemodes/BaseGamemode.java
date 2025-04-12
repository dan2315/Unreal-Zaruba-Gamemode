package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Gamemodes.GamePhases.AbstractGamePhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.IPhaseHolder;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.Utils.NBT;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassEquipper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.network.PacketDistributor;
import com.dod.UnrealZaruba.NetworkPackets.UpdateDeathTimerPacket;

public abstract class BaseGamemode implements IPhaseHolder {
    protected static BaseGamemode currentGamemode;
    private static final int RESPAWN_DURATION_SECONDS = 10;
    protected ResourceKey<Level> lobbyDimension;
    protected ResourceKey<Level> gameDimension;
    protected AbstractGamePhase currentPhase;
    protected List<AbstractGamePhase> phases = new ArrayList<>();
    protected int currentPhaseIndex = 0;

    public AbstractGamePhase GetCurrentPhase() {
        return currentPhase;
    }

    protected abstract void Initialize();
    public abstract void Cleanup();

    public abstract void HandleConnectedPlayer(Player player);
    public abstract void CheckObjectives();

    public void SetCurrentGamemode(BaseGamemode gamemode) {
        currentGamemode = gamemode;
    }

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
        currentPhase.OnStart();
    }

    public void TransitToNextPhase() {
        currentPhaseIndex++;
        int loopedIndex = currentPhaseIndex % phases.size();
        BeginPhase(phases.get(loopedIndex));
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
        ProceedToPhaseForced(PhaseId.TEAM_SELECTION);
    }

    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        StartGame();
        return 1;
    }
    
    public abstract void onServerTick(TickEvent.ServerTickEvent event);
    public abstract void onPlayerTick(TickEvent.PlayerTickEvent event);

    public void HandleRespawn(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(false);
        CharacterClassEquipper.equipPlayerWithSelectedClass(player);
        NBT.addEntityTag(player, "isPlayerDead", 0);
        SoundHandler.playSoundToPlayer(player, ModSounds.RESPAWN2.get(), 1.0f, 1.0f);
    }
    
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
        player.setInvulnerable(true);
        player.getInventory().clearContent();
        event.setCanceled(true);

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
