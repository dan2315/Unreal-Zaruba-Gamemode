package com.dod.unrealzaruba.CharacterClass;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

// Убейте меня нахуй
public class PowerEngineProvider {
    private static final String TEMPLATE_TAG = "power_engine_template";
    private static final double TEMPLATE_HEIGHT_OFFSET = 300.0; // Far above world height to be out of sight
    private static final Map<MinecraftServer, Boolean> templateInitialized = new HashMap<>();

    public static boolean ensureTemplateExists(MinecraftServer server) {
        if (server == null) return false;
        
        if (templateInitialized.getOrDefault(server, false)) {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(4);
            
            try {
                String testCommand = "execute if entity @e[tag=" + TEMPLATE_TAG + ",limit=1] run say found";
                int result = server.getCommands().performPrefixedCommand(commandSource, testCommand);
                if (result > 0) {
                    return true;
                }
                templateInitialized.put(server, false);
            } catch (Exception e) {
                UnrealZaruba.LOGGER.error("[PowerEngineProvider] Error checking template existence: " + e.getMessage());
                templateInitialized.put(server, false);
            }
        }
        
        if (!templateInitialized.getOrDefault(server, false)) {
            Vec3 spawnPos = Vec3.atBottomCenterOf(server.getLevel(server.overworld().dimension()).getSharedSpawnPos());
            int templateX = (int)Math.floor(spawnPos.x());
            int templateY = (int)Math.floor(TEMPLATE_HEIGHT_OFFSET);
            int templateZ = (int)Math.floor(spawnPos.z());
            
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(4);
            
            try {
                // First, ensure the area is loaded
                String loadCommand = String.format("forceload add %d %d", templateX >> 4, templateZ >> 4);
                server.getCommands().performPrefixedCommand(commandSource, loadCommand);
                
                // Clear any existing templates
                String cleanupCommand = "kill @e[tag=" + TEMPLATE_TAG + "]";
                server.getCommands().performPrefixedCommand(commandSource, cleanupCommand);
                
                // Create a platform to support the item frame
                String platformCommand = String.format(
                    "fill %d %d %d %d %d %d minecraft:bedrock", 
                    templateX - 1, templateY - 1, templateZ - 1,
                    templateX + 1, templateY - 1, templateZ + 1
                );
                server.getCommands().performPrefixedCommand(commandSource, platformCommand);
                
                // Summon the item frame with the Power Engine
                String summonCommand = String.format(
                    "summon minecraft:glow_item_frame %d %d %d {Invisible:1b,Fixed:1b,Invulnerable:1b,Tags:[\"%s\"],Item:{id:\"protection_pixel:powerengine\",Count:1b,tag:{count:0.0d,Inventory:{Size:9,Items:[" +
                    "{Slot:0,id:\"protection_pixel:watertank\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:1,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:2,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:3,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:4,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:5,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:6,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:7,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                    "{Slot:8,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}" +
                    "]}}}}",
                    templateX, templateY, templateZ, TEMPLATE_TAG
                );
                
                UnrealZaruba.LOGGER.info("[PowerEngineProvider] Creating Power Engine template: " + summonCommand);
                int result = server.getCommands().performPrefixedCommand(commandSource, summonCommand);
                
                if (result > 0) {
                    // Add ForgeCaps data to the item frame
                    String setForgeCapsCommand = String.format(
                        "data modify entity @e[tag=%s,limit=1,sort=nearest] Item.ForgeCaps.Parent.Items set value [" +
                        "{Slot:0,id:\"protection_pixel:watertank\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:1,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:2,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:3,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:4,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:5,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:6,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:7,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}," +
                        "{Slot:8,id:\"protection_pixel:flarerod\",Count:1b,tag:{Damage:0}}" +
                        "]",
                        TEMPLATE_TAG
                    );
                    
                    UnrealZaruba.LOGGER.info("[PowerEngineProvider] FORGECAPS COMMAND: " + setForgeCapsCommand);
                    int forgeCapsResult = server.getCommands().performPrefixedCommand(commandSource, setForgeCapsCommand);
                    
                    if (forgeCapsResult > 0) {
                        UnrealZaruba.LOGGER.info("[PowerEngineProvider] ForgeCaps set successfully");
                    } else {
                        UnrealZaruba.LOGGER.warn("[PowerEngineProvider] Failed to set ForgeCaps for Power Engine template");
                        UnrealZaruba.LOGGER.warn("[PowerEngineProvider] Failed ForgeCaps command was: " + setForgeCapsCommand);
                    }
                    
                    templateInitialized.put(server, true);
                    UnrealZaruba.LOGGER.info("[PowerEngineProvider] Power Engine template created successfully");
                    return true;
                } else {
                    UnrealZaruba.LOGGER.error("[PowerEngineProvider] Failed to create Power Engine template");
                    return false;
                }
            } catch (Exception e) {
                UnrealZaruba.LOGGER.error("[PowerEngineProvider] Error creating template: " + e.getMessage());
                return false;
            }
        }
        
        return templateInitialized.getOrDefault(server, false);
    }

    public static boolean equipPlayerWithPowerEngine(ServerPlayer player, ItemStack fallbackEngine) {
        if (player == null) return false;
        
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        
        String playerName = player.getDisplayName().getString();
        
        if (!ensureTemplateExists(server)) {
            UnrealZaruba.LOGGER.warn("[PowerEngineProvider] Could not ensure template exists for " + playerName);
            if (fallbackEngine != null) {
                if (!player.getInventory().add(fallbackEngine.copy())) {
                    player.drop(fallbackEngine.copy(), false);
                }
                player.sendSystemMessage(Component.literal("§6[SYSTEM] §eA Power Engine has been added to your inventory. Please equip it in your power slot."));
            }
            return false;
        }
        
        CommandSourceStack commandSource = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withPermission(4);
        
        try {
            // Use the correct command syntax with entity approach + container.0
            String copyCommand = String.format(
                "curios replace power 0 %s from entity-vanilla @e[tag=%s,limit=1] container.0",
                playerName, TEMPLATE_TAG
            );
            
            UnrealZaruba.LOGGER.info("[PowerEngineProvider] COMMAND: " + copyCommand);
            UnrealZaruba.LOGGER.info("[PowerEngineProvider] Copying Power Engine to " + playerName + " curios slot");
            int copyResult = server.getCommands().performPrefixedCommand(commandSource, copyCommand);
            
            if (copyResult > 0) {
                UnrealZaruba.LOGGER.info("[PowerEngineProvider] Successfully equipped Power Engine for: " + playerName);
                return true;
            } else {
                // Try with alternative command syntax
                String fallbackCommand = String.format(
                    "execute as @e[tag=%s,limit=1] run curios replace power 0 %s from entity-vanilla @s container.0",
                    TEMPLATE_TAG, playerName
                );
                
                UnrealZaruba.LOGGER.info("[PowerEngineProvider] Trying fallback command: " + fallbackCommand);
                int fallbackResult = server.getCommands().performPrefixedCommand(commandSource, fallbackCommand);
                
                if (fallbackResult > 0) {
                    player.sendSystemMessage(Component.literal("§6[SYSTEM] §eA Power Engine has been equipped in your power slot with all components."));
                    UnrealZaruba.LOGGER.info("[PowerEngineProvider] Successfully equipped Power Engine using fallback command for: " + playerName);
                    return true;
                } else {
                    // Try the item approach as last resort
                    String lastResortCommand = String.format(
                        "execute as @e[tag=%s,limit=1] run curios replace power 0 %s from entity-vanilla @s item",
                        TEMPLATE_TAG, playerName
                    );
                    
                    UnrealZaruba.LOGGER.info("[PowerEngineProvider] Trying last resort command: " + lastResortCommand);
                    int lastResortResult = server.getCommands().performPrefixedCommand(commandSource, lastResortCommand);
                    
                    if (lastResortResult > 0) {
                        player.sendSystemMessage(Component.literal("§6[SYSTEM] §eA Power Engine has been equipped in your power slot with all components."));
                        UnrealZaruba.LOGGER.info("[PowerEngineProvider] Successfully equipped Power Engine using last resort command for: " + playerName);
                        return true;
                    } else {
                        UnrealZaruba.LOGGER.warn("[PowerEngineProvider] Failed to copy Power Engine from template for " + playerName);
                        UnrealZaruba.LOGGER.warn("[PowerEngineProvider] Failed commands were:");
                        UnrealZaruba.LOGGER.warn(" - " + copyCommand);
                        UnrealZaruba.LOGGER.warn(" - " + fallbackCommand);
                        UnrealZaruba.LOGGER.warn(" - " + lastResortCommand);
                        
                        if (fallbackEngine != null) {
                            if (!player.getInventory().add(fallbackEngine.copy())) {
                                player.drop(fallbackEngine.copy(), false);
                            }
                            player.sendSystemMessage(Component.literal("§6[SYSTEM] §eA Power Engine has been added to your inventory. Please equip it in your power slot."));
                        }
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("[PowerEngineProvider] Error equipping Power Engine: " + e.getMessage());
            if (fallbackEngine != null) {
                if (!player.getInventory().add(fallbackEngine.copy())) {
                    player.drop(fallbackEngine.copy(), false);
                }
                player.sendSystemMessage(Component.literal("§6[SYSTEM] §eA Power Engine has been added to your inventory. You can manually equip it in your power slot."));
            }
            return false;
        }
    }

    public static void clearAllTemplates() {
        for (MinecraftServer server : templateInitialized.keySet()) {
            try {
                CommandSourceStack commandSource = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(4);
                
                server.getCommands().performPrefixedCommand(commandSource, "kill @e[tag=" + TEMPLATE_TAG + "]");
                
                // Also unload any chunks we may have force-loaded
                Vec3 spawnPos = Vec3.atBottomCenterOf(server.getLevel(server.overworld().dimension()).getSharedSpawnPos());
                int templateX = (int)Math.floor(spawnPos.x());
                int templateZ = (int)Math.floor(spawnPos.z());
                String unloadCommand = String.format("forceload remove %d %d", templateX >> 4, templateZ >> 4);
                server.getCommands().performPrefixedCommand(commandSource, unloadCommand);
            } catch (Exception e) {
                UnrealZaruba.LOGGER.error("[PowerEngineProvider] Error during cleanup: " + e.getMessage());
            }
        }
        templateInitialized.clear();
    }

    public static boolean recreateTemplate(MinecraftServer server) {
        if (server == null) return false;
        
        templateInitialized.put(server, false);
        return ensureTemplateExists(server);
    }
} 