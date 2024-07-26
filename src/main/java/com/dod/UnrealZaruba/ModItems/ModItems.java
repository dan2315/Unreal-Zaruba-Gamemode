package com.dod.UnrealZaruba.ModItems;

import javax.annotation.Nonnull;

import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import com.dod.UnrealZaruba.unrealzaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            unrealzaruba.MOD_ID);

    public static final RegistryObject<Item> HAND_ASSEMBLER = ModItems.ITEMS.register("hand_assembler",
            () -> new HandAssembler(new Item.Properties().tab(CreativeTabs.MAIN_TAB)) {

                // @Override
                // public InteractionResultHolder<ItemStack> use(@Nonnull Level world, @Nonnull Player player,
                //         @Nonnull InteractionHand hand) {
                //     ItemStack itemStack = player.getItemInHand(hand);

                //     if (!world.isClientSide) {
                //         // Define your custom behavior here
                //         player.sendMessage(new TextComponent("You right-clicked with the custom item!"),
                //                 player.getUUID());

                //         // Example: Heal the player
                //         player.heal(4.0F);
                //     }

                //     return InteractionResultHolder.success(itemStack);
                // }
            });
}
