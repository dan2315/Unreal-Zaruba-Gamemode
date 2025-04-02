package com.dod.UnrealZaruba.ModBlocks.BlockEntities;

import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.Vehicles.VehicleData;
import com.dod.UnrealZaruba.Vehicles.VehicleRegistry;
import com.dod.UnrealZaruba.UI.VehiclePurchaseMenu.VehiclePurchaseMenu;

import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import java.io.InputStream;

import javax.annotation.Nullable;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;


public class VehiclePurchaseBlockEntity extends BlockEntity implements MenuProvider {

    private ResourceLocation schematicLocation;
    private SchematicWorld schematicWorld;
    private SchematicRenderer renderer;
    private boolean rendererInitialized = false;
    private String selectedVehicle;

    public VehiclePurchaseBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlocks.VEHICLE_PURCHASE_BLOCK_ENTITY.get(), blockPos, blockState);
    }
    
    public String getSelectedVehicle() {
        return selectedVehicle;
    }

    public void setSelectedVehicle(String selectedVehicle) {
        this.selectedVehicle = selectedVehicle;
        this.schematicLocation = VehicleRegistry.GetLocation(selectedVehicle);
        setChanged();

        if (level != null && !level.isClientSide) {
            SendUpdatedSchematicToClient();
        }
    }

    private void SendUpdatedSchematicToClient() {
        if (level instanceof ServerLevel) {
            CompoundTag compoundTag = new CompoundTag();
            saveAdditional(compoundTag);

            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            ((ServerLevel) level).getChunkSource().chunkMap
            .getPlayers(new ChunkPos(this.getBlockPos()), false)
            .forEach(player -> player.connection.send(packet));
        }
    }

    private void LoadSchematicBlueprint() {
        try {
            InputStream inputStream = Minecraft.getInstance().getResourceManager().getResource(schematicLocation).get().open();
            CompoundTag compoundTag = NbtIo.readCompressed(inputStream);
            inputStream.close();

            StructureTemplate structureTemplate = new StructureTemplate();
            structureTemplate.load(level.holderLookup(Registries.BLOCK), compoundTag);

            structureTemplate.placeInWorld(
                    schematicWorld,
                    new BlockPos(0, 3, 0),
                    BlockPos.ZERO,
                    new StructurePlaceSettings(),
                    schematicWorld.getRandom(),
                    Block.UPDATE_CLIENTS);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clientTick() {
        if (level != null && level.isClientSide) {
            if (!rendererInitialized) {
                InitializeRenderer();
                rendererInitialized = true;
            }

            if (renderer != null) {
                renderer.tick();
            }
        }
    }

    private void InitializeRenderer() {
        try {
            schematicWorld = new SchematicWorld(level);

            LoadSchematicBlueprint();

            renderer = new SchematicRenderer();

            renderer.display(schematicWorld);
            renderer.setActive(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (schematicLocation != null) {
            compoundTag.putString("schematicLocation", schematicLocation.toString());
        }
        if (selectedVehicle != null) {
            compoundTag.putString("selectedVehicle", selectedVehicle);
        }
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        if (compoundTag.contains("schematicLocation")) {
            schematicLocation = new ResourceLocation(compoundTag.getString("schematicLocation"));
        }
        if (compoundTag.contains("selectedVehicle")) {
            selectedVehicle = compoundTag.getString("selectedVehicle");
        }

        if (level != null && level.isClientSide()) {
            rendererInitialized = false;
            if (renderer != null) {
                renderer.setActive(false);
                renderer = null;
            }
        }
    }
    
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide() && renderer != null) {
            renderer.setActive(false);
            renderer = null;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vehicle_purchase");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VehiclePurchaseMenu(containerId, inventory, this, ContainerLevelAccess.create(level, worldPosition));
    }
}
