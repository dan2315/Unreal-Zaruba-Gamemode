package com.dod.unrealzaruba.Pricing;

import java.util.List;

/**
 * What the contraption scanner shows the player: size, price and what is wrong. Built on the server, sent to the client.
 * {@code rulesMissing} means the backend rules could not be fetched, so there is no price or verdict.
 */
public record ContraptionReport(String name, int[] size, int[] maxSize, double total, double blocks, double items,
                                double fluids, List<Line> lines, int unpricedCount, List<String> problems,
                                boolean allowed, boolean rulesMissing) {
    /** kind is 0 block, 1 item, 2 fluid; count is millibuckets for fluids. */
    public record Line(int kind, String id, long count, double subtotal) {}
}
