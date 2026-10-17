package com.dod.unrealzaruba.Keybinds;

import java.lang.reflect.Method;

import com.dod.unrealzaruba.UnrealZaruba;
import com.google.gson.JsonObject;

/**
 * Jade reads its file once, so when it is already running the new overlay position is also given to it directly.
 * Reached by name: without Jade nothing happens.
 */
final class JadeLive {
    private JadeLive() {}

    static void apply(String file, JsonObject values) {
        if (!file.replace('\\', '/').equals("jade/jade.json")) return;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> config = Class.forName("snownee.jade.api.config.IWailaConfig", false, loader);
            Class<?> overlayType = Class.forName("snownee.jade.api.config.IWailaConfig$IConfigOverlay", false, loader);
            Object overlay = config.getMethod("getOverlay").invoke(config.getMethod("get").invoke(null));
            set(overlayType, overlay, "setOverlayPosX", values, "overlayPosX");
            set(overlayType, overlay, "setOverlayPosY", values, "overlayPosY");
            set(overlayType, overlay, "setAnchorX", values, "overlayAnchorX");
            set(overlayType, overlay, "setAnchorY", values, "overlayAnchorY");
            set(overlayType, overlay, "setOverlayScale", values, "overlayScale");
        } catch (ClassNotFoundException e) {
            // no Jade
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            UnrealZaruba.LOGGER.info("[Keybinds] Jade is not running yet, its file is read at start: {}", e.toString());
        }
    }

    private static void set(Class<?> overlayType, Object overlay, String setter, JsonObject values, String key) throws ReflectiveOperationException {
        if (!values.has(key)) return;
        Method method = overlayType.getMethod(setter, float.class);
        method.invoke(overlay, values.get(key).getAsFloat());
    }
}
