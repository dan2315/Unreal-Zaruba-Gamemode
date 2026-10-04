package com.dod.unrealzaruba.utils;

import java.util.HashMap;
import java.util.Map;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.io.InputStream;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.DataInputStream;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SchematicLoader {
    private static final Map<ResourceLocation, CompoundTag> SCHEMATIC_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, StructureTemplate> TEMPLATE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, StructureTemplate> VEHICLE_CACHE = new HashMap<>();

    public static CompoundTag GetSchematicNbt(ResourceLocation location) {
        if (SCHEMATIC_CACHE.containsKey(location)) {
            return SCHEMATIC_CACHE.get(location);
        }

        try {
            CompoundTag nbt = LoadFromResources(location);
            SCHEMATIC_CACHE.put(location, nbt);
            return nbt;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CompoundTag LoadFromResources(ResourceLocation location) {
        InputStream inputStream = null;
        try {
            String path = "data/" + location.getNamespace() + "/" + location.getPath() + ".nbt";
            
            inputStream = SchematicLoader.class.getClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                UnrealZaruba.LOGGER.warn("Schematic not found in mod resources: {}", path);
                return null;
            }

            byte[] bytes = inputStream.readAllBytes();
            System.out.println("File size: " + bytes.length + " bytes for " + path);
            
            if (bytes.length == 0) {
                throw new IOException("File is empty: " + path);
            }
            
            inputStream = new ByteArrayInputStream(bytes);
            
            try {
                CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
                if (tag == null) {
                    System.out.println("readCompressed returned null for " + path);
                } else {
                    // It's actually working way, other is fallbacks that could be removed I guess
                    System.out.println("Successfully read NBT with " + tag.size() + " entries"); 
                }
                return tag;
            } catch (IOException e) {
                System.out.println("Failed to read compressed NBT, trying uncompressed: " + e.getMessage());
                
                try {
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
                    CompoundTag tag = NbtIo.read(dataInputStream);
                    if (tag != null) {
                        System.out.println("Successfully read uncompressed NBT with " + tag.size() + " entries");
                        return tag;
                    }
                } catch (IOException e2) {
                    System.out.println("Failed to read uncompressed NBT: " + e2.getMessage());
                }
                
                throw new IOException("File is not a valid NBT format: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static StructureTemplate GetStructureTemplate(ResourceLocation location) {
        if (TEMPLATE_CACHE.containsKey(location)) {
            return TEMPLATE_CACHE.get(location);
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return GetStructureTemplateClient(location);
        }
        Level overworld = server.getLevel(Level.OVERWORLD);
        StructureTemplate template = new StructureTemplate();

        CompoundTag nbt = GetSchematicNbt(location);
        template.load(overworld.holderLookup(Registries.BLOCK), nbt);
        TEMPLATE_CACHE.put(location, template);
        return template;
    }

    public static StructureTemplate GetStructureTemplateClient(ResourceLocation location) {
        Minecraft minecraft = Minecraft.getInstance();

        StructureTemplate template = new StructureTemplate();
        CompoundTag nbt = GetSchematicNbt(location);
        template.load(minecraft.level.holderLookup(Registries.BLOCK), nbt);
        TEMPLATE_CACHE.put(location, template);
        return template;
    }


    // VEHICLES
    /**
     * Loads a vehicle structure template (vanilla structure .nbt, e.g. saved with a structure block).
     * A file at schematics/vehicles/<name>.nbt in the server directory overrides the copy bundled at
     * data/unrealzaruba/vehicles/<name>.nbt.
     */
    public static StructureTemplate GetVehicleTemplate(ResourceLocation location, ServerLevel level) {
        if (VEHICLE_CACHE.containsKey(location)) {
            return VEHICLE_CACHE.get(location);
        }

        try {
            CompoundTag nbt;
            File file = new File("schematics/vehicles/" + location.getPath() + ".nbt");
            if (file.exists()) {
                UnrealZaruba.LOGGER.info("Loading vehicle template from {}", file.getAbsolutePath());
                nbt = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            } else {
                nbt = GetSchematicNbt(ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "vehicles/" + location.getPath()));
            }
            if (nbt == null) {
                return null;
            }

            StructureTemplate template = new StructureTemplate();
            template.load(level.holderLookup(Registries.BLOCK), nbt);
            VEHICLE_CACHE.put(location, template);
            return template;
        } catch (Exception e) {
            UnrealZaruba.LOGGER.error("Failed to load vehicle template {}", location, e);
            return null;
        }
    }

    /**
     * World positions of every non-air block the template will place with the given settings and origin.
     */
    public static List<BlockPos> GetPlacedBlockPositions(StructureTemplate template, StructurePlaceSettings settings, BlockPos origin, ServerLevel level) {
        CompoundTag saved = template.save(new CompoundTag());
        ListTag palette = saved.contains("palette", Tag.TAG_LIST)
                ? saved.getList("palette", Tag.TAG_COMPOUND)
                : saved.getList("palettes", Tag.TAG_LIST).getList(0);
        HolderGetter<Block> blockLookup = level.holderLookup(Registries.BLOCK);

        List<BlockPos> positions = new ArrayList<>();
        ListTag blocks = saved.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i);
            BlockState state = NbtUtils.readBlockState(blockLookup, palette.getCompound(block.getInt("state")));
            if (state.isAir()) {
                continue;
            }
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            BlockPos relative = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            positions.add(StructureTemplate.calculateRelativePosition(settings, relative).offset(origin));
        }
        return positions;
    }
}
