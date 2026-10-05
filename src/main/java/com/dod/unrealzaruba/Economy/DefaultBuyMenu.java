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
 * Protection Pixel armor. Prices are a starting point, edit the json per map.
 */
final class DefaultBuyMenu {
    private DefaultBuyMenu() {}

    static Tab weapons() {
        Tab tab = tab("buymenu.unrealzaruba.tab.weapons", Layout.GRID);

        tab.groups.add(group("buymenu.unrealzaruba.group.guns",
                category("buymenu.unrealzaruba.category.rifles", "buymenu.unrealzaruba.hint.guns",
                        gun("ak47", "AUTO", "7.62x39mm", 30, 2700),
                        gun("m4a1", "AUTO", "5.56x45mm", 30, 3100),
                        gun("hk416d", "AUTO", "5.56x45mm", 30, 3200),
                        gun("aug", "AUTO", "5.56x45mm", 30, 3300),
                        gun("scar_h", "SEMI", ".308", 20, 3600)),
                category("buymenu.unrealzaruba.category.smgs", "buymenu.unrealzaruba.hint.guns",
                        gun("ump45", "AUTO", ".45 ACP", 25, 1200),
                        gun("hk_mp5a5", "AUTO", "9mm", 30, 1500),
                        gun("vector45", "AUTO", ".45 ACP", 20, 1800),
                        gun("p90", "AUTO", "5.7x28mm", 50, 2350)),
                category("buymenu.unrealzaruba.category.pistols", "buymenu.unrealzaruba.hint.guns",
                        gun("glock_17", "SEMI", "9mm", 17, 200),
                        gun("m1911", "SEMI", ".45 ACP", 7, 300),
                        gun("cz75", "AUTO", "9mm", 16, 500),
                        gun("deagle", "SEMI", ".50 AE", 7, 700)),
                category("buymenu.unrealzaruba.category.snipers", "buymenu.unrealzaruba.hint.guns",
                        gun("kar98", "SEMI", "7.92x57mm", 4, 1700),
                        gun("m700", "SEMI", ".30-06", 5, 2800),
                        gun("mk14", "SEMI", ".308", 10, 3000),
                        gun("ai_awp", "SEMI", ".338", 5, 4750)),
                category("buymenu.unrealzaruba.category.shotguns", "buymenu.unrealzaruba.hint.guns",
                        gun("m870", "SEMI", "12 gauge", 5, 1100),
                        gun("spas_12", "SEMI", "12 gauge", 5, 1500),
                        gun("aa12", "SEMI", "12 gauge", 8, 2500)),
                category("buymenu.unrealzaruba.category.machine_guns", "buymenu.unrealzaruba.hint.guns",
                        gun("m249", "AUTO", "5.56x45mm", 75, 5200))));

        String attachmentHint = "buymenu.unrealzaruba.hint.attachments";
        tab.groups.add(group("buymenu.unrealzaruba.group.attachments",
                category("buymenu.unrealzaruba.category.scopes", attachmentHint,
                        attachment("sight_rmr_dot", 150),
                        attachment("sight_exp3", 250),
                        attachment("scope_acog_ta31", 400),
                        attachment("scope_standard_8x", 700)),
                category("buymenu.unrealzaruba.category.muzzles", attachmentHint,
                        attachment("muzzle_brake_pioneer", 180),
                        attachment("muzzle_compensator_trident", 200),
                        attachment("muzzle_silencer_phantom_s1", 300)),
                category("buymenu.unrealzaruba.category.grips", attachmentHint,
                        attachment("grip_vertical_military", 120),
                        attachment("grip_magpul_afg_2", 120)),
                category("buymenu.unrealzaruba.category.stocks", attachmentHint,
                        attachment("oem_stock_light", 160),
                        attachment("oem_stock_tactical", 220)),
                category("buymenu.unrealzaruba.category.lasers", attachmentHint,
                        attachment("laser_peq15", 140)),
                category("buymenu.unrealzaruba.category.mags", attachmentHint,
                        attachment("extended_mag_1", 250),
                        attachment("extended_mag_2", 400))));

        tab.groups.add(group("buymenu.unrealzaruba.group.ammunition",
                category("buymenu.unrealzaruba.category.ammo", "buymenu.unrealzaruba.hint.ammo",
                        ammo("762x39", 60, 120),
                        ammo("556x45", 60, 120),
                        ammo("308", 40, 150),
                        ammo("9mm", 90, 90),
                        ammo("45acp", 75, 90),
                        ammo("57x28", 100, 110),
                        ammo("50ae", 28, 100),
                        ammo("792x57", 20, 120),
                        ammo("30_06", 20, 130),
                        ammo("338", 20, 180),
                        ammo("12g", 24, 100))));
        return tab;
    }

    static Tab armor() {
        Tab tab = tab("buymenu.unrealzaruba.tab.armor", Layout.SETS);
        tab.groups.add(group("buymenu.unrealzaruba.group.armor_sets",
                armorSet("buymenu.unrealzaruba.set.soldier", "hammer", "buymenu.unrealzaruba.set.soldier.note"),
                armorSet("buymenu.unrealzaruba.set.scout", "hunter", "buymenu.unrealzaruba.set.scout.note"),
                armorSet("buymenu.unrealzaruba.set.marksman", "closed", "buymenu.unrealzaruba.set.marksman.note"),
                armorSet("buymenu.unrealzaruba.set.engineer", "bloodprisoner", "buymenu.unrealzaruba.set.engineer.note")));
        return tab;
    }

    private static Category armorSet(String name, String helmet, String note) {
        Category set = category(name, null,
                armorPiece("buymenu.unrealzaruba.slot.head", helmet + "_helmet", helmet + "as_helmet", 300),
                armorPiece("buymenu.unrealzaruba.slot.chest", "breaker_chestplate", "breakeras_chestplate", 650),
                armorPiece("buymenu.unrealzaruba.slot.legs", "anchorpoint_leggings", "anchorpointas_leggings", 300),
                armorPiece("buymenu.unrealzaruba.slot.feet", "socks_boots", "socks_boots", 150));
        set.note = note;
        return set;
    }

    private static Item armorPiece(String slot, String redItem, String blueItem, int price) {
        Item piece = item("protection_pixel:" + redItem, price);
        piece.slot = slot;
        if (!redItem.equals(blueItem)) {
            piece.teamItems = new LinkedHashMap<>(Map.of("BLUE", "protection_pixel:" + blueItem));
        }
        return piece;
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
