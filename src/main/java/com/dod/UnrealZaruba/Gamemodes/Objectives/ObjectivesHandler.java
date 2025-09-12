package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.UnrealZaruba.Utils.IResettable;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import java.util.HashMap;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;


import com.mojang.datafixers.types.templates.Check;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ObjectivesHandler implements IResettable {
    private List<GameObjective> objectives;
    
    private boolean allCompleted = true;
    protected Runnable onCompleted;

    public ObjectivesHandler() {
        load();
        this.allCompleted = true;
    }

    public void OnObjectivesCompleted(Runnable onCompleted) {
        this.onCompleted = onCompleted;
    }

    public void initialize() {
    }

    public void onServerTick() {
        objectives.forEach(GameObjective::Update);
    }

    public void addObjective(GameObjective objective) {
        objectives.add(objective);
        allCompleted = false;
    }

    public void removeObjective(GameObjective objective) {
        objectives.remove(objective);
    }

    public void addRecipients(HashMap<TeamColor, TeamContext> recipients) {
        for (TeamContext recipient : recipients.values()) {
            for (GameObjective objective : objectives) {
                objective.addNotificationRecipient(recipient);
            }
        }
    }

    @Override
    public void reset() {
        for (GameObjective objective : objectives) {
            objective.Reset();
        }
        allCompleted = objectives.isEmpty();
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        
        for (GameObjective objective : objectives) {
            if (objective instanceof IPlayerTickAware playerTickAware) {
                playerTickAware.onPlayerTick(event, player);
            }
        }
    }

    public void CheckIfAllCompleted(GameObjective currentObjective) {
        var allCompleted = objectives.stream().allMatch(GameObjective::IsCompleted);
        if (allCompleted) onCompleted.run();
    }

    public void save(String configName) {
        GameObjective[] objectivesArray = objectives.toArray(new GameObjective[0]);
        for (GameObjective objective : objectivesArray) {
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Saving objective: " + objective.GetName());
        }
        
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved objectives data: " + configName);
    }

    public List<GameObjective> load() {
        BaseGamemode activeGamemode = GamemodeManager.instance.GetActiveGamemode();
        ObjectivesData objectivesData = GamemodeDataManager.getHandler(activeGamemode.getClass(), ObjectivesData.class);
        objectives = Arrays.asList(objectivesData.getObjectives());
        objectives.forEach(gameObjective -> gameObjective.SubscribeOnCompleted(this::CheckIfAllCompleted));

        return objectives;
    }

    public void clear() {
        for (GameObjective objective : objectives) {
            IProgressDisplay progressDisplay = objective.getProgressDisplay();
            if (progressDisplay != null) {
                progressDisplay.clear();
            }
        }
        objectives.clear();
        allCompleted = true;
    }
    
    public List<GameObjective> getObjectives() {
        return objectives;
    }
    
    public boolean isAllCompleted() {
        return allCompleted;
    }
}
