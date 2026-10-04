package com.dod.unrealzaruba.ModItems;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

@EventBusSubscriber(modid = UnrealZaruba.MOD_ID)
public class CreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UnrealZaruba.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
            "unreal_zaruba_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.HAND_ASSEMBLER.get())) // safe supplier
                    .title(Component.literal("Unreal Zaruba"))
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }

    @SubscribeEvent
    public static void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == MAIN_TAB.get()) {
            event.accept(ModItems.TENT.get());
            event.accept(ModItems.HAND_ASSEMBLER.get());
            event.accept(ModItems.RED_BLOCK_ITEM.get());
            event.accept(ModItems.BLUE_BLOCK_ITEM.get());
            event.accept(ModItems.YELLOW_BLOCK_ITEM.get());
            event.accept(ModItems.RED_SPAWN_ITEM.get());
            event.accept(ModItems.BLUE_SPAWN_ITEM.get());
            event.accept(ModItems.SEAT_WITHOUT_COLLISION_ITEM.get());
            event.accept(ModItems.TENT_MAIN_BLOCK_BLUE_ITEM.get());
            event.accept(ModItems.TENT_MAIN_BLOCK_RED_ITEM.get());
            event.accept(ModItems.VEHICLE_PURCHASE_BLOCK_ITEM.get());
            event.accept(ModItems.CLASS_ASSIGNER_BLOCK_ITEM.get());
        }
    }
}
