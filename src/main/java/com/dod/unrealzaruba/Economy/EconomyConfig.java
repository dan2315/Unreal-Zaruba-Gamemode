package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-map economy settings, stored as unrealzaruba/gamemodedata/&lt;Gamemode&gt;/economy.json.
 *
 * <p>Buy menu layout: tabs -> groups -> categories -> items. A tab with layout "grid" shows categories in a side
 * list and their items as cards (one item per purchase). A tab with layout "sets" shows each category as a set the
 * player can buy whole or piece by piece.</p>
 *
 * <p>Names and notes can be plain text or lang keys. An item without a name uses the item's own name.
 * {@code item} uses /give syntax, e.g. {@code tacz:ammo[custom_data={AmmoId:"tacz:9mm"}]}.</p>
 */
public class EconomyConfig {
    public int startingMoney = 1000;
    public int killReward = 300;
    public boolean buyMenuEnabled = true;
    /** The buy menu only opens in the team's respawn zones (around respawn points and in held capture areas). */
    public boolean buyOnlyInRespawnZones = true;
    /** Radius of the zone around respawn points that aren't capture points (base, tents, vehicles). */
    public int respawnZoneRadius = 20;

    public int defaultVehiclePrice = 1000;
    /** Vehicles offered at vehicle purchase blocks on this map, in menu order. */
    public List<String> vehicles = new ArrayList<>();
    /** Overrides defaultVehiclePrice for specific vehicles. */
    public Map<String, Integer> vehiclePrices = new LinkedHashMap<>();

    public List<Tab> buyMenu = new ArrayList<>();

    /** Vehicles spawned from shared schematics at vehicle purchase blocks. They cost defaultVehiclePrice. */
    public SchematicSpawns schematicSpawns = new SchematicSpawns();

    public static class SchematicSpawns {
        public boolean enabled = true;
        /** Biggest blueprint allowed, by block count and by its longest side. */
        public int maxBlocks = 6000;
        public int maxSize = 48;
        /** Per purchase block, after a successful spawn. */
        public int cooldownSeconds = 30;
        /** Free blocks between the purchase block and the vehicle's edge. */
        public int gap = 2;
    }

    // Side income, separate from the objectives, so teams choose between pushing the objective and growing richer
    public PassiveIncome passiveIncome = new PassiveIncome();
    public Bounties bounties = new Bounties();
    public SupplyDrops supplyDrops = new SupplyDrops();

    /** Paid to every team member each interval during the battle; the team behind on score gets a catch-up bonus. */
    public static class PassiveIncome {
        public int intervalSeconds = 60;
        public int amount = 100;
        /** Extra per point the team is behind the leader, up to maxCatchUp. */
        public int catchUpPerPoint = 2;
        public int maxCatchUp = 300;
    }

    /** From killStreak kills without dying, a player carries a bounty paid to the enemy who kills them. */
    public static class Bounties {
        public int killStreak = 3;
        public int amountPerKill = 150;
        public int maxAmount = 1500;
    }

    /** Crates dropped at one of {@code locations} during the battle. Opening one pays everyone on the opener's team. */
    public static class SupplyDrops {
        public boolean enabled = true;
        public int firstDropSeconds = 120;
        public int intervalSeconds = 240;
        /** How long a player must stay next to the crate to open it. */
        public int openSeconds = 6;
        /** Removed if still there after this long. */
        public int lifetimeSeconds = 180;
        public int teamReward = 250;
        /** Loot rolls per crate, each picking an entry from loot by weight. */
        public int lootRolls = 4;
        /** Where the crate goes (the crate block itself). Add spots with /supplydrop addspot. */
        public List<net.minecraft.core.BlockPos> locations = new ArrayList<>();
        public List<Loot> loot = DefaultBuyMenu.supplyLoot();
    }

    public static class Loot {
        public String item;
        public int count = 1;
        public int weight = 1;
    }

    public static class Tab {
        public String name;
        public String layout = Layout.GRID;
        public List<Group> groups = new ArrayList<>();
    }

    public static class Group {
        public String name;
        public List<Category> categories = new ArrayList<>();
    }

    public static class Category {
        public String name;
        /** Grid: line under the category title. Sets: small label above the set name, e.g. "TIER 1". */
        public String hint;
        /** Sets: description on the set card. */
        public String note;
        /** Sets: 0-100 bars on the set card. */
        public Map<String, Integer> stats;
        public List<Item> items = new ArrayList<>();
    }

    public static class Item {
        public String name;
        public String note;
        /** Sets: slot label shown above the piece name, e.g. "HEAD". */
        public String slot;
        public String item;
        /** Team color (RED, BLUE, ...) -> item string, for team-colored variants. Falls back to {@link #item}. */
        public Map<String, String> teamItems;
        public int count = 1;
        public int price;
        /** 0-100 bars in the detail panel. */
        public Map<String, Integer> stats;
        /** Label -> value rows in the detail panel. */
        public Map<String, String> info;
    }

    public static final class Layout {
        public static final String GRID = "grid";
        public static final String SETS = "sets";

        private Layout() {}
    }

    public int vehiclePrice(String vehicle) {
        Integer price = vehiclePrices != null ? vehiclePrices.get(vehicle) : null;
        return price != null ? price : defaultVehiclePrice;
    }

    public static EconomyConfig createDefault() {
        EconomyConfig config = new EconomyConfig();
        config.vehicles.addAll(List.of("pt", "car", "tank"));
        config.buyMenu.add(DefaultBuyMenu.weapons());
        config.buyMenu.add(DefaultBuyMenu.armor());
        return config;
    }
}
