package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.ArrayList;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Config.DestructibleObjectivesConfig;
import com.dod.UnrealZaruba.Utils.IResettable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DestructibleObjectivesHandler extends ObjectivesHandler implements IResettable {
    private final List<DestructibleObjective> objectives = new ArrayList<>();
    
    private boolean allCompleted;


    public DestructibleObjectivesHandler() {
        allCompleted = true;
    }

    public void onServerTick() {
        objectives.forEach(objective -> objective.Update());
        if (allCompleted) return;
        if (objectives.stream().allMatch(GameObjective::IsCompleted)) {
            UnrealZaruba.LOGGER.info("All objectives completed");
            allCompleted = true;
            onCompleted.run();
        }
    }

    public void add(DestructibleObjective objective) {
        objectives.add(objective);
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
        
        DestructibleObjectivesConfig.getInstance().saveObjectives(objectivesArray);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved destructible objectives configuration");
    }

    public DestructibleObjective[] load() {
        DestructibleObjective[] loadedObjectives = DestructibleObjectivesConfig.getInstance().loadObjectives();
        if (loadedObjectives != null && loadedObjectives.length > 0) {
            allCompleted = false;
            clear();
            for (DestructibleObjective objective : loadedObjectives) {
                this.add(objective);
                objective.setProgressDisplay(new ProgressbarForObjective(objective, objective.GetName()));
                UnrealZaruba.LOGGER.info("[UnrealZaruba] " + "Loaded objective: " + objective.name);
            }
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Loaded configuration for DestructibleObjectivesHandler");
        } else {
            UnrealZaruba.LOGGER.info("[UnrealZaruba] No destructible objectives found or empty configuration");
        }
        return loadedObjectives;
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