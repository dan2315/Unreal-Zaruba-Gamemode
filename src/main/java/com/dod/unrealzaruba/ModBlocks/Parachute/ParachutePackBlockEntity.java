package com.dod.unrealzaruba.ModBlocks.Parachute;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/**
 * Drag for the physics body this pack is part of. While deployed, every physics step pushes the body against its own
 * velocity at the pack (above the centre of mass, so the load stays level), with the strength picked so the body falls
 * at {@link #terminalSpeed} whatever it weighs. When the body has come to rest the canopy is retracted.
 */
public class ParachutePackBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    public static final double DEFAULT_TERMINAL_SPEED = 6.0;
    /** The canopy opens over this many ticks (also drives the client animation). */
    public static final int OPEN_TICKS = 20;

    /** Physics seconds before landing detection starts, so a drop that starts at rest isn't "landed". */
    private static final double MIN_FLIGHT_SECONDS = 2.5;
    private static final double LANDED_SPEED = 0.6;
    private static final double LANDED_SECONDS = 0.75;
    /** Fraction of the spin removed per second. */
    private static final double ANGULAR_DAMPING = 1.5;

    private volatile boolean deployed;
    private double terminalSpeed = DEFAULT_TERMINAL_SPEED;

    // Physics thread state
    private double flightSeconds;
    private double slowSeconds;
    private volatile boolean retractRequested;

    // Client animation
    private int openTicks;
    private int previousOpenTicks;
    private boolean received;

    public ParachutePackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PARACHUTE_PACK_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setTerminalSpeed(double terminalSpeed) {
        this.terminalSpeed = Math.max(0.5, terminalSpeed);
        setChanged();
    }

    public void setDeployed(boolean deployed) {
        if (this.deployed == deployed) return;
        this.deployed = deployed;
        flightSeconds = 0;
        slowSeconds = 0;
        retractRequested = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 0 (stowed) .. 1 (fully open), smoothed between ticks, for the renderer. */
    public float openAmount(float partialTick) {
        float ticks = previousOpenTicks + (openTicks - previousOpenTicks) * partialTick;
        return Math.max(0, Math.min(1, ticks / OPEN_TICKS));
    }

    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            previousOpenTicks = openTicks;
            openTicks = Math.max(0, Math.min(OPEN_TICKS, openTicks + (deployed ? 1 : -1)));
        } else if (retractRequested) {
            retractRequested = false;
            setDeployed(false);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double delta) {
        if (!deployed || level == null || !body.isValid()) return;

        Vector3d velocity = body.getLinearVelocity(new Vector3d());
        double speed = velocity.length();
        flightSeconds += delta;
        if (flightSeconds > MIN_FLIGHT_SECONDS) {
            slowSeconds = speed < LANDED_SPEED ? slowSeconds + delta : 0;
            if (slowSeconds > LANDED_SECONDS) {
                retractRequested = true; // applied on the server thread in tick()
                return;
            }
        }

        MassData mass = subLevel.getMassTracker();
        if (mass.isInvalid()) return;
        // Linear drag with a rate of g / terminalSpeed balances gravity at exactly the terminal speed
        double gravity = DimensionPhysicsData.getGravity(level).length();
        double rate = gravity / terminalSpeed;
        Vector3d worldImpulse = new Vector3d(velocity).mul(-mass.getMass() * rate * delta);
        Vector3d localImpulse = subLevel.logicalPose().transformNormalInverse(worldImpulse, new Vector3d());
        Vector3d point = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.1, worldPosition.getZ() + 0.5);
        body.applyImpulseAtPoint(point, localImpulse);

        Vector3d spin = body.getAngularVelocity(new Vector3d());
        if (spin.lengthSquared() > 1.0e-6) {
            Vector3d localSpin = subLevel.logicalPose().transformNormalInverse(spin, new Vector3d());
            Vector3d torque = mass.getInertiaTensor().transform(localSpin.mul(-ANGULAR_DAMPING * delta));
            body.applyAngularImpulse(torque);
        }
    }

    // ---- saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Deployed", deployed);
        tag.putDouble("TerminalSpeed", terminalSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        deployed = tag.getBoolean("Deployed");
        if (tag.contains("TerminalSpeed")) terminalSpeed = Math.max(0.5, tag.getDouble("TerminalSpeed"));
        if (level != null && level.isClientSide && !received) {
            // A pack that arrives already deployed (chunk load) shows an open canopy right away; later updates animate
            received = true;
            openTicks = previousOpenTicks = deployed ? OPEN_TICKS : 0;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
