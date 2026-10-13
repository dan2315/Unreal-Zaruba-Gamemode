package com.dod.unrealzaruba.Pricing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Runs the golden vectors the backend CI runs too (src/test/resources/vectors, copied from the backend repo, api/UnrealFrontiers.Api.Tests/Vectors).
 * A rule change edits the vectors on whichever side changes first; the other side fails here until its port follows.
 */
class GoldenVectorTests {
    @TestFactory
    Stream<DynamicTest> pricingVectors() throws IOException {
        JsonArray cases = read("/vectors/pricing-vectors.json");
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonElement element : cases) {
            JsonObject vector = element.getAsJsonObject();
            tests.add(DynamicTest.dynamicTest(vector.get("name").getAsString(), () -> runPricing(vector)));
        }
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> scannerVectors() throws IOException {
        JsonArray cases = read("/vectors/scanner-vectors.json");
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonElement element : cases) {
            JsonObject vector = element.getAsJsonObject();
            tests.add(DynamicTest.dynamicTest(vector.get("name").getAsString(), () -> runScanner(vector)));
        }
        return tests.stream();
    }

    private static void runPricing(JsonObject vector) {
        PriceCatalog catalog = catalog(vector.getAsJsonArray("catalog"));
        int[] maxSize = ints(vector.getAsJsonArray("maxSize"));
        JsonObject input = vector.getAsJsonObject("composition");
        Composition composition = new Composition();
        counts(input.getAsJsonObject("blocks"), composition.blocks);
        counts(input.getAsJsonObject("items"), composition.items);
        counts(input.getAsJsonObject("fluids"), composition.fluids);
        if (input.has("size") && !input.get("size").isJsonNull()) composition.size = ints(input.getAsJsonArray("size"));

        JsonObject expect = vector.getAsJsonObject("expect");
        Pricer.Breakdown price = Pricer.price(composition, catalog);
        assertMoney(expect.get("total"), price.total(), "total");
        assertMoney(expect.get("blocks"), price.blocks(), "blocks");
        assertMoney(expect.get("items"), price.items(), "items");
        assertMoney(expect.get("fluids"), price.fluids(), "fluids");
        assertEquals(expect.get("linesTruncated").getAsBoolean(), price.linesTruncated(), "linesTruncated");

        JsonArray lines = expect.getAsJsonArray("lines");
        assertEquals(lines.size(), price.lines().size(), "number of lines");
        for (int i = 0; i < lines.size(); i++) {
            JsonObject line = lines.get(i).getAsJsonObject();
            Pricer.Line actual = price.lines().get(i);
            assertEquals(line.get("kind").getAsString(), actual.kind(), "line " + i + " kind");
            assertEquals(line.get("id").getAsString(), actual.id(), "line " + i + " id");
            assertEquals(line.get("count").getAsLong(), actual.count(), "line " + i + " count");
            assertMoney(line.get("unitPrice"), actual.unitPrice(), "line " + i + " unitPrice");
            assertMoney(line.get("subtotal"), actual.subtotal(), "line " + i + " subtotal");
        }
        JsonArray unpriced = expect.getAsJsonArray("unpriced");
        assertEquals(unpriced.size(), price.unpriced().size(), "number of unpriced");
        for (int i = 0; i < unpriced.size(); i++) {
            JsonObject entry = unpriced.get(i).getAsJsonObject();
            Pricer.Unpriced actual = price.unpriced().get(i);
            assertEquals(entry.get("kind").getAsString(), actual.kind(), "unpriced " + i + " kind");
            assertEquals(entry.get("id").getAsString(), actual.id(), "unpriced " + i + " id");
            assertEquals(entry.get("count").getAsLong(), actual.count(), "unpriced " + i + " count");
        }

        Validator.Check check = Validator.check(composition, catalog, maxSize);
        assertEquals(strings(expect.getAsJsonArray("problems")), check.problems(), "problems");
        assertEquals(strings(expect.getAsJsonArray("forbiddenBlocks")), check.forbiddenBlocks(), "forbiddenBlocks");
        assertEquals(strings(expect.getAsJsonArray("forbiddenItems")), check.forbiddenItems(), "forbiddenItems");
        assertEquals(strings(expect.getAsJsonArray("forbiddenFluids")), check.forbiddenFluids(), "forbiddenFluids");
    }

    private static void runScanner(JsonObject vector) {
        Composition composition = new Composition();
        Tag tag = toNbt(vector.get("nbt"));
        assertTrue(tag instanceof CompoundTag, "the vector nbt is a compound");
        // The tag is a block entity tag: its own id is the block entity type, only its children are searched
        CompositionScanner.walkChildren((CompoundTag) tag, 1, composition);

        JsonObject expect = vector.getAsJsonObject("expect");
        assertEquals(expectedCounts(expect.getAsJsonObject("items")), new TreeMap<>(composition.items), "items");
        assertEquals(expectedCounts(expect.getAsJsonObject("fluids")), new TreeMap<>(composition.fluids), "fluids");
    }

    /** JSON to NBT the way the backend test does it: object = compound, array = list, integer = int (long if it does not fit), fraction = double. */
    private static Tag toNbt(JsonElement element) {
        if (element.isJsonObject()) {
            CompoundTag compound = new CompoundTag();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                compound.put(entry.getKey(), toNbt(entry.getValue()));
            }
            return compound;
        }
        if (element.isJsonArray()) {
            ListTag list = new ListTag();
            for (JsonElement child : element.getAsJsonArray()) list.add(toNbt(child));
            return list;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isString()) return StringTag.valueOf(primitive.getAsString());
        if (primitive.isBoolean()) return IntTag.valueOf(primitive.getAsBoolean() ? 1 : 0);
        String text = primitive.getAsString();
        if (text.contains(".") || text.contains("e") || text.contains("E")) return DoubleTag.valueOf(primitive.getAsDouble());
        long value = primitive.getAsLong();
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? IntTag.valueOf((int) value) : LongTag.valueOf(value);
    }

    private static PriceCatalog catalog(JsonArray entries) {
        List<PriceCatalog.Entry> list = new ArrayList<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            list.add(new PriceCatalog.Entry(entry.get("kind").getAsString(), entry.get("id").getAsString(),
                    new BigDecimal(entry.get("price").getAsString()), entry.get("forbidden").getAsBoolean()));
        }
        return new PriceCatalog(list);
    }

    private static void counts(JsonObject source, Map<String, Long> target) {
        if (source == null) return;
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) target.put(entry.getKey(), entry.getValue().getAsLong());
    }

    private static Map<String, Long> expectedCounts(JsonObject source) {
        Map<String, Long> counts = new TreeMap<>();
        counts(source, counts);
        return counts;
    }

    private static int[] ints(JsonArray array) {
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) values[i] = array.get(i).getAsInt();
        return values;
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        if (array != null) for (JsonElement element : array) values.add(element.getAsString());
        return values;
    }

    private static void assertMoney(JsonElement expected, BigDecimal actual, String what) {
        BigDecimal wanted = new BigDecimal(expected.getAsString());
        assertEquals(0, wanted.compareTo(actual), what + ": expected " + wanted + " but was " + actual);
    }

    private static JsonArray read(String resource) throws IOException {
        try (Reader reader = new InputStreamReader(GoldenVectorTests.class.getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }
}
