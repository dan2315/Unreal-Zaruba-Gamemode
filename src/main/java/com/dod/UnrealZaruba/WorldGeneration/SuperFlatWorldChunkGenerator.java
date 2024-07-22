package com.dod.UnrealZaruba.WorldGeneration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class SuperFlatWorldChunkGenerator extends ChunkGenerator {

    public SuperFlatWorldChunkGenerator(Registry<StructureSet> p_207960_, Optional<HolderSet<StructureSet>> p_207961_,
            BiomeSource p_207962_) {
        super(p_207960_, p_207961_, p_207962_);
        //TODO Auto-generated constructor stub
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'codec'");
    }

    @Override
    public ChunkGenerator withSeed(long p_62156_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'withSeed'");
    }

    @Override
    public Sampler climateSampler() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'climateSampler'");
    }

    @Override
    public void applyCarvers(WorldGenRegion p_187691_, long p_187692_, BiomeManager p_187693_,
            StructureFeatureManager p_187694_, ChunkAccess p_187695_, Carving p_187696_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyCarvers'");
    }

    @Override
    public void buildSurface(WorldGenRegion p_187697_, StructureFeatureManager p_187698_, ChunkAccess p_187699_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'buildSurface'");
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion p_62167_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'spawnOriginalMobs'");
    }

    @Override
    public int getGenDepth() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getGenDepth'");
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor p_187748_, Blender p_187749_,
            StructureFeatureManager p_187750_, ChunkAccess p_187751_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fillFromNoise'");
    }

    @Override
    public int getSeaLevel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSeaLevel'");
    }

    @Override
    public int getMinY() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMinY'");
    }

    @Override
    public int getBaseHeight(int p_156153_, int p_156154_, Types p_156155_, LevelHeightAccessor p_156156_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBaseHeight'");
    }

    @Override
    public NoiseColumn getBaseColumn(int p_156150_, int p_156151_, LevelHeightAccessor p_156152_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBaseColumn'");
    }

    @Override
    public void addDebugScreenInfo(List<String> p_208054_, BlockPos p_208055_) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addDebugScreenInfo'");
    }

    
    
}
