package com.dod.unrealzaruba.ModBlocks.ClassAssignerBlock;

import com.dod.unrealzaruba.ModBlocks.ModBlocks;
import com.dod.unrealzaruba.NetworkPackets.NetworkHandler;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.utils.Geometry.Utils;

import net.minecraft.world.item.Equipable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.dod.unrealzaruba.CharacterClass.CharacterClassRegistry;
import com.dod.unrealzaruba.CharacterClass.CharacterClassData;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import net.minecraft.world.item.Item;


public class ClassAssignerBlockEntity extends BlockEntity {
    private String classId;
    private UUID armorStandUUID;

    public ClassAssignerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlocks.CLASS_ASSIGNER_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (classId != null) {
            tag.putString("classId", classId);
        }
        if (armorStandUUID != null) {
            tag.putUUID("armorStandUUID", armorStandUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("classId")) {
            classId = tag.getString("classId");
        }
        if (tag.contains("armorStandUUID")) {
            armorStandUUID = tag.getUUID("armorStandUUID");
        }
        if (level != null && !level.isClientSide && armorStandUUID != null) {
            level.getServer().execute(() -> {
                Entity entity = ((ServerLevel)level).getEntity(armorStandUUID);
                if (entity == null) {
                    updateArmorStand();
                }
            });
        }
    }

    public void openGui(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            openConfigMenu(serverPlayer);
        }
    }

    public void openConfigMenu(ServerPlayer player) {
        NetworkHandler.Screens.openClassAssignerScreen(player, this.getBlockPos(), this.classId);
    }

    public void updateArmorStand() {
        if (level != null && !level.isClientSide) {
            removeArmorStand();

            if (classId != null) {
                CharacterClassData characterClassData = CharacterClassRegistry.getCharacterClass(
                    classId, TeamColor.RED);

                if (characterClassData != null) {
                    createArmorStand(characterClassData);
                }
            }
        }
    }

    private void createArmorStand(CharacterClassData classData) {
        if (level != null && !level.isClientSide) {
            ArmorStand armorStand = new ArmorStand(EntityType.ARMOR_STAND, level);
            armorStand.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
            var facing = this.getBlockState().getValue(ClassAssignerBlock.FACING);
            var y = Utils.getYRotForDirection(facing);
            UnrealZaruba.LOGGER.info("Y: {}", y);
            armorStand.setYRot((float)y);
            armorStand.setCustomName(Component.literal(classData.getDisplayName()));
            armorStand.setCustomNameVisible(true);
            armorStand.setInvisible(false);
            armorStand.setNoGravity(true);
            
            List<ItemStack> kitItems = classData.getKit();
            for (ItemStack item : kitItems) {
                Item itemType = item.getItem();
                if (itemType instanceof Equipable equipable) {
                    EquipmentSlot slot = equipable.getEquipmentSlot();
                    UnrealZaruba.LOGGER.info("Equipable item: {}    Slot: {}", equipable.toString(), slot != null ? slot.getName() : "null");
                    armorStand.setItemSlot(slot, item.copy());
                }

                EquipmentSlot slot = item.getEquipmentSlot();
                UnrealZaruba.LOGGER.info("Item: {}         Slot: {}", item, slot != null ? slot.getName() : "null");

                if (itemType.toString().contains("pumpkin")) {
                    armorStand.setItemSlot(EquipmentSlot.HEAD, item.copy());
                    UnrealZaruba.LOGGER.info("Pumpkin");
                }
                else if (isWeaponOrTool(itemType)) {
                    armorStand.setItemSlot(EquipmentSlot.MAINHAND, item.copy());
                    UnrealZaruba.LOGGER.info("Weapon");
                }
                else if (slot != null) {
                    armorStand.setItemSlot(slot, item.copy());
                    UnrealZaruba.LOGGER.info("Armor");
                }
            }
            
            level.addFreshEntity(armorStand);
            armorStandUUID = armorStand.getUUID();
        }
    }
    
    private boolean isWeaponOrTool(Item item) {
        String itemId = item.toString().toLowerCase();
        return itemId.contains("sword") || 
               itemId.contains("rifle") || 
               itemId.contains("cannon") || 
               itemId.contains("launcher") ||
               itemId.contains("musket") ||
               itemId.contains("laser") ||
               itemId.contains("stick") ||
               itemId.contains("bore");
    }

    public void removeArmorStand() {
        if (armorStandUUID != null && level != null && !level.isClientSide) {
            Entity entity = ((ServerLevel)level).getEntity(armorStandUUID);
            if (entity != null) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            armorStandUUID = null;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ClassAssignerBlockEntity blockEntity) {
        if (!level.isClientSide) {
            // Check if we need to create a new armor stand
            if (blockEntity.classId != null && blockEntity.armorStandUUID == null) {
                // No armor stand exists but we have a class ID, create one
                blockEntity.updateArmorStand();
            } 
            // Check if an existing armor stand has disappeared
            else if (blockEntity.armorStandUUID != null) {
                Entity entity = ((ServerLevel)level).getEntity(blockEntity.armorStandUUID);
                if (entity == null) {
                    blockEntity.updateArmorStand();
                }
            }
        }
    }
}
// [06:28:54] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [BaseGamemode] onServerTick in BaseGamemode
// [06:28:54] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [ConditionalPhase] OnTick: 0
// [06:28:54] [Server thread/WARN] [or.va.co.im.ne.NetworkChannel/]: Ship with ID 7714 has a mass of 0.0, not creating a ShipObject
// [06:28:54] [Server thread/WARN] [or.va.co.im.ne.NetworkChannel/]: Ship with ID 7715 has a mass of 0.0, not creating a ShipObject
// [06:28:54] [Server thread/WARN] [Burger Factory/]: Too many game frames in the game frame queue. Is the physics stage broken?
// [06:28:55] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [BaseGamemode] onServerTick in BaseGamemode
// [06:28:55] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [ConditionalPhase] OnTick: 0
// [06:28:55] [Server thread/WARN] [or.va.co.im.ne.NetworkChannel/]: Ship with ID 7714 has a mass of 0.0, not creating a ShipObject
// [06:28:55] [Server thread/WARN] [or.va.co.im.ne.NetworkChannel/]: Ship with ID 7715 has a mass of 0.0, not creating a ShipObject
// [06:28:55] [Server thread/WARN] [Burger Factory/]: Too many game frames in the game frame queue. Is the physics stage broken?
// [06:28:56] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [BaseGamemode] onServerTick in BaseGamemode
// [06:28:56] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [ConditionalPhase] OnTick: 0
// [06:28:56] [Server thread/WARN] [or.va.co.im.ne.NetworkChannel/]: Ship with ID 7714 has a mass of 0.0, not creating a ShipObject
// [06:28:56] [Server thread/WARN] [or.va.co.im.ne.NetworkChannel/]: Ship with ID 7715 has a mass of 0.0, not creating a ShipObject
// [06:28:56] [Server thread/WARN] [Burger Factory/]: Too many game frames in the game frame queue. Is the physics stage broken?
// [06:28:57] [Server thread/INFO] [co.do.Un.UnrealZaruba/]: [BaseGamemode] onServerTick in BaseGamemod