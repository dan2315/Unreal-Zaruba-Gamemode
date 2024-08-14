package com.dod.UnrealZaruba.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;


import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;

@Mixin(value = StructureTemplate.class)
public class StructureTemplateMixin {
    @Shadow
    @Final
    private List<StructureTemplate.Palette> palettes;

    @Unique
    public List<Palette> getPalettes() {
        return this.palettes;
    }
}

