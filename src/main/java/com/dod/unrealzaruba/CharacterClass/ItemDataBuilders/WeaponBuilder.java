package com.dod.unrealzaruba.CharacterClass.ItemDataBuilders;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class WeaponBuilder {
    private final String weaponId;
    private int ammoCount = 0;
    private int damage = 0;
    
    private String barrelId = null;
    private String scopeId = null;
    private String stockId = null;
    private String underBarrelId = null;
    private String magazineId = null;

    public WeaponBuilder(String weaponId) {
        this.weaponId = weaponId;
    }

    public WeaponBuilder ammoCount(int ammoCount) {
        this.ammoCount = ammoCount;
        return this;
    }

    public WeaponBuilder damage(int damage) {
        this.damage = damage;
        return this;
    }

    public WeaponBuilder barrel(String barrelId) {
        this.barrelId = barrelId;
        return this;
    }

    public WeaponBuilder scope(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }

    public WeaponBuilder stock(String stockId) {
        this.stockId = stockId;
        return this;
    }

    public WeaponBuilder underBarrel(String underBarrelId) {
        this.underBarrelId = underBarrelId;
        return this;
    }

    public WeaponBuilder magazine(String magazineId) {
        this.magazineId = magazineId;
        return this;
    }

    public ItemStack build() {
        ItemStack weaponStack = new ItemStack(
            BuiltInRegistries.ITEM.get(ResourceLocation.parse(weaponId)), 1);
        
        CompoundTag mainTag = new CompoundTag();
        
        mainTag.putInt("AmmoCount", ammoCount);
        mainTag.putInt("Damage", damage);

        CompoundTag attachmentsTag = new CompoundTag();
        
        if (barrelId != null) {
            CompoundTag barrelTag = createAttachmentTag(barrelId);
            attachmentsTag.put("Barrel", barrelTag);
        }
        
        if (scopeId != null) {
            CompoundTag scopeTag = createAttachmentTag(scopeId);
            attachmentsTag.put("Scope", scopeTag);
        }
        
        if (stockId != null) {
            CompoundTag stockTag = createAttachmentTag(stockId);
            attachmentsTag.put("Stock", stockTag);
        }
        
        if (underBarrelId != null) {
            CompoundTag underBarrelTag = createAttachmentTag(underBarrelId);
            attachmentsTag.put("Under_Barrel", underBarrelTag);
        }

        if (magazineId != null) {
            CompoundTag magazineTag = createAttachmentTag(magazineId);
            attachmentsTag.put("Magazine", magazineTag);
        }
        
        mainTag.put("Attachments", attachmentsTag);
        
        // TODO(1.21): this mod item still uses its pre-1.20.5 NBT layout; check the mod's 1.21 data components
        weaponStack.set(DataComponents.CUSTOM_DATA, CustomData.of(mainTag));
        return weaponStack;
    }
    
    private CompoundTag createAttachmentTag(String attachmentId) {
        CompoundTag attachmentTag = new CompoundTag();
        attachmentTag.putString("id", attachmentId);
        attachmentTag.putInt("Count", 1);
        
        CompoundTag damageTag = new CompoundTag();
        damageTag.putInt("Damage", 0);
        attachmentTag.put("tag", damageTag);
        
        return attachmentTag;
    }
    
    public static ItemStack createIronSpear() {
        return new WeaponBuilder("scguns:iron_spear")
            .ammoCount(5)
            .barrel("scguns:muzzle_brake")
            .scope("scguns:medium_scope")
            .stock("scguns:wooden_stock")
            .underBarrel("scguns:vertical_grip")
            .build();
    }
}