package com.dod.UnrealZaruba.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.dod.UnrealZaruba.UnrealZaruba;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UnrealZaruba.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("unreal_zaruba_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.HAND_ASSEMBLER.get())) // Set icon
                    .title(Component.literal("Unreal Zaruba")) // Tab name
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TENT.get());
                        output.accept(ModItems.HAND_ASSEMBLER.get());
                        output.accept(ModItems.RED_BLOCK_ITEM.get());
                        output.accept(ModItems.BLUE_BLOCK_ITEM.get());
                        output.accept(ModItems.RED_SPAWN_ITEM.get());
                        output.accept(ModItems.BLUE_SPAWN_ITEM.get());
                        output.accept(ModItems.SEAT_WITHOUT_COLLISION_ITEM.get());
                        output.accept(ModItems.TENT_MAIN_BLOCK_BLUE_ITEM.get());
                        output.accept(ModItems.TENT_MAIN_BLOCK_RED_ITEM.get());
                    })
                    .build());

    @SubscribeEvent
    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == MAIN_TAB.get()) {
            event.accept(ModItems.TENT.get());
            event.accept(ModItems.HAND_ASSEMBLER.get());
            event.accept(ModItems.RED_BLOCK_ITEM.get());
            event.accept(ModItems.BLUE_BLOCK_ITEM.get());
            event.accept(ModItems.RED_SPAWN_ITEM.get());
            event.accept(ModItems.BLUE_SPAWN_ITEM.get());
            event.accept(ModItems.SEAT_WITHOUT_COLLISION_ITEM.get());
            event.accept(ModItems.TENT_MAIN_BLOCK_BLUE_ITEM.get());
            event.accept(ModItems.TENT_MAIN_BLOCK_RED_ITEM.get());
        }
    }
}
