package com.dod.UnrealZaruba.Gamemodes.Objectives;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DestructibleObjectivesHandler {
    public static final Map<DestructibleObjective, ServerBossEvent> destructibleObjectives = new HashMap<>();
    private static final Map<DestructibleObjective, Set<UUID>> playersWithBossBar = new HashMap<>();
    private static float PROGRESSBAR_ACTIVATION_DISTANCE = 10000f;
    private static final int BossbarUpdateFrequency = 40; //ticks
    private static final int ObjectiveUpdateFrequency = 40; //ticks
    private static int serverTickCounter = 0;
    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    public static void Add(DestructibleObjective objective) {
        destructibleObjectives.put(objective, new ServerBossEvent(Component.literal(objective.GetName()),
                BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS));
        playersWithBossBar.put(objective, new HashSet<>());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == ServerTickEvent.Phase.START) {
            serverTickCounter++;
            if (serverTickCounter % ObjectiveUpdateFrequency != 0) return; 
            UpdateObjectives(event);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        UUID id = event.player.getUUID();
        playerTickCounters.putIfAbsent(id, 0);
        playerTickCounters.put(id, playerTickCounters.get(id) + 1);
        if (playerTickCounters.get(id) % BossbarUpdateFrequency != 0) return;

        UpdatePlayersWithBossBar(event);
    }

    private static void UpdateObjectives(TickEvent.ServerTickEvent event) {
        for (var objective : destructibleObjectives.keySet()) {
            float progress = objective.Update();
            updateBossBar(objective, progress);
        }
    }

    private static void UpdatePlayersWithBossBar(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            for (DestructibleObjective objective : destructibleObjectives.keySet()) {

                boolean isNearTarget = isPlayerNearTarget(player, objective.volume.GetCenter());

                if (isNearTarget && !playersWithBossBar.get(objective).contains(player.getUUID())) {
                    addPlayerToBossBar(player, objective);
                } else if (!isNearTarget && playersWithBossBar.get(objective).contains(player.getUUID())) {
                    removePlayerFromBossBar(player, objective);
                }
            }
        }
    }


    private static boolean isPlayerNearTarget(ServerPlayer player, BlockPos pos) {
        double distance = player.blockPosition().distSqr(pos);
        return distance <= PROGRESSBAR_ACTIVATION_DISTANCE;
    }

    private static void addPlayerToBossBar(ServerPlayer player, DestructibleObjective objective) {
        destructibleObjectives.get(objective).addPlayer(player);
        playersWithBossBar.get(objective).add(player.getUUID());
    }

    private static void removePlayerFromBossBar(ServerPlayer player, DestructibleObjective objective) {
        destructibleObjectives.get(objective).removePlayer(player);
        playersWithBossBar.get(objective).remove(player.getUUID());
    }

    public static void updateBossBar(DestructibleObjective objective, float progress) {
        destructibleObjectives.get(objective).setProgress(progress);
    }

    public static void Save() {
        DestructibleObjective[] objectivesArray = destructibleObjectives.keySet().toArray(new DestructibleObjective[destructibleObjectives.size()]);
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

    public static DestructibleObjective[] Load(BaseGamemode containingGamemode) {
        DestructibleObjective[] loadedObjectives;
        try {
            loadedObjectives = ConfigManager.loadConfig(ConfigManager.Objectives, DestructibleObjective[].class);
            if (loadedObjectives == null) return loadedObjectives;
            Clear();
            for (DestructibleObjective objective : loadedObjectives) {
                DestructibleObjectivesHandler.Add(objective);
                objective.SetContainingGamemode(containingGamemode);
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

    public static void Clear() {
        destructibleObjectives.clear();
        playersWithBossBar.clear();
    }
}