package com.dod.unrealzaruba.OtherModTweaks.ProtectionPixel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class ProtectionPixelArmorAttributes {
    public static final String FLOAT_SHIELD_ATTRIBUTE_ID = "protection_pixel:floatshieldattribute";
    public static final String EFFECT_CLEAR_ID = "protection_pixel:effectclear";
    public static final String HIGH_SPEED_DAMAGE_INCREASE_ID = "protection_pixel:highspeeddamageincrease";
    public static final String DYNAMIC_ENTITY_PERCEPTION_ID = "protection_pixel:dynamicentityperception";
    public static final String ARROW_MARKING_ID = "protection_pixel:arrowmarking";
    public static final String BREATH_IN_SWIMMING_ID = "protection_pixel:breathinswimming";
    public static final String BURN_REDUCTION_ID = "protection_pixel:burnreduction";
    public static final String MAGIC_DAMAGE_REDUCTION_ID = "protection_pixel:magicdamagereduction";
    public static final String HEALTH_CONVERSION_ID = "protection_pixel:healthconversion";
    public static final String WEAK_RADIATION_ID = "protection_pixel:weakradiation";
    public static final String ARROW_BARRIER_ID = "protection_pixel:arrowbarrier";
    public static final String PROSTHETIC_ATTACK_ID = "protection_pixel:prostheticattack";
    public static final String MAGNETIC_RANGE_ID = "protection_pixel:magneticrange";
    public static final String MAGNETIC_ATTRACT_TARGET_ID = "protection_pixel:magneticattracttarget";
    public static final String PNEUMATIC_PICKAXE_ID = "protection_pixel:pneumaticpickaxea";
    public static final String PREVENT_BURIAL_ID = "protection_pixel:preventburial";
    public static final String HELLFIRE_ID = "protection_pixel:hellfire";
    public static final String ROCKET_ASSIST_ID = "protection_pixel:rocketassist";
    public static final String SHIELD_ID = "protection_pixel:shielda";
    public static final String JUMP_BOOST_ID = "protection_pixel:jumpboost";
    public static final String FALL_DAMAGE_REDUCTION_ID = "protection_pixel:falldamagereduction";
    public static final String SUSPENSION_ID = "protection_pixel:suspension";
    public static final String FOG_PERSPECTIVE_ID = "protection_pixel:fogperspective";
    public static final String CLEAVE_ID = "protection_pixel:cleave";
    public static final String NEUTRALITY_ID = "protection_pixel:neutrality";
    public static final String DRONE_ID = "protection_pixel:drone";
    
    private static final Map<String, Attribute> ATTRIBUTE_CACHE = new HashMap<>();
    
    public static Attribute FLOAT_SHIELD_ATTRIBUTE = getOrCreateAttribute(FLOAT_SHIELD_ATTRIBUTE_ID);
    public static Attribute EFFECT_CLEAR = getOrCreateAttribute(EFFECT_CLEAR_ID);
    public static Attribute HIGH_SPEED_DAMAGE_INCREASE = getOrCreateAttribute(HIGH_SPEED_DAMAGE_INCREASE_ID);
    public static Attribute DYNAMIC_ENTITY_PERCEPTION = getOrCreateAttribute(DYNAMIC_ENTITY_PERCEPTION_ID);
    public static Attribute ARROW_MARKING = getOrCreateAttribute(ARROW_MARKING_ID);
    public static Attribute BREATH_IN_SWIMMING = getOrCreateAttribute(BREATH_IN_SWIMMING_ID);
    public static Attribute BURN_REDUCTION = getOrCreateAttribute(BURN_REDUCTION_ID);
    public static Attribute MAGIC_DAMAGE_REDUCTION = getOrCreateAttribute(MAGIC_DAMAGE_REDUCTION_ID);
    public static Attribute HEALTH_CONVERSION = getOrCreateAttribute(HEALTH_CONVERSION_ID);
    public static Attribute WEAK_RADIATION = getOrCreateAttribute(WEAK_RADIATION_ID);
    public static Attribute ARROW_BARRIER = getOrCreateAttribute(ARROW_BARRIER_ID);
    public static Attribute PROSTHETIC_ATTACK = getOrCreateAttribute(PROSTHETIC_ATTACK_ID);
    public static Attribute MAGNETIC_RANGE = getOrCreateAttribute(MAGNETIC_RANGE_ID);
    public static Attribute MAGNETIC_ATTRACT_TARGET = getOrCreateAttribute(MAGNETIC_ATTRACT_TARGET_ID);
    public static Attribute PNEUMATIC_PICKAXE = getOrCreateAttribute(PNEUMATIC_PICKAXE_ID);
    public static Attribute PREVENT_BURIAL = getOrCreateAttribute(PREVENT_BURIAL_ID);
    public static Attribute HELLFIRE = getOrCreateAttribute(HELLFIRE_ID);
    public static Attribute ROCKET_ASSIST = getOrCreateAttribute(ROCKET_ASSIST_ID);
    public static Attribute SHIELD = getOrCreateAttribute(SHIELD_ID);
    public static Attribute JUMP_BOOST = getOrCreateAttribute(JUMP_BOOST_ID);
    public static Attribute FALL_DAMAGE_REDUCTION = getOrCreateAttribute(FALL_DAMAGE_REDUCTION_ID);
    public static Attribute SUSPENSION = getOrCreateAttribute(SUSPENSION_ID);
    public static Attribute FOG_PERSPECTIVE = getOrCreateAttribute(FOG_PERSPECTIVE_ID);
    public static Attribute CLEAVE = getOrCreateAttribute(CLEAVE_ID);
    public static Attribute NEUTRALITY = getOrCreateAttribute(NEUTRALITY_ID);
    public static Attribute DRONE = getOrCreateAttribute(DRONE_ID);

    private static Attribute getOrCreateAttribute(String id) {
        return ATTRIBUTE_CACHE.computeIfAbsent(id, key -> 
            ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key)));
    }

    public static Attribute getAttribute(String id) {
        return getOrCreateAttribute(id);
    }
}