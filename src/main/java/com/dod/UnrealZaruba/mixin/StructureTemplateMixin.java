package com.dod.UnrealZaruba.mixin;

import org.checkerframework.common.aliasing.qual.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;


import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(value = StructureTemplate.class)
public class StructureTemplateMixin {
    @Shadow
    @Final
    private List<StructureTemplate.Palette> palettes;

    @Unique
    public List<StructureTemplate.Palette> GetPalletes() {
        return palettes;
    }
}
