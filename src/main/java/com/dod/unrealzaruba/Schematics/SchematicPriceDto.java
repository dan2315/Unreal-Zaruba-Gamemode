package com.dod.unrealzaruba.Schematics;

import java.util.ArrayList;
import java.util.List;

/** Price breakdown from GET /schematics/{id}/price. Filled by Gson. */
public class SchematicPriceDto {
    public double total;
    public double blocks;
    public double items;
    public double fluids;
    public String pricedAt;
    /** Costliest first, at most 200. */
    public List<Line> lines = new ArrayList<>();
    public boolean linesTruncated;
    /** Things with no price in the price list; counted as 0. */
    public List<Unpriced> unpriced = new ArrayList<>();

    /** kind is "block", "item" or "fluid"; fluid counts are millibuckets and unitPrice is per bucket. */
    public static class Line {
        public String kind = "";
        public String id = "";
        public long count;
        public double unitPrice;
        public double subtotal;
    }

    public static class Unpriced {
        public String kind = "";
        public String id = "";
        public long count;
    }
}
