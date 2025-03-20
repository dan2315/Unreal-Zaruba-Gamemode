package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DestructibleObjectivesHandler extends ObjectivesHandler {
    private final List<DestructibleObjective> objectives = new ArrayList<>();
    private static final int OBJECTIVE_UPDATE_FREQUENCY = 40; //ticks
    private static final int BOSSBAR_UPDATE_FREQUENCY = 40; //ticks
    private static int serverTickCounter = 0;
    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();
    
    // Static instance for event handling
    private static DestructibleObjectivesHandler instance;
    
    public DestructibleObjectivesHandler() {
        instance = this;
    }

    public void add(DestructibleObjective objective) {
        objectives.add(objective);
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (instance == null) return;
        
        if (event.phase == ServerTickEvent.Phase.START) {
            serverTickCounter++;
            if (serverTickCounter % OBJECTIVE_UPDATE_FREQUENCY != 0) return; 
            instance.updateObjectives(event);
        }
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (instance == null) return;
        
        UUID id = event.player.getUUID();
        playerTickCounters.putIfAbsent(id, 0);
        playerTickCounters.put(id, playerTickCounters.get(id) + 1);
        if (playerTickCounters.get(id) % BOSSBAR_UPDATE_FREQUENCY != 0) return;

        instance.updatePlayersWithBossBar(event);
    }

    private void updateObjectives(TickEvent.ServerTickEvent event) {
        for (DestructibleObjective objective : objectives) {
            objective.Update();
        }
    }

    private void updatePlayersWithBossBar(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            for (DestructibleObjective objective : objectives) {
                IProgressDisplay progressDisplay = objective.getProgressDisplay();
                if (progressDisplay != null) {
                    progressDisplay.updatePlayerVisibility(player);
                }
            }
        }
    }

    public void setProgressBarActivationDistance(float distance) {
        for (DestructibleObjective objective : objectives) {
            IProgressDisplay progressDisplay = objective.getProgressDisplay();
            if (progressDisplay != null) {
                progressDisplay.setActivationDistance(distance);
            }
        }
    }

    public void save() {
        DestructibleObjective[] objectivesArray = objectives.toArray(new DestructibleObjective[objectives.size()]);
        for (DestructibleObjective objective : objectivesArray) {
            UnrealZaruba.LOGGER.info(objective.name);
        }
        try {
            ConfigManager.saveConfig(ConfigManager.Objectives, objectivesArray);
            UnrealZaruba.LOGGER.info("[Во, бля] Сделал конфиг");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[Ай, бля] Unable to create Config file for DestructibleObjectivesHandler");
            e.printStackTrace();
        }
    }

    public DestructibleObjective[] load(BaseGamemode containingGamemode) {
        DestructibleObjective[] loadedObjectives;
        try {
            loadedObjectives = ConfigManager.loadConfig(ConfigManager.Objectives, DestructibleObjective[].class);
            if (loadedObjectives == null) return loadedObjectives;
            clear();
            for (DestructibleObjective objective : loadedObjectives) {
                this.add(objective);
                objective.SetContainingGamemode(containingGamemode);
                // Re-initialize the progress display since it's transient
                objective.setProgressDisplay(new ProgressbarForObjective(objective, objective.GetName()));
                UnrealZaruba.LOGGER.info("[Во, бля] " + objective.name);
            }
            UnrealZaruba.LOGGER.info("[Во, бля] Загрузил конфиг для DestructibleObjectivesHandler");
            return loadedObjectives;
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[Ай, бля] Config file for DestructibleObjectivesHandler was not found");
            e.printStackTrace();
        }
        return null;
    }

    public void clear() {
        for (DestructibleObjective objective : objectives) {
            IProgressDisplay progressDisplay = objective.getProgressDisplay();
            if (progressDisplay != null) {
                progressDisplay.clear();
            }
        }
        objectives.clear();
    }
}