package com.dod.unrealzaruba.OtherModTweaks.ProtectionPixel;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Equipable;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import java.util.*;

public class ArmorBalancer {
    private static final Map<String, ArmorStats> ARMOR_STATS = new HashMap<>();
    
    static {
        //Default leggings
        registerArmorStats("protection_pixel:anchorpoint_leggings")
            .setVanillaStats(3.0f, 1.0f, 7.0f)
            .setAttribute(ProtectionPixelArmorAttributes.FALL_DAMAGE_REDUCTION, 0.5f);

        registerArmorStats("protection_pixel:anchorpointas_leggings")
            .setVanillaStats(3.0f, 1.0f, 7.0f)
            .setAttribute(ProtectionPixelArmorAttributes.FALL_DAMAGE_REDUCTION, 0.5f);
            
        //Default chestplate
        registerArmorStats("protection_pixel:breaker_chestplate")
            .setVanillaStats(8.0f, 2.0f, 0.05f)
            .setAdditionalVanillaStats(1f, 1f);
            
        registerArmorStats("protection_pixel:breakeras_chestplate")
            .setVanillaStats(6.0f, 1.5f, 0.0f)
            .setAdditionalVanillaStats(1f, 1f);
        
        //Vegetable Warrior
        registerArmorStats("protection_pixel:hunter_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.ARROW_MARKING, 1f)
            .setAttribute(ProtectionPixelArmorAttributes.DYNAMIC_ENTITY_PERCEPTION, 1f);
            
        registerArmorStats("protection_pixel:hunteras_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.ARROW_MARKING, 1f)
            .setAttribute(ProtectionPixelArmorAttributes.DYNAMIC_ENTITY_PERCEPTION, 1f);

        // Ueban s valinoy
            // No specific armor

        // Marksman
        registerArmorStats("protection_pixel:closed_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.MAGIC_DAMAGE_REDUCTION, 0.3f)
            .setAttribute(ProtectionPixelArmorAttributes.BURN_REDUCTION, 0.5f)
            .setAttribute(ProtectionPixelArmorAttributes.BREATH_IN_SWIMMING, 1f);

        registerArmorStats("protection_pixel:closedas_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.MAGIC_DAMAGE_REDUCTION, 0.3f)
            .setAttribute(ProtectionPixelArmorAttributes.BURN_REDUCTION, 0.5f)
            .setAttribute(ProtectionPixelArmorAttributes.BREATH_IN_SWIMMING, 1f);

        // Engineer
        registerArmorStats("protection_pixel:bloodprisoner_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.HEALTH_CONVERSION, 0.3f);

        registerArmorStats("protection_pixel:bloodprisoneras_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.HEALTH_CONVERSION, 0.3f);

        // Soldier
        registerArmorStats("protection_pixel:hammer_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f);

        registerArmorStats("protection_pixel:hammeras_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f);

        // Demolisher
        registerArmorStats("protection_pixel:lancer_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.HIGH_SPEED_DAMAGE_INCREASE, 0.5f);

        registerArmorStats("protection_pixel:lanceras_helmet")
            .setVanillaStats(3.0f, 1.0f, 0.0f)
            .setAttribute(ProtectionPixelArmorAttributes.HIGH_SPEED_DAMAGE_INCREASE, 0.5f);
    }

    private static ArmorStats registerArmorStats(String armorId) {
        ArmorStats stats = new ArmorStats();
        ARMOR_STATS.put(armorId, stats);
        return stats;
    }

    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        
        if (itemId == null) return;
        String itemKey = itemId.toString();
        
        if (ARMOR_STATS.containsKey(itemKey)) {
            ArmorStats stats = ARMOR_STATS.get(itemKey);
            
            EquipmentSlot itemSlot = null;
            if (item instanceof Equipable equipable) {
                itemSlot = equipable.getEquipmentSlot();
            }
            if (itemSlot == null) {
                UnrealZaruba.LOGGER.error("ArmorBalancer: itemSlot is null for item: {}", itemKey);
                return;
            }

            replaceAttribute(event, itemSlot, Attributes.ARMOR, stats.armor);
            replaceAttribute(event, itemSlot, Attributes.ARMOR_TOUGHNESS, stats.toughness);
            replaceAttribute(event, itemSlot, Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResistance);
            if (stats.attackDamage > 0) {
                replaceAttribute(event, itemSlot, Attributes.ATTACK_DAMAGE, stats.attackDamage);
            }
            if (stats.attackSpeed > 0) {
                replaceAttribute(event, itemSlot, Attributes.ATTACK_SPEED, stats.attackSpeed);
            }
            
            for (Map.Entry<Holder<Attribute>, Float> entry : stats.customAttributes.entrySet()) {
                replaceAttribute(event, itemSlot, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void replaceAttribute(ItemAttributeModifierEvent event, EquipmentSlot slot, Holder<Attribute> attribute, float value) {
        if (attribute == null) return;

        event.removeAllModifiersFor(attribute);

        if (value > 0) {
            event.addModifier(attribute, new AttributeModifier(
                getConsistentId(attribute, slot),
                value,
                AttributeModifier.Operation.ADD_VALUE
            ), EquipmentSlotGroup.bySlot(slot));
        }
    }

    private static ResourceLocation getConsistentId(Holder<Attribute> attribute, EquipmentSlot slot) {
        String attributeName = attribute.unwrapKey().map(key -> key.location().getPath()).orElse("unknown");
        return ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "armor_balance/" + slot.getName() + "/" + attributeName);
    }

    private static class ArmorStats {
        float armor = 0.0f;
        float toughness = 0.0f;
        float knockbackResistance = 0.0f;
        float attackDamage = 0.0f;
        float attackSpeed = 0.0f;
        Map<Holder<Attribute>, Float> customAttributes = new HashMap<>();
        
        ArmorStats setVanillaStats(float armor, float toughness, float knockbackResistance) {
            this.armor = armor;
            this.toughness = toughness;
            this.knockbackResistance = knockbackResistance;
            return this;
        }

        ArmorStats setAdditionalVanillaStats(float attackDamage, float attackSpeed) {
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
            return this;
        }
        
        ArmorStats setAttribute(Holder<Attribute> attribute, float value) {
            if (attribute != null) {
                customAttributes.put(attribute, value);
            }
            return this;
        }
    }
}