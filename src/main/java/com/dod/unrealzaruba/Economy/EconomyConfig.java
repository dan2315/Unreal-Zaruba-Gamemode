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
    /**
     * A death with no clear killer (a fall, a blast or a shell that names nobody): the reward is shared by the enemies within this
     * many blocks of the body, or goes to the nearest enemy when none is that close.
     */
    public int unclearKillRadius = 30;
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
        /** The income grows by growthAmount every growthEverySeconds of the battle (0 for no growth), so late minutes can afford more. */
        public int growthEverySeconds = 300;
        public int growthAmount = 50;
    }

    /**
     * Every enemy a player kills adds amountPerKill to what they are worth; the enemy who kills them takes all of it (at most
     * maxAmount) on top of killReward, and it starts again from nothing.
     */
    public static class Bounties {
        public int amountPerKill = 150;
        public int maxAmount = 1500;
    }

    /** Supply drops: parachuting stands with a barrel of loot and a supply case. See {@link Paradrop}. */
    public static class SupplyDrops {
        public boolean enabled = true;
        public Paradrop paradrop = new Paradrop();
    }

    /**
     * A small wooden stand (3x3 floor and roof, four posts, a barrel in the middle) with a parachute pack on its roof,
     * assembled into a physics body above the drop spot. The barrel holds the supply case plus random extra loot.
     */
    public static class Paradrop {
        /** How far above the spot the drop starts, capped by the build height. */
        public int dropHeight = 80;
        /** Speed (blocks per second) the parachute settles the fall at. */
        public double terminalSpeed = 6.0;
        /** The stand's blocks (block ids); a missing or unknown id falls back to the default shown. */
        public String floorBlock = "minecraft:spruce_planks";
        public String roofBlock = "minecraft:spruce_planks";
        public String postBlock = "minecraft:spruce_fence";
        public String barrelBlock = "minecraft:barrel";
        /**
         * Automatic drops: during the battle, after firstDropSeconds and then every intervalSeconds, one parachutes in at a
         * random spot of the area below (instead of the old crates at the map spots), while fewer than maxActive are around.
         * Each drop is removed lifetimeSeconds after it was dropped, opened or not.
         */
        public boolean auto = false;
        public int firstDropSeconds = 120;
        public int intervalSeconds = 240;
        public int lifetimeSeconds = 900;
        public int maxActive = 3;
        public Area area = new Area();
        /**
         * Which way the drops lean: toward the team that is behind. The axis is the one that runs from one base to the other
         * (X or Z; empty for no leaning). Along the other axis the spot is random. Which side belongs to which team is read from
         * the main spawns of the teams.
         */
        public String biasAxis = "X";
        /** How far the drops move toward the team that is behind: 0 stays in the middle, 1 goes to the edge of the area when the lead is full. */
        public double biasStrength = 0.8;
        /** A lead of this share of the score needed to win counts as full. */
        public double biasFullLeadShare = 0.5;
        /** Randomness left along the axis, as a share of the width of the area. */
        public double biasJitter = 0.1;
        /** The item players carry to their base to cash in; put in the middle slot of the barrel. Empty for none. */
        public String cashItem = "unrealzaruba:supply_case";
        /** Paid to every online member of the team that hands a case in at its base, per case. */
        public int caseTeamReward = 600;
        /** Extra for the player who carried it, per case. */
        public int caseCarrierBonus = 300;
        /** Extra random loot added to the barrel: lootRolls weighted picks from loot. */
        public int lootRolls = 3;
        public List<Loot> loot = DefaultBuyMenu.supplyLoot();
    }

    /**
     * Where automatic drops may land: a rectangle seen from above, given by two opposite corners (x1, z1) and (x2, z2) in
     * either order, and the y of the ground there (the drop starts dropHeight above it). Set it with /supplydrop area.
     */
    public static class Area {
        public int x1;
        public int z1;
        public int x2;
        public int z2;
        public int floorY = 64;

        /** An area with no width or no depth is "not set". */
        public boolean isValid() {
            return x1 != x2 && z1 != z2;
        }

        /** A spot random along one axis and at {@code fraction} (0 = the low end, 1 = the high end) of the width along the other. */
        public net.minecraft.core.BlockPos randomSpot(net.minecraft.util.RandomSource random, boolean alongX, double fraction) {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            double f = Math.max(0, Math.min(1, fraction));
            int x = alongX ? minX + (int) Math.round(f * (maxX - minX)) : minX + random.nextInt(maxX - minX + 1);
            int z = alongX ? minZ + random.nextInt(maxZ - minZ + 1) : minZ + (int) Math.round(f * (maxZ - minZ));
            return new net.minecraft.core.BlockPos(x, floorY, z);
        }

        public net.minecraft.core.BlockPos randomSpot(net.minecraft.util.RandomSource random) {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            return new net.minecraft.core.BlockPos(minX + random.nextInt(maxX - minX + 1), floorY, minZ + random.nextInt(maxZ - minZ + 1));
        }
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
        config.buyMenu.add(DefaultBuyMenu.food());
        config.buyMenu.add(DefaultBuyMenu.tools());
        return config;
    }
}
