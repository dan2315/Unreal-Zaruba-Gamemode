package com.dod.unrealzaruba.CharacterClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.dod.unrealzaruba.CharacterClass.ItemDataBuilders.ProtectionPixel;
import com.dod.unrealzaruba.CharacterClass.ItemDataBuilders.WeaponConstants;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.resources.ResourceLocation;
import com.dod.unrealzaruba.CharacterClass.ItemDataBuilders.VanillaItems;
import com.dod.unrealzaruba.CharacterClass.ItemDataBuilders.WeaponBuilder;

public class CharacterClassRegistry {

    private static final Map<String, Map<TeamColor, CharacterClassData>> CHARACTER_CLASSES = new HashMap<>();

    public static CharacterClassData getCharacterClass(String classId, TeamColor teamColor) {
        Map<TeamColor, CharacterClassData> teamClasses = CHARACTER_CLASSES.get(classId);
        if (teamClasses == null) {
            return null;
        }
        return teamClasses.get(teamColor);
    }

    public static void registerCharacterClass(TeamColor teamColor, CharacterClassData characterClassData) {
        CHARACTER_CLASSES.computeIfAbsent(characterClassData.getNameId(), k -> new HashMap<>())
                         .put(teamColor, characterClassData);
    }

    public static List<String> getCharacterClassIds() {
        return new ArrayList<>(CHARACTER_CLASSES.keySet());
    }

    public static List<TeamColor> getAvailableTeams(String classId) {
        Map<TeamColor, CharacterClassData> teamClasses = CHARACTER_CLASSES.get(classId);
        if (teamClasses == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(teamClasses.keySet());
    }

    private static void registerForBothTeams(
            String classId, 
            String displayName,
            Consumer<CharacterClassData> commonItems,
            Consumer<CharacterClassData> redArmor,
            Consumer<CharacterClassData> blueArmor) {
        
        // Create RED team class
        CharacterClassData redClass = new CharacterClassData(classId, displayName);
        commonItems.accept(redClass);
        redArmor.accept(redClass);
        redClass.addKitItem(new ResourceLocation("minecraft:cooked_beef"), 16);
        registerCharacterClass(TeamColor.RED, redClass);
        
        // Create BLUE team class
        CharacterClassData blueClass = new CharacterClassData(classId, displayName);
        commonItems.accept(blueClass);
        blueArmor.accept(blueClass);
        blueClass.addKitItem(new ResourceLocation("minecraft:cooked_beef"), 16);
        registerCharacterClass(TeamColor.BLUE, blueClass);
    }

    private static Consumer<CharacterClassData> redArmorSet(String helmetType) {
        return classData -> {
            classData.addKitItem(new ResourceLocation("protection_pixel:socks_boots"), 1)
                    .addKitItem(new ResourceLocation("protection_pixel:anchorpoint_leggings"), 1)
                    .addKitItem(new ResourceLocation("protection_pixel:breaker_chestplate"), 1)
                    .addKitItem(new ResourceLocation("protection_pixel:" + helmetType + "_helmet"), 1);
        };
    }

    private static Consumer<CharacterClassData> blueArmorSet(String helmetType) {
        return classData -> {
            classData.addKitItem(new ResourceLocation("protection_pixel:socks_boots"), 1)
                    .addKitItem(new ResourceLocation("protection_pixel:anchorpointas_leggings"), 1)
                    .addKitItem(new ResourceLocation("protection_pixel:breakeras_chestplate"), 1)
                    .addKitItem(new ResourceLocation("protection_pixel:" + helmetType + "as_helmet"), 1);
        };
    }

    static {
        // Vegetable Warrior =========================================================================
        registerForBothTeams(
            "vegetable_warrior", 
            "Vegetable Warrior",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(new ResourceLocation("minecraft:melon"), 64)
                .addKitItem(new ResourceLocation("minecraft:golden_carrot"), 64)
                .addKitItem(new ResourceLocation("create:potato_cannon"), 1)
                .addKitItem(new ResourceLocation("tfmg:advanced_potato_cannon"), 1)
                .addKitItem(new ResourceLocation("tfmg:napalm_potato"), 1),
            redArmorSet("hunter"),
            blueArmorSet("hunter")
        );

        // Ueban s valinoy =========================================================================
        registerForBothTeams(
            "ueban_s_valinoy", 
            "Ueban s valinoy",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(VanillaItems.getStickyPumpkin())
                .addKitItem(new ResourceLocation(WeaponConstants.ScorchedGuns.Weapons.BIG_BORE), 1)
                .addKitItem(new ResourceLocation("scguns:osborne_slug"), 4)
                .addKitItem(new ResourceLocation("minecraft:netherite_axe"), 1)
                .addKitItem(new ResourceLocation("scguns:stun_grenade"), 4),
            redArmorSet(""),  // Just base armor with no special helmet
            blueArmorSet("")  // Just base armor with no special helmet
        );

        // Marksman =========================================================================
        var gaussRifle = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.GAUSS_RIFLE)
            .ammoCount(8)
            .scope(WeaponConstants.ScorchedGuns.Attachments.MEDIUM_SCOPE)
            .stock(WeaponConstants.ScorchedGuns.Attachments.WOODEN_STOCK)
            .underBarrel(WeaponConstants.ScorchedGuns.Attachments.VERTICAL_GRIP)
            .build();

        var soulDrummer = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.SOUL_DRUMMER)
            .ammoCount(22)
            .barrel(WeaponConstants.ScorchedGuns.Attachments.MUZZLE_BRAKE)
            .stock(WeaponConstants.ScorchedGuns.Attachments.WOODEN_STOCK)
            .build();
            
        registerForBothTeams(
            "marksman", 
            "Marksman",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(gaussRifle)
                .addKitItem(new ResourceLocation("scguns:energy_cell"), 64)
                .addKitItem(soulDrummer)
                .addKitItem(new ResourceLocation("scguns:compact_advanced_round"), 128),
            redArmorSet("closed"),
            blueArmorSet("closed")
        );

        // Engineer =========================================================================
        var laserMusket = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.LASER_MUSKET)
            .scope(WeaponConstants.ScorchedGuns.Attachments.LASER_SIGHT)
            .ammoCount(12)
            .build();

        var miningLaser = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.MINING_LASER)
            .ammoCount(100)
            .build();

        registerForBothTeams(
            "engineer", 
            "Engineer",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(laserMusket)
                .addKitItem(miningLaser)
                .addKitItem(new ResourceLocation("scguns:energy_core"), 64)
                .addKitItem(new ResourceLocation("tfmg:cast_iron_frame"), 32)
                .addKitItem(new ResourceLocation("create:industrial_iron_block"), 32),
            redArmorSet("bloodprisoner"),
            blueArmorSet("bloodprisoner")
        );

        // Soldier =========================================================================
        var prushGun = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.PRUSH_GUN)
            .barrel(WeaponConstants.ScorchedGuns.Attachments.EXTENDED_BARREL)
            .stock(WeaponConstants.ScorchedGuns.Attachments.WOODEN_STOCK)
            .underBarrel(WeaponConstants.ScorchedGuns.Attachments.LIGHT_GRIP)
            .magazine(WeaponConstants.ScorchedGuns.Attachments.SPEED_MAG)
            .ammoCount(21)
            .build();

        var boomStick = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.BOOMSTICK)
            .ammoCount(4)
            .build();

        registerForBothTeams(
            "soldier", 
            "Soldier",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(prushGun)
                .addKitItem(new ResourceLocation("scguns:compact_advanced_round"), 128)
                .addKitItem(boomStick)
                .addKitItem(new ResourceLocation("scguns:shotgun_shell"), 32)
                .addKitItem(new ResourceLocation("tfmg:thermite_grenade"), 8),
            redArmorSet("hammer"),
            blueArmorSet("hammer")
        );

        // Demolisher =========================================================================
        var rocketRifle = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.ROCKET_RIFLE)
            .ammoCount(1)
            .build();

        var bombLance = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.BOMB_LANCE)
            .ammoCount(1)
            .build();

        registerForBothTeams(
            "demolisher", 
            "Demolisher",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(rocketRifle)
                .addKitItem(new ResourceLocation("scguns:rocket"), 8)
                .addKitItem(bombLance)
                .addKitItem(new ResourceLocation("scguns:microject"), 64)
                .addKitItem(new ResourceLocation("minecraft:flint_and_steel"), 1)
                .addKitItem(new ResourceLocation("minecraft:tnt"), 16),
            redArmorSet("lancer"),
            blueArmorSet("lancer")
        );

        // Medic =========================================================================
        var crossbow = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.NIAMI)
            .ammoCount(7)
            .build();

        var healingCrossbow = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.CRUSADER)
            .ammoCount(3)
            .build();

        registerForBothTeams(
            "medic",
            "Medic",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(crossbow)
                .addKitItem(healingCrossbow)
                .addKitItem(new ResourceLocation("scguns:syringe"), 32)
                .addKitItem(new ResourceLocation("minecraft:apple"), 64)
                .addKitItem((new Supplier<ItemStack>() {
                    @Override
                    public ItemStack get() {
                        ItemStack stack = new ItemStack(Items.SPLASH_POTION, 6);
                        stack.getOrCreateTag().putString("Potion", "minecraft:strong_regeneration");
                        return stack;
                    }
                }).get()),
            redArmorSet("plague"),
            blueArmorSet("plague")
        );


        // For ships
        var blunderbuss = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.BLUNDERBUSS)
            .stock(WeaponConstants.ScorchedGuns.Attachments.WOODEN_STOCK)
            .ammoCount(1)
            .build();

        var musket = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.MUSKET)
            .scope(WeaponConstants.ScorchedGuns.Attachments.MEDIUM_SCOPE)
            .ammoCount(1)
            .build();

        var repeatingMusket = new WeaponBuilder(WeaponConstants.ScorchedGuns.Weapons.REPEATING_MUSKET)
            .underBarrel(WeaponConstants.ScorchedGuns.Attachments.VERTICAL_GRIP)
            .ammoCount(4)
            .build();

        registerForBothTeams(
            "blunderbuss",
            "Blunderbuss",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(blunderbuss)
                .addKitItem(new ResourceLocation("scguns:grapeshot"), 32)
                .addKitItem(new ResourceLocation("minecraft:iron_sword"), 1)
                .addKitItem(new ResourceLocation("minecraft:cooked_beef"), 16),
            redArmorSet(""),
            blueArmorSet("")
        );

        registerForBothTeams(
            "musket",
            "Musket",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(musket)
                .addKitItem(new ResourceLocation("scguns:powder_and_ball"), 32)
                .addKitItem(new ResourceLocation("minecraft:iron_sword"), 1)
                .addKitItem(new ResourceLocation("minecraft:cooked_beef"), 16),
            redArmorSet(""),
            blueArmorSet("")
        );

        registerForBothTeams(
            "repeating_musket",
            "Repeating Musket",
            classData -> classData
                .addKitItem(ProtectionPixel.CreatePowerEngineWithWaterTank())
                .addKitItem(repeatingMusket)
                .addKitItem(new ResourceLocation("scguns:powder_and_ball"), 64)
                .addKitItem(new ResourceLocation("minecraft:iron_sword"), 1)
                .addKitItem(new ResourceLocation("minecraft:cooked_beef"), 16),
            redArmorSet(""),
            blueArmorSet("")
        );
    }
}
