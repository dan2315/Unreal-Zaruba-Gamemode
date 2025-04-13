package com.dod.UnrealZaruba.ModBlocks.ClassAssignerBlock;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.packs.BundleRecipeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import com.dod.UnrealZaruba.CharacterClass.CharacterClassRegistry;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassData;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;


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
            armorStand.setCustomName(Component.literal(classData.getDisplayName()));
            armorStand.setCustomNameVisible(true);
            armorStand.setInvisible(false);
            armorStand.setNoGravity(true);
            
            List<ItemStack> kitItems = classData.getKit();
            for (ItemStack item : kitItems) {
                Item itemType = item.getItem();

                EquipmentSlot slot = item.getEquipmentSlot();
                UnrealZaruba.LOGGER.info("Item: {} Slot: " + slot);
                if (slot != null) {
                    armorStand.setItemSlot(slot, item.copy());
                }
                else if (itemType.toString().contains("pumpkin")) {
                    armorStand.setItemSlot(EquipmentSlot.HEAD, item.copy());
                }
                else if (isWeaponOrTool(itemType)) {
                    armorStand.setItemSlot(EquipmentSlot.MAINHAND, item.copy());
                }
            }
            // TODO: Убрать и сделать нормально
            armorStand.setItemSlot(EquipmentSlot.FEET, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("protection_pixel:socks_boots")), 1));
            armorStand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("protection_pixel:anchorpoint_leggings")), 1));
            armorStand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("protection_pixel:breaker_chestplate")), 1));
            armorStand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("protection_pixel:hammer_helmet")), 1));
            
            level.addFreshEntity(armorStand);
            armorStandUUID = armorStand.getUUID();
        }
    }
    
    private boolean isWeaponOrTool(Item item) {
        String itemId = item.toString().toLowerCase();
        return itemId.contains("sword") || 
               itemId.contains("rifle") || 
               itemId.contains("gun") || 
               itemId.contains("cannon") || 
               itemId.contains("launcher") ||
               itemId.contains("musket") ||
               itemId.contains("laser") ||
               itemId.contains("stick") ||
               itemId.contains("bore") ||
               itemId.contains("lance");
    }

    private void removeArmorStand() {
        if (armorStandUUID != null && level != null && !level.isClientSide) {
            Entity entity = ((ServerLevel)level).getEntity(armorStandUUID);
            if (entity != null) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            armorStandUUID = null;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ClassAssignerBlockEntity blockEntity) {
        if (!level.isClientSide && blockEntity.armorStandUUID != null) {
            Entity entity = ((ServerLevel)level).getEntity(blockEntity.armorStandUUID);
            if (entity == null) {
                blockEntity.updateArmorStand();
            }
        }
    }
}
