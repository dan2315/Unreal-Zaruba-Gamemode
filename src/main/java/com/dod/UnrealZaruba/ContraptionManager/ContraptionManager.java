package com.dod.UnrealZaruba.ContraptionManager;

import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ContraptionManager {

    public static void CreateContraptionFromSchematic() {
    }

    public static CompoundTag readSchematicFile(String filePath) {
        try (FileInputStream inputStream = new FileInputStream(new File(filePath))) {
            return NbtIo.readCompressed(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void loadSchematicToWorld(ServerLevel world, BlockPos origin, CompoundTag schematic) {
        if (schematic == null) {
            UnrealZaruba.LOGGER.warn("Failed to read schematic file.");
            return;
        }

        ListTag blocks = schematic.getList("blocks", Tag.TAG_COMPOUND);
        ListTag palette = schematic.getList("palette", Tag.TAG_STRING);

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i);
            int state = blockTag.getInt("state");
            ListTag posList = blockTag.getList("pos", Tag.TAG_INT);
            BlockPos pos = new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2));

            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(palette.getString(state)));
            BlockState blockState = block.defaultBlockState();

            world.setBlock(origin.offset(pos), blockState, 2);
        }

    }
}
