package com.dod.UnrealZaruba.Utils;

public class Conversion {
    public static byte fromNormalizedFloatToByte(float value) {
        float clamped = Math.max(0, Math.min(1, value));
        int scaled = Math.round(clamped * 255f) - 128;
        return (byte) scaled;
    }


}
