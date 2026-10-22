package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Gamemodes.Barriers.BarrierVolumesData;
import com.dod.unrealzaruba.Gamemodes.GamePhases.AbstractGamePhase;
import com.dod.unrealzaruba.Gamemodes.GamePhases.ConditionalPhase;
import com.dod.unrealzaruba.Gamemodes.GamePhases.PhaseId;
import com.dod.unrealzaruba.ModBlocks.VehicleSpawn.VehicleSpawnData;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.Gamemodes.GamePhases.IPhaseHolder;
import com.dod.unrealzaruba.SoundHandler.ModSounds;
import com.dod.unrealzaruba.SoundHandler.SoundHandler;
import com.dod.unrealzaruba.TeamLogic.TeamData;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.NBT;
import com.dod.unrealzaruba.utils.Timers.TimerManager;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import com.dod.unrealzaruba.Economy.EconomyData;
import com.dod.unrealzaruba.Economy.Wallets;
import com.dod.unrealzaruba.Economy.SideIncome;
import com.dod.unrealzaruba.Economy.TeamStorage;
import com.dod.unrealzaruba.WorldManager.MapPacks;
import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.dod.unrealzaruba.NetworkPackets.UpdateDeathTimerPacket;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectivesData;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectivesHandler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;


public abstract class BaseGamemode implements IPhaseHolder {
    protected static final int RESPAWN_DURATION_SECONDS = 5;
    protected ObjectivesHandler objectivesHandler;
    protected AbstractGamePhase currentPhase;
    // Smotri, kakuyu huinyu ya pridumal
    protected ConditionalPhase conditionalPhase;
    protected List<AbstractGamePhase> phases = new ArrayList<>();
    protected int currentPhaseIndex = 0;
    protected MinecraftServer server;
    private boolean isConditionalPhase = false;

    public BaseGamemode() {
        GamemodeManager.instance.SetActiveGamemode(this);
        new TeamData(this.getClass());
        new VehicleSpawnData(this.getClass());
        new BarrierVolumesData(this.getClass());
        new EconomyData(this.getClass());
        new ScoringData(this.getClass());
        this.server = ServerLifecycleHooks.getCurrentServer();
        Wallets.reset(server);
        SideIncome.reset();
        TeamStorage.reset();
        // A new match, or a restarted mode: the previous one leaves no vehicles or purchases on the clients
        com.dod.unrealzaruba.Vehicles.TrackedVehicles.reset();
        com.dod.unrealzaruba.Vehicles.PurchaseSessions.reset();
        MapPacks.select(server, this.getClass().getSimpleName());
        WorldManager.ReloadGameWorldDelayed(this);
    }

    public void LateInitialize() {
        new ObjectivesData(this.getClass());
        objectivesHandler = new ObjectivesHandler();
    }

    /** Whether the fighting part of the match is running (side income only pays then). */
    public boolean IsBattleActive() {
        PhaseId phase = GetCurrentPhaseId();
        return phase == PhaseId.BATTLE || phase == PhaseId.GAME;
    }

    /** A team's score for catch-up income, or null if this gamemode has no comparable score. */
    public Integer GetTeamScore(com.dod.unrealzaruba.Commands.Arguments.TeamColor team) {
        return null;
    }

    /** Where players will be when the match starts; the map loader loads the chunks around these first. */
    public java.util.Collection<net.minecraft.core.BlockPos> GetPreloadPositions() {
        return List.of();
    }

    public AbstractGamePhase GetCurrentPhase() {
        return currentPhase;
    }

    protected abstract void Initialize();
    public abstract void Cleanup();

    public abstract void HandleConnectedPlayer(Player player);
    public abstract void CheckObjectives();

    public IPhaseHolder AddPhase(AbstractGamePhase phase) {
        phases.add(phase);
        return this;
    }

    public AbstractGamePhase Build() {
        return null;
    }

    public void BeginPhase(AbstractGamePhase phase) {
        UnrealZaruba.LOGGER.info("Beginning phase " + phase.GetPhaseId());
        currentPhase = phase;
        if (phase instanceof ConditionalPhase conditionalPhase) {
            conditionalPhase.SetOnConditionMet(() -> {
                CompletePhase();
            });
            isConditionalPhase = true;
            this.conditionalPhase = conditionalPhase;
        }
        else {
            isConditionalPhase = false;
            conditionalPhase = null;
        }

        currentPhase.OnStart();
    }

    public void TransitToNextPhase() {
        currentPhaseIndex++;
        if (currentPhaseIndex < phases.size())
        {
            BeginPhase(phases.get(currentPhaseIndex));
        }
        else
        {
            UnrealZaruba.LOGGER.warn("NO MORE PHASES LEFT");
        }
    }

    public void CompletePhase() {
        currentPhase.OnCompleted();
        TransitToNextPhase();
    }

    public void CompletePhase(PhaseId phaseId) {
        if (currentPhase.GetPhaseId() == phaseId) {
            CompletePhase();
        } else {
            throw new RuntimeException("Expected phase " + phaseId + " but got " + currentPhase.GetPhaseId());
        }
    }
    
    public void ProceedToPhaseForced(PhaseId phaseId) {
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).GetPhaseId() == phaseId) {
                currentPhaseIndex = i;
                BeginPhase(phases.get(i));
                return;
            }
        }
    }
    
    public Optional<AbstractGamePhase> GetPhaseById(PhaseId phaseId) {
        return phases.stream()
                .filter(phase -> phase.GetPhaseId() == phaseId)
                .findFirst();
    }
    
    public PhaseId GetCurrentPhaseId() {
        return currentPhase != null ? currentPhase.GetPhaseId() : PhaseId.UNDEFINED;
    }

    public void StartGame() {
        CompletePhase(PhaseId.TEAM_SELECTION);
    }

    public void EndGame() {
        if (currentPhase.GetPhaseId() == PhaseId.GAME) {
            CompletePhase();
        }
    }

    public int StartGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        StartGame();
        return 1;
    }

    public void onServerTick(ServerTickEvent.Pre event) {
        if (objectivesHandler != null) objectivesHandler.onServerTick();
        if (isConditionalPhase) { // По сути тикают помимо этой фазы ещё ТаймедФазы, но по внутреннему таймеру
            conditionalPhase.OnTick(0);
        }
    }

    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (objectivesHandler != null) objectivesHandler.onPlayerTick(event);
    }

    public void HandleRespawn(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(false);
        CharacterClassEquipper.equipPlayerWithSelectedClass(player);
        NBT.addEntityTag(player, "isPlayerDead", 0);
        SoundHandler.playSoundToPlayer(player, ModSounds.RESPAWN2.get(), 1.0f, 1.0f);
    }

    /** Where each dead player's body lies: the camera of the flyover rises over it. */
    private final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> deathSpots = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Float> deathYaws = new java.util.concurrent.ConcurrentHashMap<>();
    /** Dead players whose waiting is over: they return when they confirm a respawn point. */
    private final java.util.Set<java.util.UUID> awaiting = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Players who died for real and wait for their respawn. The death is vanilla, so the Corpse mod leaves a body with their items. */
    private final java.util.Set<java.util.UUID> dead = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Whether this death is a real one (a body stays, a respawn timer follows); otherwise it is undone and the player is healed. */
    protected boolean IsRealDeath(ServerPlayer player) {
        return currentPhase.GetPhaseId() != PhaseId.TEAM_SELECTION;
    }

    /** Where a player comes back to after dying, in the game world; null for the default. */
    protected BlockPos DeathRespawnPosition(ServerPlayer player) {
        return null;
    }

    // Помянем усопших
    public void HandleDeath(ServerPlayer player, LivingDeathEvent event) {
        if (!IsRealDeath(player)) {
            player.setHealth(20);
            player.getFoodData().setFoodLevel(20);
            event.setCanceled(true);
            return;
        }

        ServerPlayer killerPlayer = resolveKiller(player, event.getSource());
        int reward = 0;
        if (killerPlayer != null) {
            reward = isEnemy(killerPlayer, player) ? EconomyData.current().killReward : 0;
            if (reward > 0) {
                Wallets.add(killerPlayer, reward);
                killerPlayer.displayClientMessage(Component.translatable("economy.unrealzaruba.kill_reward", player.getDisplayName(), Money.format(reward)), true);
            } else {
                killerPlayer.displayClientMessage(Component.translatable("economy.unrealzaruba.kill", player.getDisplayName()), true);
            }
            player.displayClientMessage(Component.literal("You have been killed by " + killerPlayer.getDisplayName().getString()), true);
        }
        ServerPlayer enemyKiller = killerPlayer != null && isEnemy(killerPlayer, player) ? killerPlayer : null;
        int bounty = SideIncome.onDeath(player, enemyKiller, server);
        com.dod.unrealzaruba.Feed.MatchStats.onDeath(player, enemyKiller);
        int streak = enemyKiller == null ? 0 : SideIncome.streakOf(enemyKiller);
        // The kill line of the event feed: the weapon, the teams, what it paid
        com.dod.unrealzaruba.Feed.Feed.kill(server, player, killerPlayer != player ? killerPlayer : null, event.getSource(), reward, bounty, streak);
        SoundHandler.playSoundToPlayer(player, ModSounds.DEATH.get(), 1.0f, 1.0f);

        java.util.UUID id = player.getUUID();
        dead.add(id);
        deathSpots.put(id, player.position());
        deathYaws.put(id, player.getYRot());
        // Every respawn is chosen again: the point from the last life does not carry over
        if (PlayerContext.Get(id) instanceof TeamPlayerContext deadContext) deadContext.DeselectRespawnPoint();
        // Back at the base of the team in the game world, not at the world spawn of the lobby
        BlockPos spawn = DeathRespawnPosition(player);
        if (spawn != null) player.setRespawnPosition(WorldManager.GAME_DIMENSION, spawn, 0f, true, false);
        // The player respawns at once (so there is no death screen); the waiting is done as a spectator on the base
        TimerManager.createTickTimer(2, () -> respawnNow(id), null);
    }

    private void respawnNow(java.util.UUID id) {
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(id);
        if (player == null || !dead.contains(id) || !player.isDeadOrDying()) return;
        player.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
                net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
    }

    /**
     * The player who died has respawned (a new ServerPlayer): they wait as a spectator until the respawn timer is over.
     * Returns false when the player is not one that died here.
     */
    public boolean HandleDeathRespawn(ServerPlayer player) {
        if (!dead.contains(player.getUUID())) return false;
        player.setGameMode(GameType.SPECTATOR);
        player.setHealth(20);
        player.getFoodData().setFoodLevel(20);
        player.setInvulnerable(true);
        player.getInventory().clearContent();
        NBT.addEntityTag(player, "isPlayerDead", 1);
        startRespawnTimer(player.getUUID());
        return true;
    }

    /** Players who left in the middle of the battle: what they carried is lost, and they come back as after a death. */
    private final java.util.Set<java.util.UUID> leftInBattle = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * A player leaves while alive in the battle: leaving counts as a death (their things stay on the ground, the death is counted), so
     * a way out cannot be used to keep the inventory or to dodge a death.
     */
    public void HandleLeaveInMatch(ServerPlayer player) {
        if (server == null || !server.isRunning() || !IsBattleActive() || !IsRealDeath(player)) return;
        if (dead.contains(player.getUUID()) || player.isSpectator() || player.isDeadOrDying()) return;
        player.getInventory().dropAll();
        com.dod.unrealzaruba.Feed.MatchStats.onDeath(player, null);
        leftInBattle.add(player.getUUID());
    }

    /** A player who left in the battle has come back: they return to the base with their class, as after a death. */
    public void ApplyLeavePenalty(ServerPlayer player) {
        if (!leftInBattle.remove(player.getUUID())) return;
        player.setHealth(player.getMaxHealth());
        HandleRespawn(player);
    }

    /** A player left: they are no longer waiting for a respawn (the login brings them back to their base). */
    public void forgetDead(java.util.UUID id) {
        dead.remove(id);
        awaiting.remove(id);
        deathSpots.remove(id);
        deathYaws.remove(id);
    }

    /**
     * Who gets the kill. Normally the player who dealt the damage; for a shot from a vehicle or an explosion that does not name a player
     * (a cannon shell, a blast) the owner of the projectile, then whoever last hurt the victim, and failing that the nearest enemy of the
     * victim in reach of the weapon. Falls, the void and the like are nobody's kill.
     */
    private ServerPlayer resolveKiller(ServerPlayer victim, net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer k) return k;
        for (net.minecraft.world.entity.Entity cause : new net.minecraft.world.entity.Entity[]{source.getEntity(), source.getDirectEntity()}) {
            if (cause instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) return owner;
        }
        if (victim.getKillCredit() instanceof ServerPlayer credit && credit != victim) return credit;
        boolean weapon = source.getEntity() != null || source.getDirectEntity() != null
                || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) || source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
        if (!weapon || server == null) return null;
        ServerPlayer nearest = null;
        double best = 100.0 * 100.0;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == victim || other.level() != victim.level() || other.isSpectator() || !isEnemy(other, victim)) continue;
            double distance = other.distanceToSqr(victim);
            if (distance < best) {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }

    // No money for suicides and team kills
    private static boolean isEnemy(ServerPlayer killer, ServerPlayer victim) {
        if (killer == victim) return false;
        PlayerContext killerContext = PlayerContext.Get(killer.getUUID());
        PlayerContext victimContext = PlayerContext.Get(victim.getUUID());
        if (killerContext instanceof TeamPlayerContext killerTeam && victimContext instanceof TeamPlayerContext victimTeam
                && killerTeam.Team() != null) {
            return killerTeam.Team() != victimTeam.Team();
        }
        return true;
    }

    private void startRespawnTimer(java.util.UUID id) {
        TimerManager.createRealTimeTimer(
            RESPAWN_DURATION_SECONDS * 1000,
            () -> {
                ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(id);
                if (player == null || !dead.contains(id)) return;
                awaiting.add(id);
                OnRespawnReady(player);
            },
            ticks -> updateRespawnTimer(id, ticks)
        );
    }

    /**
     * The waiting is over. By default the player returns at once; a team mode waits until the player has chosen where
     * (see {@link #confirmRespawn}).
     */
    protected void OnRespawnReady(ServerPlayer player) {
        confirmRespawn(player);
    }

    /** The player confirmed their respawn point: they come back. Nothing happens unless their waiting is over. */
    public boolean confirmRespawn(ServerPlayer player) {
        java.util.UUID id = player.getUUID();
        if (!awaiting.remove(id)) return false;
        dead.remove(id);
        deathSpots.remove(id);
        deathYaws.remove(id);
        HandleRespawn(player);
        return true;
    }

    /** Where the body of a dead player lies, or null. */
    protected net.minecraft.world.phys.Vec3 deathSpotOf(java.util.UUID id) {
        return deathSpots.get(id);
    }

    protected float deathYawOf(java.util.UUID id) {
        return deathYaws.getOrDefault(id, 0f);
    }

    /** The camera of a dead player: a slow rise over the place of the body, turning a little. */
    private void flyOverBody(ServerPlayer player, int ticks) {
        net.minecraft.world.phys.Vec3 spot = deathSpots.get(player.getUUID());
        if (spot == null || server == null) return;
        net.minecraft.server.level.ServerLevel level = server.getLevel(WorldManager.GAME_DIMENSION);
        if (level == null) return;
        // Placed over the body once; the rise itself is drawn by the client (a teleport every tick would be jerky)
        if (ticks == 0) player.teleportTo(level, spot.x, spot.y + 1.5, spot.z, deathYaws.getOrDefault(player.getUUID(), 0f), 70f);
    }

    private void updateRespawnTimer(java.util.UUID id, int ticks) {
        ServerPlayer flyer = server == null ? null : server.getPlayerList().getPlayer(id);
        if (flyer != null && dead.contains(id)) flyOverBody(flyer, ticks);
        if (ticks % 20 != 0) {
            return;
        }

        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(id);
        if (player == null) return;
        int remainingSeconds = RESPAWN_DURATION_SECONDS - (ticks / 20);
        PacketDistributor.sendToPlayer(player, new UpdateDeathTimerPacket(remainingSeconds));
    }
}
