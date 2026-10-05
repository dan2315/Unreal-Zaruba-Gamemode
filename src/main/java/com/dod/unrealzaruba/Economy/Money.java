package com.dod.unrealzaruba.Economy;

/** Shared by server messages and client UI, so it must not touch client-only classes. */
public final class Money {
    private Money() {}

    public static String format(int amount) {
        return "$" + String.format("%,d", amount);
    }
}
