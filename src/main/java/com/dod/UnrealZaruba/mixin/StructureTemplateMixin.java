//package com.dod.UnrealZaruba.mixin;
//
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import java.util.List;
//
//
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;
//
//@Mixin(value = StructureTemplate.class)
//public class StructureTemplateMixin implements StructureTemplateAccessor {
//    @Shadow
//    @Final
//    private List<StructureTemplate.Palette> palettes;
//
//    @Override
//    public List<Palette> getPalettes() {
//        return palettes;
//    }
//}
//
