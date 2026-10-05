package com.dod.unrealzaruba.Vehicles;

import net.minecraft.resources.ResourceLocation;

/** A purchasable vehicle. Prices are per map, see {@link com.dod.unrealzaruba.Economy.EconomyConfig#vehiclePrice}. */
public class VehicleData {
    private final ResourceLocation schematicLocation;
    private final String displayName;

    public ResourceLocation getSchematicLocation() {
        return schematicLocation;
    }

    public ResourceLocation getKey() {
        return schematicLocation; // mega lul
    }

    /** Short name used in the map's economy config, e.g. "car". */
    public String getId() {
        return schematicLocation.getPath();
    }

    public String getName() {
        return displayName;
    }

    public VehicleData(String nameId, String displayName) {
        this.schematicLocation = VehicleRegistry.GetLocation(nameId);
        this.displayName = displayName;
    }
}
