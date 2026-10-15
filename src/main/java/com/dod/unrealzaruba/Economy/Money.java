package com.dod.unrealzaruba.Economy;

import java.util.Locale;

/** Shared by server messages and client UI, so it must not touch client-only classes. */
public final class Money {
    private Money() {}

    public static String format(int amount) {
        return "$" + String.format("%,d", amount);
    }

    /** What a backend price costs in whole dollars: rounded up, never negative or beyond an int. */
    public static int charge(java.math.BigDecimal price) {
        java.math.BigDecimal whole = price.setScale(0, java.math.RoundingMode.CEILING);
        if (whole.signum() < 0) return 0;
        return whole.compareTo(java.math.BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : whole.intValue();
    }

    public static int charge(double price) {
        return charge(java.math.BigDecimal.valueOf(price));
    }

    /** Backend prices can be fractional: whole numbers without decimals, otherwise one or two decimals. */
    public static String format(double amount) {
        if (amount == Math.rint(amount)) return "$" + String.format(Locale.ROOT, "%,.0f", amount);
        String s = String.format(Locale.ROOT, "%,.2f", amount);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return "$" + s;
    }
}
