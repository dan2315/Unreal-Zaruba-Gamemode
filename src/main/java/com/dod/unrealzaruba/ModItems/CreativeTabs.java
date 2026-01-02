package com.dod.unrealzaruba.ModItems;

import com.dod.unrealzaruba.UnrealZaruba;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

@Mod.EventBusSubscriber(modid = UnrealZaruba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UnrealZaruba.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
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
            event.accept(ModItems.TENT);
            event.accept(ModItems.HAND_ASSEMBLER);
            event.accept(ModItems.RED_BLOCK_ITEM);
            event.accept(ModItems.BLUE_BLOCK_ITEM);
            event.accept(ModItems.YELLOW_BLOCK_ITEM);
            event.accept(ModItems.RED_SPAWN_ITEM);
            event.accept(ModItems.BLUE_SPAWN_ITEM);
            event.accept(ModItems.SEAT_WITHOUT_COLLISION_ITEM);
            event.accept(ModItems.TENT_MAIN_BLOCK_BLUE_ITEM);
            event.accept(ModItems.TENT_MAIN_BLOCK_RED_ITEM);
            event.accept(ModItems.VEHICLE_PURCHASE_BLOCK_ITEM);
            event.accept(ModItems.CLASS_ASSIGNER_BLOCK_ITEM);
        }
    }
}
