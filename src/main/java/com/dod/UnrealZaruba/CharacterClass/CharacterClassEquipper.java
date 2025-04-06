package com.dod.UnrealZaruba.CharacterClass;

import java.util.List;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.CharacterClass.PowerEngineProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.registries.ForgeRegistries;

public class CharacterClassEquipper {

    public static boolean equipPlayerWithSelectedClass(ServerPlayer player) {
        if (player == null) return false;
        
        PlayerContext playerContext = PlayerContext.Get(player.getUUID());
        if (!(playerContext instanceof TeamPlayerContext teamPlayerContext)) {
            player.sendSystemMessage(Component.literal("You don't have a team player context!"));
            return false;
        }
        
        String selectedClassId = teamPlayerContext.SelectedClassId();
        if (selectedClassId == null) {
            player.sendSystemMessage(Component.literal("You haven't selected a class!"));
            return false;
        }
        
        TeamColor teamColor = teamPlayerContext.Team() != null ? 
                              teamPlayerContext.Team().Color() : 
                              TeamColor.RED;
                              
        CharacterClassData classData = CharacterClassRegistry.getCharacterClass(selectedClassId, teamColor);
        if (classData == null) {
            player.sendSystemMessage(Component.literal("Your selected class is invalid!"));
            return false;
        }
        
        // Clear inventory and prepare to apply class items
        player.getInventory().clearContent();
        List<ItemStack> kitItems = classData.getKit();
        
        ItemStack steamReactor = null;
        
        for (ItemStack item : kitItems) {
            Item itemType = item.getItem();
            
            if (itemType.toString().contains("powerengine")) {
                UnrealZaruba.LOGGER.info("Found power engine: " + itemType.getDescription().toString());
                steamReactor = item.copy();
                continue;
            }
            
            if (itemType instanceof ArmorItem) {
                ArmorItem armorItem = (ArmorItem) itemType;
                EquipmentSlot slot = armorItem.getEquipmentSlot();
                player.setItemSlot(slot, item.copy());
            }
            else if (itemType.toString().contains("pumpkin")) {
                player.setItemSlot(EquipmentSlot.HEAD, item.copy());
                UnrealZaruba.LOGGER.info("Found pumpkin: " + itemType.getDescriptionId());
            }
            else if (!player.getInventory().add(item.copy())) {
                player.drop(item.copy(), false);
            }
        }
        
        if (steamReactor != null) {
            try {
                boolean success = PowerEngineProvider.equipPlayerWithPowerEngine(player, steamReactor);
                
                if (success) {
                    UnrealZaruba.LOGGER.info("[Class Equipper] Successfully equipped " + player.getScoreboardName() + 
                                             " with Power Engine using the PowerEngineProvider");
                } else {
                    UnrealZaruba.LOGGER.warn("[Class Equipper] PowerEngineProvider failed to equip " + 
                                             player.getScoreboardName() + ", but fallback was provided");
                }
            } catch (Exception e) {
                UnrealZaruba.LOGGER.error("[Class Equipper] Error equipping Power Engine: " + e.getMessage(), e);
                if (!player.getInventory().add(steamReactor)) {
                    player.drop(steamReactor, false);
                }
                player.sendSystemMessage(Component.literal("Failed to handle Power Engine: " + e.getMessage()));
            }
        }
        
        player.sendSystemMessage(Component.literal("You are now equipped as a " + classData.getDisplayName() + "!"));
        return true;
    }

    public static int equipTeamWithSelectedClasses(List<ServerPlayer> players) {
        int successCount = 0;
        for (ServerPlayer player : players) {
            if (equipPlayerWithSelectedClass(player)) {
                successCount++;
            }
        }
        return successCount;
    }
} 