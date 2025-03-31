package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.Utils.IResettable;


import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DestructibleObjectivesHandler extends ObjectivesHandler implements IResettable {
    private final List<DestructibleObjective> objectives = new ArrayList<>();
    

    public DestructibleObjectivesHandler() {
    }


    public void add(DestructibleObjective objective) {
        objectives.add(objective);
    }


    public void onServerTick() {
        for (DestructibleObjective objective : objectives) {
            objective.Update();
        }

        if (objectives.stream().allMatch(DestructibleObjective::IsCompleted)) {
            onCompleted.run();
        }
    }

    @Override
    public void reset() {
        for (DestructibleObjective objective : objectives) {
            objective.reset();
        }
    }



    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        
        for (DestructibleObjective objective : objectives) {
            objective.onPlayerTick(event, player);
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

    public DestructibleObjective[] load() {
        DestructibleObjective[] loadedObjectives;
        try {
            loadedObjectives = ConfigManager.loadConfig(ConfigManager.Objectives, DestructibleObjective[].class);
            if (loadedObjectives == null) return loadedObjectives;
            clear();
            for (DestructibleObjective objective : loadedObjectives) {
                this.add(objective);
                objective.setProgressDisplay(new ProgressbarForObjective(objective, objective.GetName()));
                UnrealZaruba.LOGGER.info("[UnrealZaruba] " + "Загрузил цель: " + objective.name);
            }
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Загрузил конфиг для DestructibleObjectivesHandler");
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