package com.dod.unrealzaruba.UI;

import java.lang.reflect.Field;
import java.util.function.Function;

import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * The font of our own screens and overlays: Montserrat from the brand book. It is a Font that answers every request
 * for the default font with Montserrat (which falls back to the vanilla glyphs for what Montserrat does not have), so
 * the screens keep using plain {@code font.width(...)} and {@code drawString(font, ...)}.
 */
public final class UiFont {
    public static final ResourceLocation REGULAR = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "montserrat");
    /** For headings: {@code Component.literal(text).withStyle(Style.EMPTY.withFont(UiFont.BOLD))}. */
    public static final ResourceLocation BOLD = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "montserrat_bold");

    /** Montserrat is switched off: everything uses the game font. Set to true to bring the brand font back. */
    private static final boolean MONTSERRAT = false;

    private static Font font;
    private static Font vanilla;

    private UiFont() {}

    public static Font get() {
        Font current = Minecraft.getInstance().font;
        if (!MONTSERRAT) return current;
        if (font != null && vanilla == current) return font;
        vanilla = current;
        font = build(current);
        return font;
    }

    public static Style bold() {
        return MONTSERRAT ? Style.EMPTY.withFont(BOLD) : Style.EMPTY.withBold(true);
    }

    @SuppressWarnings("unchecked")
    private static Font build(Font base) {
        try {
            Field provider = null;
            for (Field field : Font.class.getDeclaredFields()) {
                if (Function.class.isAssignableFrom(field.getType())) {
                    provider = field;
                    break;
                }
            }
            if (provider == null) return base;
            provider.setAccessible(true);
            Function<ResourceLocation, FontSet> original = (Function<ResourceLocation, FontSet>) provider.get(base);
            return new Font(location -> original.apply(Style.DEFAULT_FONT.equals(location) ? REGULAR : location), false);
        } catch (ReflectiveOperationException | RuntimeException e) {
            UnrealZaruba.LOGGER.warn("[UiFont] Could not set up Montserrat, using the vanilla font: {}", e.toString());
            return base;
        }
    }
}
