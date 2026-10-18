package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.ModItems.ModItems;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyCasePacket;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The supply case from a paradrop is worth cash only at the carrier's own base: a player holding it inside their
 * team's zone ({@link TeamZones}) hands it in, and the team (plus a bonus for the carrier) is paid. Meanwhile
 * everyone sees a column of light over it, in the colour of the team that last held it. A case that is dropped
 * (the carrier dies) stays where it fell, with that colour, until someone picks it up.
 */
public final class SupplyCases {
    private static final int INTERVAL_TICKS = 5;
    private static final String TEAM_TAG = "Team";

    private static final Set<UUID> CARRIERS = new HashSet<>();
    private static List<ClientboundSupplyCasePacket.Entry> lastSent = List.of();

    /** A drop's case that has not been cashed in yet: no new automatic drop comes down until it is. */
    private static boolean outstanding;
    /** Whether that case has been seen outside the barrel (carried or lying): when it later is nowhere, it is lost. */
    private static boolean seen;
    private static UUID outstandingDrop;

    private SupplyCases() {}

    /** Called every tick of the battle. */
    static void tick(MinecraftServer server, EconomyConfig.SupplyDrops config, int battleTicks) {
        if (battleTicks % INTERVAL_TICKS != 0) return;
        ServerLevel level = WorldManager.gameLevel;
        if (level == null) return;

        List<ClientboundSupplyCasePacket.Entry> entries = new ArrayList<>();
        Set<UUID> carriers = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive()) continue;
            List<ItemStack> cases = casesOf(player);
            if (cases.isEmpty()) continue;

            TeamContext team = TeamZones.teamOf(player);
            if (team != null && TeamZones.isInOwnZone(player)) {
                cashIn(server, player, team, config.paradrop);
                continue;
            }
            TeamColor color = team == null ? TeamColor.UNDEFINED : team.Color();
            for (ItemStack stack : cases) paint(stack, color);
            carriers.add(player.getUUID());
            if (!CARRIERS.contains(player.getUUID())) {
                com.dod.unrealzaruba.Feed.Feed.event(server, com.dod.unrealzaruba.Feed.FeedKind.CASE, com.dod.unrealzaruba.Feed.Feed.colorOf(color), "feed.case_picked", player.getGameProfile().getName());
                com.dod.unrealzaruba.SoundHandler.Voice.playSplit(server, color, com.dod.unrealzaruba.SoundHandler.VoiceLine.CASE_PICKED_ALLY, com.dod.unrealzaruba.SoundHandler.VoiceLine.CASE_PICKED_ENEMY);
            }
            entries.add(new ClientboundSupplyCasePacket.Entry(player.getId(), player.getX(), player.getY(), player.getZ(), rgb(color)));
        }
        CARRIERS.clear();
        CARRIERS.addAll(carriers);

        int lying = 0;
        for (ItemEntity item : level.getEntities(EntityTypeTest.forClass(ItemEntity.class), entity -> isCase(entity.getItem()))) {
            item.setUnlimitedLifetime(); // a dropped case must not despawn
            item.setInvulnerable(true);
            lying++;
            entries.add(new ClientboundSupplyCasePacket.Entry(item.getId(), item.getX(), item.getY(), item.getZ(),
                    rgb(teamOf(item.getItem()))));
        }
        publish(entries);

        // A case that was taken out of the barrel and is now nowhere (destroyed, lost in the void) no longer holds the drops back
        if (outstanding) {
            if (!carriers.isEmpty() || lying > 0) seen = true;
            else if (seen) {
                outstanding = false;
                UnrealZaruba.LOGGER.info("[SupplyCases] The case is lost, the next drop is free to come");
            }
        }
    }

    /** A drop with a case has been started: until it is cashed in the next automatic drop waits. */
    static void caseDropped() {
        outstanding = true;
        seen = false;
        outstandingDrop = null;
    }

    /** The drop has been assembled and has its id now. */
    static void caseDropAssembled(UUID id) {
        if (outstanding) outstandingDrop = id;
    }

    /** The drop could not be built, or went away: its untouched case went with it. */
    static void caseDropEnded(UUID id) {
        if (outstanding && !seen && (id == null || id.equals(outstandingDrop))) outstanding = false;
    }

    static boolean blocksNextDrop() {
        return outstanding;
    }

    /** Nothing is going on (no battle): make sure no stale beams stay up. */
    static void idle() {
        if (!lastSent.isEmpty() || !CARRIERS.isEmpty()) reset();
    }

    public static void reset() {
        outstanding = false;
        seen = false;
        outstandingDrop = null;
        CARRIERS.clear();
        lastSent = List.of();
        if (WorldManager.server != null) {
            PacketDistributor.sendToAllPlayers(new ClientboundSupplyCasePacket(List.of()));
        }
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundSupplyCasePacket(lastSent));
    }

    private static void publish(List<ClientboundSupplyCasePacket.Entry> entries) {
        if (entries.equals(lastSent)) return;
        lastSent = entries;
        PacketDistributor.sendToAllPlayers(new ClientboundSupplyCasePacket(entries));
    }

    private static void cashIn(MinecraftServer server, ServerPlayer player, TeamContext team, EconomyConfig.Paradrop settings) {
        int count = player.getInventory().clearOrCountMatchingItems(SupplyCases::isCase, Integer.MAX_VALUE,
                player.inventoryMenu.getCraftSlots());
        if (count <= 0) return;
        player.containerMenu.broadcastChanges();
        outstanding = false; // the case is cashed in: the next drop may come

        int teamReward = Math.max(0, settings.caseTeamReward) * count;
        int bonus = Math.max(0, settings.caseCarrierBonus) * count;
        for (ServerPlayer member : SideIncome.onlineMembers(server, team)) {
            Wallets.add(member, teamReward + (member.getUUID().equals(player.getUUID()) ? bonus : 0));
        }
        if (!SideIncome.onlineMembers(server, team).contains(player)) Wallets.add(player, bonus);
        com.dod.unrealzaruba.Feed.Feed.event(server, com.dod.unrealzaruba.Feed.FeedKind.CASE, com.dod.unrealzaruba.Feed.Feed.colorOf(team.Color()), "feed.case_cashed",
                player.getGameProfile().getName(), Money.format(teamReward + bonus));
        com.dod.unrealzaruba.SoundHandler.Voice.playSplit(server, team.Color(), com.dod.unrealzaruba.SoundHandler.VoiceLine.CASE_CASHED_ALLY, com.dod.unrealzaruba.SoundHandler.VoiceLine.CASE_CASHED_ENEMY);
    }

    private static List<ItemStack> casesOf(ServerPlayer player) {
        List<ItemStack> cases = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isCase(stack)) cases.add(stack);
        }
        return cases;
    }

    public static boolean isCase(ItemStack stack) {
        return stack.is(ModItems.SUPPLY_CASE.get());
    }

    /** Remembers which team last held the case, so its beam keeps that colour after it is dropped. */
    private static void paint(ItemStack stack, TeamColor color) {
        if (color.name().equals(teamName(stack))) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TEAM_TAG, color.name()));
    }

    private static String teamName(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(TEAM_TAG);
    }

    private static TeamColor teamOf(ItemStack stack) {
        return TeamColor.fromString(teamName(stack));
    }

    private static int rgb(TeamColor color) {
        return switch (color) {
            case RED -> 0xFF3B30;
            case BLUE -> 0x3B82FF;
            case YELLOW -> 0xFFD60A;
            case PURPLE -> 0xB347EA;
            default -> 0xFFFFFF;
        };
    }
}
