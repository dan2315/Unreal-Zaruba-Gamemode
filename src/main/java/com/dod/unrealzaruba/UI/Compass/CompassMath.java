package com.dod.unrealzaruba.UI.Compass;

/**
 * Pure compass math (no Minecraft classes), so it can be unit-tested.
 * Bearings are compass degrees: 0 = North (-Z), 90 = East (+X), 180 = South (+Z), 270 = West (-X).
 */
public final class CompassMath {
    private CompassMath() {}

    /** Visible half-range of the strip, in degrees. Markers outside are clamped to the edges. */
    public static final double HALF_FOV = 60.0;

    public static double normalize360(double deg) {
        double r = deg % 360.0;
        if (r < 0) r += 360.0;
        // Avoid 359.9999999 vs 0 flicker at North.
        if (r >= 359.9995) r = 0;
        return r;
    }

    /** Wraps to [-180, 180). */
    public static double wrap180(double deg) {
        double r = (deg + 180.0) % 360.0;
        if (r < 0) r += 360.0;
        return r - 180.0;
    }

    /** Minecraft yaw (0 = South, 90 = West) to compass bearing (0 = North, 90 = East). */
    public static double yawToBearing(float yaw) {
        return normalize360(yaw + 180.0);
    }

    /** Compass bearing from (px, pz) to (tx, tz). */
    public static double bearingTo(double px, double pz, double tx, double tz) {
        return normalize360(Math.toDegrees(Math.atan2(tx - px, -(tz - pz))));
    }

    /** Signed turn from center to target, in [-180, 180). Positive = turn right (eastward). */
    public static double relative(double targetBearing, double centerBearing) {
        return wrap180(targetBearing - centerBearing);
    }

    /** 8-wind cardinal for a bearing: N, NE, E, SE, S, SW, W, NW. */
    public static String cardinalFor(double bearing) {
        int i = (int) Math.floor((normalize360(bearing) + 22.5) / 45.0) % 8;
        return switch (i) {
            case 0 -> "N";
            case 1 -> "NE";
            case 2 -> "E";
            case 3 -> "SE";
            case 4 -> "S";
            case 5 -> "SW";
            case 6 -> "W";
            default -> "NW";
        };
    }

    public static boolean isCardinal(double bearing) {
        double n = normalize360(bearing);
        return n % 90.0 < 0.001 || n % 90.0 > 89.999;
    }

    public static boolean isIntercardinal(double bearing) {
        double n = normalize360(bearing);
        double m = n % 90.0;
        return Math.abs(m - 45.0) < 0.001;
    }

    /** Degree label for the strip: cardinal/intercardinal letters, numbers every 15 degrees, else null (tick only). */
    public static String labelFor(double bearing) {
        double n = normalize360(bearing);
        long r = Math.round(n);
        if (r == 0 || r == 360) return "N";
        if (r == 90) return "E";
        if (r == 180) return "S";
        if (r == 270) return "W";
        if (r == 45) return "NE";
        if (r == 135) return "SE";
        if (r == 225) return "SW";
        if (r == 315) return "NW";
        if (r % 15 == 0) return String.valueOf(r);
        return null;
    }

    /** Pixel x for a bearing on a strip centered at centerX. Assumes |relative| <= halfFov. */
    public static double xForBearing(double bearing, double centerBearing, double centerX, double halfWidth, double halfFov) {
        return centerX + relative(bearing, centerBearing) / halfFov * halfWidth;
    }
}
