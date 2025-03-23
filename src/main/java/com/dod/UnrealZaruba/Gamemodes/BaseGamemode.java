package com.dod.UnrealZaruba.Gamemodes;

import com.dod.UnrealZaruba.Gamemodes.GamePhases.IGamePhase;
import com.dod.UnrealZaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.Utils.NBT;
import com.dod.UnrealZaruba.Utils.Timers.TimerManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

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


public abstract class BaseGamemode {
    protected static BaseGamemode currentGamemode;
    private static final int RESPAWN_DURATION_SECONDS = 10;
    protected ResourceKey<Level> lobbyDimension;
    protected ResourceKey<Level> gameDimension;
    protected IGamePhase currentPhase;
    protected List<IGamePhase> phases = new ArrayList<>();
    protected int currentPhaseIndex = 0;

    public GameStage gameStage = GameStage.Preparation;

    protected abstract void Initialize();
    public abstract void Cleanup();

    public abstract void HandleConnectedPlayer(Player player);
    public abstract void CheckObjectives();
    public abstract int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

    public void SetCurrentGamemode(BaseGamemode gamemode) {
        currentGamemode = gamemode;
    }

    protected BaseGamemode AddPhase(IGamePhase phase) {
        phases.add(phase);
        return this;
    }

    protected BaseGamemode ProceedToNextPhase() {
        if (currentPhaseIndex < phases.size() - 1) {
            currentPhaseIndex++;
            currentPhase = phases.get(currentPhaseIndex);
        }
        return this;
    }

    protected BaseGamemode ProceedToNextPhase(PhaseId phaseId) {
        if (currentPhaseIndex < phases.size() - 1) {
            currentPhaseIndex++;
            var nextPhase = phases.get(currentPhaseIndex);
            if (nextPhase.GetPhaseId() == phaseId) {
                currentPhase = nextPhase;
            } else {
                throw new RuntimeException("Expected phase " + phaseId + " but got " + nextPhase.GetPhaseId());
            }
        }
        return this;
    }
    
    protected BaseGamemode ProceedToPhaseForced(PhaseId phaseId) {
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).GetPhaseId() == phaseId) {
                currentPhaseIndex = i;
                currentPhase = phases.get(i);
                return this;
            }
        }
        return this;
    }
    
    protected Optional<IGamePhase> GetPhaseById(PhaseId phaseId) {
        return phases.stream()
                .filter(phase -> phase.GetPhaseId() == phaseId)
                .findFirst();
    }
    
    public PhaseId GetCurrentPhaseId() {
        return currentPhase != null ? currentPhase.GetPhaseId() : PhaseId.UNDEFINED;
    }

    public void StartGame() {

    }
    public abstract void onServerTick(TickEvent.ServerTickEvent event);
    public abstract void onPlayerTick(TickEvent.PlayerTickEvent event);

    public void HandleRespawn(ServerPlayer player) {
        player.setGameMode(GameType.ADVENTURE);
        NBT.addEntityTag(player, "isPlayerDead", 0);
        SoundHandler.playSoundToPlayer(player, ModSounds.RESPAWN2.get(), 1.0f, 1.0f);
    }
    
    public void HandleDeath(ServerPlayer player) {                    
        player.setGameMode(GameType.SPECTATOR);
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
        
        // TODO: NetworkHandler.CHANNEL.send(
        //     PacketDistributor.PLAYER.with(() -> player), 
        //     new UpdateDeathTimerPacket(remainingSeconds)
        // );
    }
}
