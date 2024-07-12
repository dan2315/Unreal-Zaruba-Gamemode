package com.dod.UnrealZaruba.ModItems;


import org.joml.Vector3i;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import com.dod.UnrealZaruba.unrealzaruba;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            unrealzaruba.MOD_ID);

    public static final RegistryObject<Item> HAND_ASSEMBLER = ModItems.ITEMS.register("hand_assembler",
            () -> new Item(new Item.Properties().tab(CreativeTabs.MAIN_TAB)) {
                @Override
                public InteractionResult useOn(UseOnContext useContext) {
                    unrealzaruba.LOGGER.debug("ASDSADASDASDASDASDASDADASDSAD");
                    Level level = useContext.getLevel();
                    if ((level instanceof ServerLevel) == false) return InteractionResult.FAIL;
                    BlockPos blockPos = useContext.getClickedPos();

                    DenseBlockPosSet denseBlockPosSet = new DenseBlockPosSet();
                    denseBlockPosSet.add(new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                    ShipAssemblyKt.createNewShipWithBlocks(blockPos, denseBlockPosSet, (ServerLevel) useContext.getLevel());
                    return InteractionResult.SUCCESS;
                };
            });
}
