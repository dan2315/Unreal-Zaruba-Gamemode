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

    public int defaultVehiclePrice = 1000;
    /** Vehicles offered at vehicle purchase blocks on this map, in menu order. */
    public List<String> vehicles = new ArrayList<>();
    /** Overrides defaultVehiclePrice for specific vehicles. */
    public Map<String, Integer> vehiclePrices = new LinkedHashMap<>();

    public List<Tab> buyMenu = new ArrayList<>();

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
