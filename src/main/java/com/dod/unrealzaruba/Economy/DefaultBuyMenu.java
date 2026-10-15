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
 * their TACZ stats: shot damage (with armor ignore and headshot bonus) x fire rate^0.6 x magazine^0.4 x range, scaled so
 * the strongest (AWP) is about half of the tank and a pistol is 6-12% of it. Armor for a whole body tops out at about a
 * third of the tank.
 */
final class DefaultBuyMenu {
    private DefaultBuyMenu() {}

    static Tab weapons() {
        Tab tab = tab("buymenu.unrealzaruba.tab.weapons", Layout.GRID);

        tab.groups.add(group("buymenu.unrealzaruba.group.guns",
                category("buymenu.unrealzaruba.category.rifles", "buymenu.unrealzaruba.hint.guns",
                        gun("ak47", "AUTO", "7.62x39mm", 30, 820),
                        gun("m4a1", "AUTO", "5.56x45mm", 30, 640),
                        gun("hk416d", "AUTO", "5.56x45mm", 30, 730),
                        gun("aug", "AUTO", "5.56x45mm", 30, 690),
                        gun("scar_h", "SEMI", ".308", 20, 1310)),
                category("buymenu.unrealzaruba.category.smgs", "buymenu.unrealzaruba.hint.guns",
                        gun("ump45", "AUTO", ".45 ACP", 25, 780),
                        gun("hk_mp5a5", "AUTO", "9mm", 30, 500),
                        gun("vector45", "AUTO", ".45 ACP", 20, 520),
                        gun("p90", "AUTO", "5.7x28mm", 50, 730)),
                category("buymenu.unrealzaruba.category.pistols", "buymenu.unrealzaruba.hint.guns",
                        gun("glock_17", "SEMI", "9mm", 17, 240),
                        gun("m1911", "SEMI", ".45 ACP", 7, 340),
                        gun("cz75", "AUTO", "9mm", 16, 340),
                        gun("deagle", "SEMI", ".50 AE", 7, 470)),
                category("buymenu.unrealzaruba.category.snipers", "buymenu.unrealzaruba.hint.guns",
                        gun("kar98", "SEMI", "7.92x57mm", 4, 860),
                        gun("m700", "SEMI", ".30-06", 5, 840),
                        gun("mk14", "SEMI", ".308", 10, 1080),
                        gun("ai_awp", "SEMI", ".338", 5, 2000)),
                category("buymenu.unrealzaruba.category.shotguns", "buymenu.unrealzaruba.hint.guns",
                        gun("m870", "SEMI", "12 gauge", 5, 630),
                        gun("spas_12", "SEMI", "12 gauge", 5, 1140),
                        gun("aa12", "SEMI", "12 gauge", 8, 750)),
                category("buymenu.unrealzaruba.category.machine_guns", "buymenu.unrealzaruba.hint.guns",
                        gun("m249", "AUTO", "5.56x45mm", 75, 1240))));

        String attachmentHint = "buymenu.unrealzaruba.hint.attachments";
        tab.groups.add(group("buymenu.unrealzaruba.group.attachments",
                category("buymenu.unrealzaruba.category.scopes", attachmentHint,
                        attachment("sight_rmr_dot", 90),
                        attachment("sight_exp3", 150),
                        attachment("scope_acog_ta31", 240),
                        attachment("scope_standard_8x", 420)),
                category("buymenu.unrealzaruba.category.muzzles", attachmentHint,
                        attachment("muzzle_brake_pioneer", 110),
                        attachment("muzzle_compensator_trident", 120),
                        attachment("muzzle_silencer_phantom_s1", 180)),
                category("buymenu.unrealzaruba.category.grips", attachmentHint,
                        attachment("grip_vertical_military", 70),
                        attachment("grip_magpul_afg_2", 70)),
                category("buymenu.unrealzaruba.category.stocks", attachmentHint,
                        attachment("oem_stock_light", 100),
                        attachment("oem_stock_tactical", 130)),
                category("buymenu.unrealzaruba.category.lasers", attachmentHint,
                        attachment("laser_peq15", 90)),
                category("buymenu.unrealzaruba.category.mags", attachmentHint,
                        attachment("extended_mag_1", 150),
                        attachment("extended_mag_2", 240))));

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
                        ammo("12g", 24, 60))));
        return tab;
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
        gun.info.put("buymenu.unrealzaruba.info.magazine", String.valueOf(magazine));
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
