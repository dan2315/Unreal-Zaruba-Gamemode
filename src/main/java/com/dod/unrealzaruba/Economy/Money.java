package com.dod.unrealzaruba.Economy;

import java.util.Locale;

/** Shared by server messages and client UI, so it must not touch client-only classes. */
public final class Money {
    private Money() {}

    public static String format(int amount) {
        return "$" + String.format("%,d", amount);
    }

    /** Backend prices can be fractional: whole numbers without decimals, otherwise one or two decimals. */
    public static String format(double amount) {
        if (amount == Math.rint(amount)) return "$" + String.format(Locale.ROOT, "%,.0f", amount);
        String s = String.format(Locale.ROOT, "%,.2f", amount);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return "$" + s;
    }
}
