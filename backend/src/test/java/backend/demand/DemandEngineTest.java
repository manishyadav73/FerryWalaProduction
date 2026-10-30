package backend.demand;

import backend.demand.DemandEngine.Confidence;
import backend.demand.DemandEngine.Level;
import backend.demand.DemandEngine.Opportunity;
import backend.demand.DemandEngine.Result;
import backend.demand.DemandEngine.VendorContext;
import backend.model.Category;
import backend.model.SignalType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandEngineTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    // Fixed "now": 2026-10-05 12:00 IST
    private static final Instant NOW = LocalDate.of(2026, 10, 5).atTime(12, 0).atZone(IST).toInstant();

    private final DemandEngine engine = new DemandEngine(DemandEngine.Config.defaults());

    private static final double GP_LAT = 13.0500;   // "Green Park"
    private static final double GP_LNG = 80.2500;
    private static final double FAR_LAT = 13.1500;  // ~11 km away
    private static final double FAR_LNG = 80.2500;

    private static SignalInput sig(SignalType t, String product, double lat, double lng, String area,
                                   Instant at, Long customer) {
        return new SignalInput(t, product, null, lat, lng, GeoUtil.areaKey(lat, lng), area, at, customer);
    }

    private static Instant ago(Duration d) {
        return NOW.minus(d);
    }

    private static Instant todayAt(int hour) {
        return LocalDate.of(2026, 10, 5).atTime(hour, 0).atZone(IST).toInstant();
    }

    @Test
    void noSignalsGivesNoOpportunities() {
        Result r = engine.compute(List.of(), VendorContext.none(), NOW);
        assertTrue(r.ranked().isEmpty());
        assertEquals(0, r.signalsConsidered());
    }

    @Test
    void busierAreaRanksAboveSparseArea() {
        List<SignalInput> in = new ArrayList<>();
        for (long c = 1; c <= 6; c++) {
            in.add(sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "Green Park", ago(Duration.ofHours(c)), c));
        }
        in.add(sig(SignalType.REQUEST, "Onion", 13.0900, 80.2900, "Quiet Lane", ago(Duration.ofHours(2)), 99L));

        Result r = engine.compute(in, VendorContext.none(), NOW);

        assertEquals(2, r.ranked().size());
        Opportunity best = r.ranked().get(0);
        assertEquals("Green Park", best.areaName());
        assertTrue(best.score() > r.ranked().get(1).score());
        assertEquals(6, best.customerCount());
        assertEquals(6, best.estimatedCustomersMin());
        assertEquals("Tomato", best.topProducts().get(0).name());
    }

    @Test
    void hinglishAndPluralNamesAreMergedIntoOneProduct() {
        List<SignalInput> in = List.of(
                sig(SignalType.REQUEST, "Tamatar", GP_LAT, GP_LNG, "Green Park", ago(Duration.ofHours(1)), 1L),
                sig(SignalType.REQUEST, "tomatoes", GP_LAT, GP_LNG, "Green Park", ago(Duration.ofHours(2)), 2L),
                sig(SignalType.REQUEST, " Tomato ", GP_LAT, GP_LNG, "Green Park", ago(Duration.ofHours(3)), 3L));

        Opportunity o = engine.compute(in, VendorContext.none(), NOW).ranked().get(0);

        assertEquals(1, o.topProducts().size());
        assertEquals("Tomato", o.topProducts().get(0).name());
        assertEquals(3, o.topProducts().get(0).signals());
    }

    @Test
    void freshSignalsOutweighOldOnes() {
        List<SignalInput> fresh = List.of(
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 1L),
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(2)), 2L),
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(3)), 3L));
        List<SignalInput> old = List.of(
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofDays(21)), 1L),
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofDays(22)), 2L),
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofDays(23)), 3L));

        int freshScore = engine.compute(fresh, VendorContext.none(), NOW).ranked().get(0).score();
        List<Opportunity> oldResult = engine.compute(old, VendorContext.none(), NOW).ranked();

        assertTrue(oldResult.isEmpty() || oldResult.get(0).score() < freshScore);
    }

    @Test
    void signalsOutsideTheWindowAreIgnored() {
        List<SignalInput> in = List.of(
                sig(SignalType.REQUEST, "Potato", GP_LAT, GP_LNG, "A", ago(Duration.ofDays(45)), 1L));
        Result r = engine.compute(in, VendorContext.none(), NOW);
        assertTrue(r.ranked().isEmpty());
        assertEquals(0, r.signalsConsidered());
    }

    @Test
    void sameCustomerSameProductSameDayCountsOnce() {
        List<SignalInput> spam = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spam.add(sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofMinutes(10 + i)), 1L));
        }
        List<SignalInput> single = List.of(
                sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofMinutes(10)), 1L));

        Opportunity s = engine.compute(spam, VendorContext.none(), NOW).ranked().get(0);
        Opportunity one = engine.compute(single, VendorContext.none(), NOW).ranked().get(0);

        assertEquals(one.score(), s.score());
        assertEquals(1, s.signalCount());
    }

    @Test
    void strongestSignalWinsWithinTheSameDedupeKey() {
        List<SignalInput> in = List.of(
                sig(SignalType.VIEW, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(2)), 1L),
                sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 1L));
        Opportunity o = engine.compute(in, VendorContext.none(), NOW).ranked().get(0);
        assertEquals(1, o.signalCount());
        assertEquals(1, o.estimatedCustomersMin()); // REQUEST is a strong signal
    }

    @Test
    void customerDueToRebuyAddsExpectedDemand() {
        // Vegetables re-buy cycle is 3 days; last bought 3 days ago -> due today.
        SignalInput bought = new SignalInput(SignalType.PURCHASE, "Tomato", Category.VEGETABLES, GP_LAT, GP_LNG,
                GeoUtil.areaKey(GP_LAT, GP_LNG), "Green Park", ago(Duration.ofDays(3)), 7L);

        Opportunity due = engine.compute(List.of(bought), VendorContext.none(), NOW).ranked().get(0);

        assertTrue(due.reasons().stream().anyMatch(s -> s.contains("likely due to re-buy Tomato")),
                "expected a replenishment reason, got " + due.reasons());

        // Same purchase 12 days ago is NOT due (cycle passed, signal also decayed).
        SignalInput stale = new SignalInput(SignalType.PURCHASE, "Tomato", Category.VEGETABLES, GP_LAT, GP_LNG,
                GeoUtil.areaKey(GP_LAT, GP_LNG), "Green Park", ago(Duration.ofDays(12)), 7L);
        List<Opportunity> notDue = engine.compute(List.of(stale), VendorContext.none(), NOW).ranked();
        assertTrue(notDue.isEmpty()
                || notDue.get(0).reasons().stream().noneMatch(s -> s.contains("due to re-buy")));
    }

    @Test
    void recentExplicitRequestSuppressesDuplicateReplenishment() {
        SignalInput bought = new SignalInput(SignalType.PURCHASE, "Tomato", Category.VEGETABLES, GP_LAT, GP_LNG,
                GeoUtil.areaKey(GP_LAT, GP_LNG), "A", ago(Duration.ofDays(3)), 7L);
        SignalInput asked = sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(2)), 7L);

        Opportunity o = engine.compute(List.of(bought, asked), VendorContext.none(), NOW).ranked().get(0);

        assertTrue(o.reasons().stream().noneMatch(s -> s.contains("due to re-buy")));
    }

    @Test
    void productsTheVendorDoesNotCarryCountForLess() {
        List<SignalInput> in = new ArrayList<>();
        for (long c = 1; c <= 4; c++) {
            in.add(sig(SignalType.REQUEST, "Spinach", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(c)), c));
        }
        VendorContext carriesTomato = new VendorContext(null, null, Set.of("tomato"));

        int unfiltered = engine.compute(in, VendorContext.none(), NOW).ranked().get(0).score();
        Opportunity filtered = engine.compute(in, carriesTomato, NOW).ranked().get(0);

        assertTrue(filtered.score() < unfiltered);
        assertTrue(filtered.reasons().stream().anyMatch(s -> s.contains("not on your product list")));
        assertFalse(filtered.topProducts().get(0).vendorCarries());
    }

    @Test
    void recommendedWindowFollowsWhenSignalsHappen() {
        List<SignalInput> in = new ArrayList<>();
        long c = 1;
        for (int day = 1; day <= 3; day++) {
            for (int hour : new int[]{18, 19, 19}) {
                Instant at = todayAt(hour).minus(Duration.ofDays(day));
                in.add(sig(SignalType.REQUEST, "Onion", GP_LAT, GP_LNG, "A", at, c++));
            }
        }
        Opportunity o = engine.compute(in, VendorContext.none(), NOW).ranked().get(0);

        assertTrue(o.recommendedWindow().fromData());
        assertTrue(o.recommendedWindow().startHour() >= 17 && o.recommendedWindow().startHour() <= 18,
                "window starts at " + o.recommendedWindow().startHour());
    }

    @Test
    void sparseTimingFallsBackToEveningDefaultAndSaysSo() {
        List<SignalInput> in = List.of(
                sig(SignalType.VIEW, "Onion", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 1L),
                sig(SignalType.REQUEST, "Onion", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(2)), 2L));
        Opportunity o = engine.compute(in, VendorContext.none(), NOW).ranked().get(0);
        assertFalse(o.recommendedWindow().fromData());
        assertEquals(17, o.recommendedWindow().startHour());
        assertEquals(20, o.recommendedWindow().endHour());
    }

    @Test
    void distanceBreaksNearTies() {
        List<SignalInput> in = new ArrayList<>();
        for (long c = 1; c <= 4; c++) {
            in.add(sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "Near", ago(Duration.ofHours(c)), c));
            in.add(sig(SignalType.REQUEST, "Tomato", FAR_LAT, FAR_LNG, "Far", ago(Duration.ofHours(c)), 100 + c));
        }
        VendorContext vendor = new VendorContext(GP_LAT, GP_LNG, Set.of());

        Result r = engine.compute(in, vendor, NOW);

        assertEquals("Near", r.ranked().get(0).areaName());
        assertNotNull(r.ranked().get(0).distanceKm());
        assertTrue(r.ranked().get(1).distanceKm() > 10.0);
    }

    @Test
    void estimateIsARangeBuiltFromStrongThenWeakCustomers() {
        List<SignalInput> in = List.of(
                sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 1L),
                sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 2L),
                sig(SignalType.SEARCH, "Onion", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 3L),
                sig(SignalType.VIEW, "Onion", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 4L));
        Opportunity o = engine.compute(in, VendorContext.none(), NOW).ranked().get(0);
        assertEquals(2, o.estimatedCustomersMin());
        assertEquals(4, o.estimatedCustomersMax());
    }

    @Test
    void confidenceIsHonestAboutThinData() {
        Opportunity thin = engine.compute(List.of(
                sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(1)), 1L)),
                VendorContext.none(), NOW).ranked().get(0);
        assertEquals(Confidence.LOW, thin.confidence());

        List<SignalInput> rich = new ArrayList<>();
        for (long c = 1; c <= 8; c++) {
            rich.add(sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(c)), c));
            rich.add(sig(SignalType.SEARCH, "Onion", GP_LAT, GP_LNG, "A", ago(Duration.ofHours(c)), c));
        }
        Opportunity strong = engine.compute(rich, VendorContext.none(), NOW).ranked().get(0);
        assertEquals(Confidence.HIGH, strong.confidence());
        assertEquals(Level.HIGH, strong.level());
        assertTrue(strong.score() <= 100);
    }

    @Test
    void scoreNeverExceedsOneHundredEvenWithHugeVolume() {
        List<SignalInput> in = new ArrayList<>();
        for (long c = 1; c <= 500; c++) {
            in.add(sig(SignalType.REQUEST, "Tomato", GP_LAT, GP_LNG, "A", ago(Duration.ofMinutes(c)), c));
        }
        Opportunity o = engine.compute(in, VendorContext.none(), NOW).ranked().get(0);
        assertTrue(o.score() <= 100 && o.score() >= 95);
    }

    @Test
    void limitsResultsToMaxAreas() {
        List<SignalInput> in = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            double lat = 13.00 + i * 0.05;
            for (long c = 1; c <= 2; c++) {
                in.add(sig(SignalType.REQUEST, "Tomato", lat, 80.25, "Area" + i, ago(Duration.ofHours(c)), i * 10L + c));
            }
        }
        assertEquals(5, engine.compute(in, VendorContext.none(), NOW).ranked().size());
    }

    @Test
    void productNameNormalisation() {
        assertEquals("tomato", ProductNames.normalize("Tomatoes"));
        assertEquals("potato", ProductNames.normalize("ALOO"));
        assertEquals("onion", ProductNames.normalize("  pyaz!! "));
        assertEquals("apple", ProductNames.normalize("apples"));
        assertEquals("", ProductNames.normalize("  ?? "));
        assertEquals("Tomato", ProductNames.displayName("tomato"));
    }

    @Test
    void nativeScriptProductNamesMapToTheSameProduct() {
        assertEquals("tomato", ProductNames.normalize("टमाटर"));
        assertEquals("potato", ProductNames.normalize(" आलू "));
        assertEquals("onion", ProductNames.normalize("வெங்காயம்"));
        assertEquals("tomato", ProductNames.normalize("தக்காளி"));
        assertEquals("banana", ProductNames.normalize("केला"));
        assertEquals(backend.model.Category.VEGETABLES, ProductNames.inferCategory(ProductNames.normalize("टमाटर")));
        assertEquals(backend.model.Category.FRUITS, ProductNames.inferCategory(ProductNames.normalize("வாழைப்பழம்")));
    }

    @Test
    void vendorsNeverSeeTheExactPointACustomerChose() {
        List<SignalInput> in = new ArrayList<>();
        for (long c = 1; c <= 4; c++) {
            in.add(sig(SignalType.REQUEST, "Tomato", 13.0537, 80.2561, "Green Park", ago(Duration.ofHours(c)), c));
        }

        Result r = engine.compute(in, VendorContext.none(), NOW);

        Opportunity o = r.ranked().get(0);
        // snapped to the centre of the ~1 km grid cell, not 13.0537 / 80.2561
        assertEquals(13.05, o.latitude(), 1e-9);
        assertEquals(80.26, o.longitude(), 1e-9);
    }
}
