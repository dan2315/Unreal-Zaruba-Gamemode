package com.dod.unrealzaruba.Economy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dod.unrealzaruba.Economy.EconomyConfig.Category;
import com.dod.unrealzaruba.Economy.EconomyConfig.Group;
import com.dod.unrealzaruba.Economy.EconomyConfig.Item;
import com.dod.unrealzaruba.Economy.EconomyConfig.Layout;
import com.dod.unrealzaruba.Economy.EconomyConfig.Tab;

/**
 * The buy menu written into a map's economy.json when it doesn't have one yet: TACZ default gun pack weapons and
 * vanilla armor. Prices are a starting point, edit the json per map.
 *
 * Scale: the reference is a one-man tank with a 30-round magazine at $3920.8 (about $131 a shot). Guns are priced from
 * their TACZ stats against a player with no armor, iron and diamond (armor ignore and a 15% share of headshots counted):
 * the time to kill (shots needed x fire interval, with the bolt), the damage per second over a magazine and its reload,
 * and the range before the damage drops. Power = sqrt(1 / (time to kill + 0.4) x sustained damage) x (range / 30)^0.35;
 * price = 800 x (power / power of the AK-47)^2, rounded. Armor for a whole body tops out at about a third of the tank.
 */
final class DefaultBuyMenu {
    private DefaultBuyMenu() {}

    static final String WEAPONS_TAB = "buymenu.unrealzaruba.tab.weapons";

    static Tab weapons() {
        Tab tab = tab(WEAPONS_TAB, Layout.GRID);

        tab.groups.add(group("buymenu.unrealzaruba.group.guns",
                category("buymenu.unrealzaruba.category.rifles", "buymenu.unrealzaruba.hint.guns",
                        gun("aug", "AUTO", "5.56x45mm", 30, 550),
                        gun("m4a1", "AUTO", "5.56x45mm", 30, 560),
                        gun("qbz_95", "AUTO", "5.8x42mm", 30, 660),
                        gun("hk416d", "AUTO", "5.56x45mm", 30, 710),
                        gun("ak47", "AUTO", "7.62x39mm", 30, 800),
                        gun("sks_tactical", "SEMI", "7.62x39mm", 10, 830),
                        gun("m16a1", "AUTO", "5.56x45mm", 20, 920),
                        gun("spr15hb", "SEMI", "5.56x45mm", 15, 1000),
                        gun("mk14", "SEMI", ".308", 10, 1500),
                        gun("scar_h", "SEMI", ".308", 20, 1700)),
                category("buymenu.unrealzaruba.category.smgs", "buymenu.unrealzaruba.hint.guns",
                        gun("uzi", "AUTO", "9mm", 20, 260),
                        gun("hk_mp5a5", "AUTO", "9mm", 30, 300),
                        gun("vector45", "AUTO", ".45 ACP", 20, 380),
                        gun("p90", "AUTO", "5.7x28mm", 50, 470),
                        gun("ump45", "AUTO", ".45 ACP", 25, 720)),
                category("buymenu.unrealzaruba.category.pistols", "buymenu.unrealzaruba.hint.guns",
                        gun("rhino357", "SEMI", ".357 Magnum", 6, 130),
                        gun("glock_17", "SEMI", "9mm", 17, 150),
                        gun("cz75", "AUTO", "9mm", 16, 190),
                        gun("m1911", "SEMI", ".45 ACP", 7, 260),
                        gun("p320", "SEMI", ".45 ACP", 12, 360),
                        gun("deagle_golden", "SEMI", ".357 Magnum", 9, 370),
                        gun("lonetrail", "SEMI", ".30-06", 1, 380),
                        gun("deagle", "SEMI", ".50 AE", 7, 440),
                        gun("taurus500", "SEMI", ".500 Magnum", 5, 1600)),
                category("buymenu.unrealzaruba.category.snipers", "buymenu.unrealzaruba.hint.guns",
                        gun("springfield1873", "SEMI", ".45-70", 1, 540),
                        gun("kar98", "SEMI", "7.92x57mm", 4, 570),
                        gun("m700", "SEMI", ".30-06", 5, 720),
                        gun("ai_awp", "SEMI", ".338", 5, 1900),
                        gun("m95", "SEMI", ".50 BMG", 5, 2300)),
                // Above the formula: at the range of a street they kill in one or two shots
                category("buymenu.unrealzaruba.category.shotguns", "buymenu.unrealzaruba.hint.guns",
                        gun("db_short", "SEMI", "12 gauge", 2, 100),
                        gun("m870", "SEMI", "12 gauge", 5, 300),
                        gun("aa12", "SEMI", "12 gauge", 8, 400),
                        gun("m1014", "SEMI", "12 gauge", 6, 450),
                        gun("spas_12", "SEMI", "12 gauge", 5, 650)),
                // The minigun is fed from the inventory (no magazine) and capped below the formula (4400): it slows its carrier by a quarter
                category("buymenu.unrealzaruba.category.machine_guns", "buymenu.unrealzaruba.hint.guns",
                        gun("m249", "AUTO", "5.56x45mm", 75, 700),
                        gun("fn_evolys", "AUTO", ".308", 75, 2050),
                        gun("minigun", "AUTO", ".308", 0, 3500)),
                category("buymenu.unrealzaruba.category.launchers", "buymenu.unrealzaruba.hint.guns",
                        gun("m320", "SEMI", "40mm", 1, LAUNCHER_PRICE))));

        // Every attachment of the pack that fits a gun sold here (the PEQ-6 laser and the Timeless 50 brake fit none). Priced from
        // their stats: zoom, recoil, spread, aiming time, silence, range, headshots, magazine size, weight; the shotgun choke and
        // the explosive rounds by hand
        String attachmentHint = "buymenu.unrealzaruba.hint.attachments";
        tab.groups.add(group("buymenu.unrealzaruba.group.attachments",
                category("buymenu.unrealzaruba.category.scopes", attachmentHint,
                        attachment("sight_p90", 90),
                        attachment("sight_okp7", 100),
                        attachment("sight_srs_02", 110),
                        attachment("sight_coyote", 120),
                        attachment("sight_deltapoint_pistol", 120),
                        attachment("sight_deltapoint_rifle", 120),
                        attachment("sight_fastfire_pistol", 120),
                        attachment("sight_fastfire_rifle", 120),
                        attachment("sight_sro_dot", 120),
                        attachment("sight_rmr_dot", 130),
                        attachment("sight_552", 150),
                        attachment("scope_acog_ta31", 160),
                        attachment("sight_exp3", 160),
                        attachment("sight_acro_pistol", 170),
                        attachment("sight_pk06_pistol", 170),
                        attachment("sight_pk06_rifle", 170),
                        attachment("sight_acro_rifle", 180),
                        attachment("scope_qmk152", 190),
                        attachment("sight_t2", 190),
                        attachment("sight_uh1", 190),
                        attachment("scope_aug_default", 200),
                        attachment("scope_retro_2x", 200),
                        attachment("sight_t1", 200),
                        attachment("scope_98k", 240),
                        attachment("scope_contender", 250),
                        attachment("scope_elcan_4x", 250),
                        attachment("scope_hamr", 270),
                        attachment("scope_1873_6x", 290),
                        attachment("scope_lpvo_1_6", 330),
                        attachment("scope_standard_8x", 330),
                        attachment("scope_vudu", 370),
                        attachment("scope_mk5hd", 420)),
                category("buymenu.unrealzaruba.category.muzzles", attachmentHint,
                        attachment("muzzle_brake_mastiff_sg", 90),
                        attachment("muzzle_brake_cthulhu", 110),
                        attachment("muzzle_compensator_trident", 120),
                        attachment("muzzle_brake_cyclone_d2", 130),
                        attachment("muzzle_brake_trex", 130),
                        attachment("muzzle_choke_sg", 150),
                        attachment("muzzle_brake_pioneer", 160),
                        attachment("muzzle_silencer_ptilopsis", 190),
                        attachment("muzzle_silencer_mirage", 200),
                        attachment("muzzle_silencer_phantom_s1", 200),
                        attachment("muzzle_silencer_sg", 220),
                        attachment("muzzle_silencer_knight_qd", 280),
                        attachment("muzzle_silencer_ursus", 280),
                        attachment("deagle_golden_long_barrel", 320),
                        attachment("muzzle_silencer_wraith", 320),
                        attachment("muzzle_silencer_vulture", 360)),
                category("buymenu.unrealzaruba.category.bayonets", attachmentHint,
                        attachment("bayonet_6h3", 50),
                        attachment("bayonet_m9", 60)),
                category("buymenu.unrealzaruba.category.grips", attachmentHint,
                        attachment("grip_rk1_b25u", 40),
                        attachment("grip_cobra", 60),
                        attachment("grip_td", 70),
                        attachment("grip_magpul_afg_2", 80),
                        attachment("grip_osovets_black", 80),
                        attachment("grip_rk6", 80),
                        attachment("grip_cqr", 100),
                        attachment("grip_rk0", 100),
                        attachment("grip_se_5", 100),
                        attachment("grip_vertical_talon", 120),
                        attachment("grip_vertical_military", 160),
                        attachment("grip_vertical_ranger", 190)),
                category("buymenu.unrealzaruba.category.stocks", attachmentHint,
                        attachment("stock_ripstock", 60),
                        attachment("stock_m4ss", 80),
                        attachment("stock_moe", 100),
                        attachment("stock_sba3", 100),
                        attachment("oem_stock_light", 130),
                        attachment("stock_carbon_bone_c5", 130),
                        attachment("stock_ak12", 150),
                        attachment("stock_hk_slim_line", 150),
                        attachment("oem_stock_tactical", 170),
                        attachment("stock_tactical_spas_12", 180),
                        attachment("stock_tactical_ar", 190),
                        attachment("oem_stock_heavy", 220),
                        attachment("stock_heavy_spas_12", 240),
                        attachment("stock_militech_b5", 260)),
                category("buymenu.unrealzaruba.category.lasers", attachmentHint,
                        attachment("laser_nightstick", 50),
                        attachment("laser_peq15", 100),
                        attachment("laser_lopro", 160),
                        attachment("laser_compact", 170)),
                category("buymenu.unrealzaruba.category.mags", attachmentHint,
                        attachment("light_extended_mag_1", 100),
                        attachment("shotgun_extended_mag_1", 120),
                        attachment("extended_mag_1", 140),
                        attachment("sniper_extended_mag_1", 170),
                        attachment("light_extended_mag_2", 180),
                        attachment("shotgun_extended_mag_2", 210),
                        attachment("extended_mag_2", 260),
                        attachment("light_extended_mag_3", 260),
                        attachment("shotgun_extended_mag_3", 310),
                        attachment("sniper_extended_mag_2", 320),
                        attachment("extended_mag_3", 370),
                        attachment("sniper_extended_mag_3", 460)),
                category("buymenu.unrealzaruba.category.ammo_mods", attachmentHint,
                        attachment("ammo_mod_slug", 230),
                        attachment("ammo_mod_hp", 280),
                        attachment("ammo_mod_i", 280),
                        attachment("ammo_mod_fmj", 310),
                        attachment("ammo_mod_he", 1000))));

        tab.groups.add(group("buymenu.unrealzaruba.group.ammunition",
                category("buymenu.unrealzaruba.category.ammo", "buymenu.unrealzaruba.hint.ammo",
                        ammo("762x39", 60, 70),
                        ammo("556x45", 60, 70),
                        ammo("308", 40, 90),
                        ammo("9mm", 90, 50),
                        ammo("45acp", 75, 55),
                        ammo("57x28", 100, 70),
                        ammo("50ae", 28, 60),
                        ammo("792x57", 20, 60),
                        ammo("30_06", 20, 70),
                        ammo("338", 20, 100),
                        ammo("12g", 24, 60),
                        ammo("58x42", 60, 70),
                        ammo("357mag", 36, 60),
                        ammo("500mag", 15, 75),
                        ammo("45_70", 15, 60),
                        ammo("50bmg", 10, 100),
                        ammo("40mm", 1, GRENADE_PRICE))));
        return tab;
    }

    /** The grenade launcher is cheap to own and dear to fire: every grenade costs as much as a good rifle. */
    static final int LAUNCHER_PRICE = 1400;
    static final int GRENADE_PRICE = 2000;

    static Tab food() {
        Tab tab = tab(FOOD_TAB, Layout.GRID);
        tab.groups.add(group("buymenu.unrealzaruba.group.food",
                category("buymenu.unrealzaruba.category.food", "buymenu.unrealzaruba.hint.food",
                        // Cheaper than steak for what it feeds: 24 hunger for 25 against 32 for 60
                        stack("minecraft:carrot", 8, 25),
                        stack("minecraft:cooked_beef", 4, 60),
                        stack("minecraft:golden_apple", 1, 200)),
                category("buymenu.unrealzaruba.category.potions", "buymenu.unrealzaruba.hint.potions",
                        item("minecraft:potion[potion_contents={potion:\"minecraft:strong_healing\"}]", 220),
                        item("minecraft:potion[potion_contents={potion:\"minecraft:swiftness\"}]", 90),
                        item("minecraft:potion[potion_contents={potion:\"minecraft:strong_swiftness\"}]", 140))));
        return tab;
    }

    /** The basic paraglider of the Gliders mod (the leather one: its paper is made with leather). */
    private static Category glidersCategory() {
        return category("buymenu.unrealzaruba.category.gliders", "buymenu.unrealzaruba.hint.gliders", item("vc_gliders:paraglider_wood", GLIDER_PRICE));
    }

    static final int GLIDER_PRICE = 500;
    static final String FOOD_TAB = "buymenu.unrealzaruba.tab.food";

    private static Item stack(String item, int count, int price) {
        Item entry = item(item, price);
        entry.count = count;
        return entry;
    }

    /**
     * Adds what a map's saved menu may lack (the menu in economy.json is written once and then only edited by hand): the Food tab, and the
     * grenade launcher with its grenades. Returns true when something was added. Nothing the map has is changed or removed.
     */
    static boolean addMissing(EconomyConfig config) {
        boolean changed = false;
        if (config.buyMenu.stream().noneMatch(t -> FOOD_TAB.equals(t.name))) {
            config.buyMenu.add(Math.min(2, config.buyMenu.size()), food());
            changed = true;
        }
        for (Tab tab : config.buyMenu) {
            if (!"buymenu.unrealzaruba.tab.tools".equals(tab.name)) continue;
            boolean hasGlider = tab.groups.stream().flatMap(g -> g.categories.stream()).flatMap(c -> c.items.stream())
                    .anyMatch(i -> i.item != null && i.item.startsWith("vc_gliders:"));
            if (!hasGlider && !tab.groups.isEmpty()) {
                tab.groups.get(0).categories.add(glidersCategory());
                changed = true;
            }
        }
        for (Tab tab : config.buyMenu) {
            if (!"buymenu.unrealzaruba.tab.weapons".equals(tab.name)) continue;
            boolean hasLauncher = tab.groups.stream().flatMap(g -> g.categories.stream()).flatMap(c -> c.items.stream())
                    .anyMatch(i -> i.item != null && i.item.contains("tacz:m320"));
            if (!hasLauncher) {
                for (Group group : tab.groups) {
                    if ("buymenu.unrealzaruba.group.guns".equals(group.name)) {
                        group.categories.add(category("buymenu.unrealzaruba.category.launchers", "buymenu.unrealzaruba.hint.guns", gun("m320", "SEMI", "40mm", 1, LAUNCHER_PRICE)));
                        changed = true;
                    }
                    if ("buymenu.unrealzaruba.group.ammunition".equals(group.name)) {
                        for (Category category : group.categories) {
                            if ("buymenu.unrealzaruba.category.ammo".equals(category.name)) category.items.add(ammo("40mm", 1, GRENADE_PRICE));
                        }
                    }
                }
            }
        }
        return changed;
    }

    static Tab armor() {
        Tab tab = tab("buymenu.unrealzaruba.tab.armor", Layout.SETS);
        tab.groups.add(group("buymenu.unrealzaruba.group.armor_sets",
                armorSet("buymenu.unrealzaruba.set.leather", "leather", "buymenu.unrealzaruba.set.leather.note", 30, 60, 45, 25),
                armorSet("buymenu.unrealzaruba.set.chainmail", "chainmail", "buymenu.unrealzaruba.set.chainmail.note", 90, 160, 120, 70),
                armorSet("buymenu.unrealzaruba.set.iron", "iron", "buymenu.unrealzaruba.set.iron.note", 150, 260, 200, 110),
                armorSet("buymenu.unrealzaruba.set.diamond", "diamond", "buymenu.unrealzaruba.set.diamond.note", 330, 560, 430, 240)));
        return tab;
    }

    static Tab tools() {
        Tab tab = tab("buymenu.unrealzaruba.tab.tools", Layout.GRID);
        tab.groups.add(group("buymenu.unrealzaruba.group.tools",
                category("buymenu.unrealzaruba.category.pickaxes", "buymenu.unrealzaruba.hint.tools",
                        item("minecraft:wooden_pickaxe", 20), item("minecraft:stone_pickaxe", 40),
                        item("minecraft:iron_pickaxe", 90), item("minecraft:diamond_pickaxe", 240)),
                category("buymenu.unrealzaruba.category.axes", "buymenu.unrealzaruba.hint.tools",
                        item("minecraft:wooden_axe", 20), item("minecraft:stone_axe", 40),
                        item("minecraft:iron_axe", 90), item("minecraft:diamond_axe", 240)),
                category("buymenu.unrealzaruba.category.shovels", "buymenu.unrealzaruba.hint.tools",
                        item("minecraft:wooden_shovel", 10), item("minecraft:stone_shovel", 20),
                        item("minecraft:iron_shovel", 45), item("minecraft:diamond_shovel", 120)),
                category("buymenu.unrealzaruba.category.other_tools", "buymenu.unrealzaruba.hint.tools",
                        item("minecraft:shears", 60), item("minecraft:flint_and_steel", 200),
                        item("minecraft:bucket", 80), item("minecraft:fishing_rod", 30)),
                glidersCategory()));
        return tab;
    }

    /** Supply crate contents: mostly ammo and healing, sometimes a good gun or an optic. */
    static List<EconomyConfig.Loot> supplyLoot() {
        List<EconomyConfig.Loot> loot = new java.util.ArrayList<>();
        loot.add(loot(ammo("762x39", 60, 0).item, 60, 10));
        loot.add(loot(ammo("556x45", 60, 0).item, 60, 10));
        loot.add(loot(ammo("9mm", 90, 0).item, 90, 10));
        loot.add(loot(ammo("12g", 24, 0).item, 24, 6));
        loot.add(loot(ammo("308", 40, 0).item, 40, 5));
        loot.add(loot("minecraft:golden_apple", 2, 8));
        loot.add(loot("minecraft:cooked_beef", 8, 8));
        loot.add(loot(attachment("scope_acog_ta31", 0).item, 1, 4));
        loot.add(loot(attachment("muzzle_silencer_phantom_s1", 0).item, 1, 4));
        loot.add(loot(attachment("extended_mag_2", 0).item, 1, 4));
        loot.add(loot(gun("ak47", "AUTO", "", 30, 0).item, 1, 3));
        loot.add(loot(gun("m4a1", "AUTO", "", 30, 0).item, 1, 3));
        loot.add(loot(gun("hk_mp5a5", "AUTO", "", 30, 0).item, 1, 3));
        loot.add(loot(gun("m870", "SEMI", "", 5, 0).item, 1, 3));
        loot.add(loot(gun("ai_awp", "SEMI", "", 5, 0).item, 1, 1));
        loot.add(loot(gun("m249", "AUTO", "", 75, 0).item, 1, 1));
        return loot;
    }

    private static EconomyConfig.Loot loot(String item, int count, int weight) {
        EconomyConfig.Loot entry = new EconomyConfig.Loot();
        entry.item = item;
        entry.count = count;
        entry.weight = weight;
        return entry;
    }

    /** Leather is dyed in the team's colour; the other materials are the same for everyone. */
    private static Category armorSet(String name, String material, String note, int head, int chest, int legs, int feet) {
        Category set = category(name, null,
                armorPiece("buymenu.unrealzaruba.slot.head", material, "helmet", head),
                armorPiece("buymenu.unrealzaruba.slot.chest", material, "chestplate", chest),
                armorPiece("buymenu.unrealzaruba.slot.legs", material, "leggings", legs),
                armorPiece("buymenu.unrealzaruba.slot.feet", material, "boots", feet));
        set.note = note;
        return set;
    }

    private static Item armorPiece(String slot, String material, String piece, int price) {
        String id = "minecraft:" + material + "_" + piece;
        Item entry = item(id, price);
        entry.slot = slot;
        if (material.equals("leather")) {
            entry.teamItems = new LinkedHashMap<>(Map.of(
                    "RED", id + "[dyed_color=11546150]",
                    "BLUE", id + "[dyed_color=3949738]"));
        }
        return entry;
    }

    private static Item gun(String gunId, String fireMode, String ammo, int magazine, int price) {
        Item gun = item("tacz:modern_kinetic_gun[custom_data={GunId:\"tacz:" + gunId + "\",GunFireMode:\"" + fireMode
                + "\",GunCurrentAmmoCount:" + magazine + "}]", price);
        gun.note = ammo;
        gun.info = new LinkedHashMap<>();
        gun.info.put("buymenu.unrealzaruba.info.ammo", ammo);
        if (magazine > 0) gun.info.put("buymenu.unrealzaruba.info.magazine", String.valueOf(magazine)); // 0: fed from the inventory
        return gun;
    }

    private static Item attachment(String attachmentId, int price) {
        return item("tacz:attachment[custom_data={AttachmentId:\"tacz:" + attachmentId + "\"}]", price);
    }

    private static Item ammo(String ammoId, int count, int price) {
        Item ammo = item("tacz:ammo[custom_data={AmmoId:\"tacz:" + ammoId + "\"}]", price);
        ammo.count = count;
        ammo.note = count + " rds";
        return ammo;
    }

    private static Item item(String item, int price) {
        Item entry = new Item();
        entry.item = item;
        entry.price = price;
        return entry;
    }

    private static Tab tab(String name, String layout) {
        Tab tab = new Tab();
        tab.name = name;
        tab.layout = layout;
        return tab;
    }

    private static Group group(String name, Category... categories) {
        Group group = new Group();
        group.name = name;
        group.categories.addAll(List.of(categories));
        return group;
    }

    private static Category category(String name, String hint, Item... items) {
        Category category = new Category();
        category.name = name;
        category.hint = hint;
        category.items.addAll(List.of(items));
        return category;
    }
}
