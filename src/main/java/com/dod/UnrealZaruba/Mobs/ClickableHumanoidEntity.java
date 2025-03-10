package com.dod.UnrealZaruba.Mobs;


import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.WorldManager.SimpleWorldManager;

import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

public class ClickableHumanoidEntity extends Mob {

    private Integer id;

    public ClickableHumanoidEntity(EntityType<? extends Mob> p_21368_, Level p_21369_) {
        super(p_21368_, p_21369_);
    }

    public void Initialize(Integer id) {
        this.id = id;
        
    }

    public Integer GetId() {
        return this.id;
    }

    @Override
    public boolean isPushable() {
        return false; // Prevents the mob from being pushed by other entities
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("id", this.id);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("id")) {
            this.id = tag.getInt("id");
        }
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitVec, InteractionHand hand) {
        if (!this.level().isClientSide) {

            switch (id) {
                case 1:
                    teleportPlayerToDimension((ServerPlayer) player, SimpleWorldManager.UnrealZarubaLobby.GetDimension(),0d, 128d, 0d);
                    break;

                default:
                    break;
            }

        }
        return InteractionResult.SUCCESS;
    }


    public void teleportPlayerToDimension(ServerPlayer player, ResourceKey<Level> targetDimension, double x, double y, double z) {
    ServerLevel targetWorld = player.getServer().getLevel(targetDimension); // Get the target dimension's world

    if (targetWorld != null && !player.level().dimension().equals(targetDimension)) {
        player.teleportTo(targetWorld, x, y, z, player.getYRot(), player.getXRot());
    } else {
        player.teleportTo(x, y, z);
    }
}

}
