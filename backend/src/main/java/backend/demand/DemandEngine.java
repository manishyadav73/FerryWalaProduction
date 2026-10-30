package backend.demand;

import backend.model.Category;
import backend.model.SignalType;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Explainable demand scoring. Every number it produces can be traced to
 * a handful of rules below; there is no model and no hidden state.
 *
 * <p>What it does:
 * <ol>
 *   <li>Normalises product names (so "Tamatar" and "tomatoes" are one product).</li>
 *   <li>Counts at most one signal per customer + product + day (strongest wins).</li>
 *   <li>Adds <em>expected replenishment</em>: a customer whose last purchase of a
 *       product was roughly one re-purchase cycle ago is "due" again.</li>
 *   <li>Weights each signal by type and recency (exponential half-life).</li>
 *   <li>Aggregates by ~1 km grid cell, then maps summed weight to a 0-100 score
 *       with a saturating curve.</li>
 *   <li>Estimates interested customers as a <em>range</em> (people with strong
 *       signals ... plus people with only weak signals).</li>
 *   <li>Finds the busiest 3-hour window from when signals were created.</li>
 * </ol>
 *
 * <p>What it deliberately does not do: promise that anyone will buy. Output
 * is "estimated" / "likely" demand from app users only.
 */
public final class DemandEngine {

    public enum Level { HIGH, MEDIUM, LOW }

    public enum Confidence { HIGH, MEDIUM, LOW }

    public record Config(
            double halfLifeDays,
            int windowDays,
            double scoreScale,
            double minRawWeight,
            int maxAreas,
            double distanceSoftKm,
            ZoneId zone
    ) {
        public static Config defaults() {
            return new Config(7.0, 30, 10.0, 1.0, 5, 10.0, ZoneId.of("Asia/Kolkata"));
        }
    }

    public record VendorContext(Double latitude, Double longitude, Set<String> carriedProducts) {
        public static VendorContext none() {
            return new VendorContext(null, null, Set.of());
        }

        public boolean hasLocation() {
            return latitude != null && longitude != null;
        }
    }

    public record ProductDemand(String name, double weight, int signals, boolean vendorCarries) {
    }

    public record TimeWindow(int startHour, int endHour, String label, boolean fromData) {
    }

    public record Opportunity(
            String areaKey,
            String areaName,
            double latitude,
            double longitude,
            Double distanceKm,
            int score,
            Level level,
            int estimatedCustomersMin,
            int estimatedCustomersMax,
            List<ProductDemand> topProducts,
            TimeWindow recommendedWindow,
            int requestCount,
            int signalCount,
            int customerCount,
            Confidence confidence,
            List<String> reasons
    ) {
    }

    public record Result(List<Opportunity> ranked, int signalsConsidered) {
    }

    private static final double W_PURCHASE = 3.0;
    private static final double W_PRE_ORDER = 2.5;
    private static final double W_REQUEST = 2.5;
    private static final double W_SEARCH = 1.0;
    private static final double W_VIEW = 0.5;
    private static final double W_REPLENISH = 2.0;
    private static final double NOT_CARRIED_FACTOR = 0.3;
    private static final int WINDOW_HOURS = 3;
    private static final double MIN_TIMING_WEIGHT = 3.0;

    private final Config config;

    public DemandEngine(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public static double baseWeight(SignalType type) {
        return switch (type) {
            case PURCHASE -> W_PURCHASE;
            case PRE_ORDER -> W_PRE_ORDER;
            case REQUEST -> W_REQUEST;
            case SEARCH -> W_SEARCH;
            case VIEW -> W_VIEW;
            case REPLENISHMENT -> W_REPLENISH;
        };
    }

    /** Internal normalised, weighted signal. */
    private static final class Sig {
        final SignalType type;
        final String product;
        final Category category;
        final double lat;
        final double lng;
        final String areaKey;
        final String areaName;
        final Instant at;
        final Long customerId;
        final boolean derived;
        final double strength; // 1.0 for real signals, <1 for fuzzy replenishment
        double weight;

        Sig(SignalType type, String product, Category category, double lat, double lng, String areaKey,
            String areaName, Instant at, Long customerId, boolean derived, double strength) {
            this.type = type;
            this.product = product;
            this.category = category;
            this.lat = lat;
            this.lng = lng;
            this.areaKey = areaKey;
            this.areaName = areaName;
            this.at = at;
            this.customerId = customerId;
            this.derived = derived;
            this.strength = strength;
        }
    }

    private static final class Area {
        final String key;
        double raw;
        double latSum;
        double lngSum;
        double geoWeight;
        final Map<String, Integer> names = new HashMap<>();
        final Map<String, double[]> products = new HashMap<>(); // [weight, signals]
        final Set<Long> strongCustomers = new HashSet<>();
        final Set<Long> weakCustomers = new HashSet<>();
        final Set<Long> allCustomers = new HashSet<>();
        final double[] hourly = new double[24];
        int realSignals;
        int requestCount;
        int replenishmentCustomers;
        final Map<String, Set<Long>> replenishProducts = new HashMap<>();

        Area(String key) {
            this.key = key;
        }
    }

    public Result compute(List<SignalInput> inputs, VendorContext vendor, Instant now) {
        VendorContext ctx = vendor != null ? vendor : VendorContext.none();
        Instant windowStart = now.minus(Duration.ofDays(config.windowDays()));

        List<Sig> sigs = normalise(inputs, windowStart, now);
        sigs = dedupePerCustomerProductDay(sigs);
        List<Sig> all = new ArrayList<>(sigs);
        all.addAll(deriveReplenishment(sigs, now));

        boolean hasCatalog = !ctx.carriedProducts().isEmpty();
        for (Sig s : all) {
            double decay = s.derived ? 1.0 : decay(Duration.between(s.at, now));
            double relevance = (!hasCatalog || ctx.carriedProducts().contains(s.product)) ? 1.0 : NOT_CARRIED_FACTOR;
            s.weight = baseWeight(s.type) * s.strength * decay * relevance;
        }

        Map<String, Area> areas = new LinkedHashMap<>();
        for (Sig s : all) {
            Area a = areas.computeIfAbsent(s.areaKey, Area::new);
            accumulate(a, s, now);
        }

        List<Opportunity> built = new ArrayList<>();
        for (Area a : areas.values()) {
            if (a.raw < config.minRawWeight()) {
                continue;
            }
            built.add(toOpportunity(a, ctx));
        }

        boolean hasLoc = ctx.hasLocation();
        Comparator<Opportunity> byRank = Comparator
                .comparingDouble((Opportunity o) -> -rankValue(o, hasLoc))
                .thenComparing(Comparator.comparingInt(Opportunity::score).reversed())
                .thenComparing(Opportunity::areaKey);
        built.sort(byRank);

        List<Opportunity> top = built.size() > config.maxAreas()
                ? new ArrayList<>(built.subList(0, config.maxAreas()))
                : built;
        return new Result(List.copyOf(top), sigs.size());
    }

    // ---------------------------------------------------------------- steps

    private List<Sig> normalise(List<SignalInput> inputs, Instant windowStart, Instant now) {
        List<Sig> out = new ArrayList<>();
        if (inputs == null) {
            return out;
        }
        for (SignalInput in : inputs) {
            if (in == null || in.type() == null || in.at() == null) {
                continue;
            }
            if (in.type() == SignalType.REPLENISHMENT) {
                continue; // engine-derived only
            }
            if (in.at().isBefore(windowStart) || in.at().isAfter(now)) {
                continue;
            }
            String product = ProductNames.normalize(in.productName());
            if (product.isEmpty()) {
                continue;
            }
            String key = in.areaKey() != null && !in.areaKey().isBlank()
                    ? in.areaKey()
                    : GeoUtil.areaKey(in.latitude(), in.longitude());
            Category cat = in.category() != null ? in.category() : ProductNames.inferCategory(product);
            out.add(new Sig(in.type(), product, cat, in.latitude(), in.longitude(), key,
                    in.areaName(), in.at(), in.customerId(), false, 1.0));
        }
        return out;
    }

    /** One signal per customer + product + local day; the strongest type wins. */
    private List<Sig> dedupePerCustomerProductDay(List<Sig> sigs) {
        Map<String, Sig> best = new LinkedHashMap<>();
        int anonymous = 0;
        for (Sig s : sigs) {
            String key;
            if (s.customerId == null) {
                key = "anon#" + (anonymous++);
            } else {
                LocalDate day = s.at.atZone(config.zone()).toLocalDate();
                key = s.customerId + "|" + s.product + "|" + day;
            }
            Sig existing = best.get(key);
            if (existing == null || baseWeight(s.type) > baseWeight(existing.type)) {
                best.put(key, s);
            }
        }
        return new ArrayList<>(best.values());
    }

    /**
     * A customer is "due" for a product when their latest purchase of it was
     * about one re-purchase cycle ago (within +/- 1 day of today).
     */
    private List<Sig> deriveReplenishment(List<Sig> sigs, Instant now) {
        Map<String, Sig> latestPurchase = new HashMap<>();
        for (Sig s : sigs) {
            if (s.type != SignalType.PURCHASE || s.customerId == null) {
                continue;
            }
            String key = s.customerId + "|" + s.product;
            Sig prev = latestPurchase.get(key);
            if (prev == null || s.at.isAfter(prev.at)) {
                latestPurchase.put(key, s);
            }
        }

        Set<String> recentStrong = new HashSet<>();
        for (Sig s : sigs) {
            boolean strong = s.type == SignalType.REQUEST || s.type == SignalType.PRE_ORDER;
            if (strong && s.customerId != null
                    && Duration.between(s.at, now).compareTo(Duration.ofDays(1)) <= 0) {
                recentStrong.add(s.customerId + "|" + s.product);
            }
        }

        List<Sig> derived = new ArrayList<>();
        for (Map.Entry<String, Sig> e : latestPurchase.entrySet()) {
            Sig p = e.getValue();
            if (recentStrong.contains(e.getKey())) {
                continue; // already asking for it right now
            }
            double cycleDays = ProductNames.cycleDays(p.product, p.category);
            double expectedInDays = cycleDays - ageDays(p.at, now);
            double distance = Math.abs(expectedInDays);
            if (distance <= 1.0) {
                double strength = 1.0 - (distance / 1.5); // 1.0 .. 0.33
                derived.add(new Sig(SignalType.REPLENISHMENT, p.product, p.category, p.lat, p.lng,
                        p.areaKey, p.areaName, now, p.customerId, true, strength));
            }
        }
        return derived;
    }

    private void accumulate(Area a, Sig s, Instant now) {
        a.raw += s.weight;
        a.latSum += s.lat * s.weight;
        a.lngSum += s.lng * s.weight;
        a.geoWeight += s.weight;
        if (s.areaName != null && !s.areaName.isBlank()) {
            a.names.merge(s.areaName.trim(), 1, Integer::sum);
        }
        double[] pw = a.products.computeIfAbsent(s.product, k -> new double[2]);
        pw[0] += s.weight;
        pw[1] += 1;

        double age = ageDays(s.at, now);
        if (!s.derived) {
            a.realSignals++;
            int hour = s.at.atZone(config.zone()).getHour();
            a.hourly[hour] += s.weight;
        }
        if (s.type == SignalType.REQUEST || s.type == SignalType.PRE_ORDER) {
            if (age <= 7) {
                a.requestCount++;
            }
        }

        if (s.customerId != null) {
            a.allCustomers.add(s.customerId);
            boolean strong = switch (s.type) {
                case REQUEST, PRE_ORDER -> age <= 7;
                case PURCHASE -> age <= 14;
                case REPLENISHMENT -> true;
                default -> false;
            };
            boolean weak = (s.type == SignalType.SEARCH || s.type == SignalType.VIEW) && age <= 7;
            if (strong) {
                a.strongCustomers.add(s.customerId);
            } else if (weak) {
                a.weakCustomers.add(s.customerId);
            }
            if (s.derived) {
                a.replenishProducts.computeIfAbsent(s.product, k -> new HashSet<>()).add(s.customerId);
            }
        }
    }

    private static double[] cellCentre(String key, double lat, double lng) {
        try {
            String[] p = key.split(",");
            return new double[]{Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim())};
        } catch (RuntimeException e) {
            return new double[]{Math.round(lat * 100.0) / 100.0, Math.round(lng * 100.0) / 100.0};
        }
    }

    private Opportunity toOpportunity(Area a, VendorContext ctx) {
        double exactLat = a.geoWeight > 0 ? a.latSum / a.geoWeight : 0;
        double exactLng = a.geoWeight > 0 ? a.lngSum / a.geoWeight : 0;
        // Privacy: vendors get the centre of the ~1 km grid cell, never the point a customer chose
        // (with one customer in a cell the weighted average would be that household's exact spot).
        double[] centre = cellCentre(a.key, exactLat, exactLng);
        double lat = centre[0];
        double lng = centre[1];

        int score = (int) Math.round(100.0 * (1.0 - Math.exp(-a.raw / config.scoreScale())));
        score = Math.max(0, Math.min(100, score));
        Level level = score >= 65 ? Level.HIGH : score >= 35 ? Level.MEDIUM : Level.LOW;

        Set<Long> weakOnly = new HashSet<>(a.weakCustomers);
        weakOnly.removeAll(a.strongCustomers);
        int estMin = a.strongCustomers.size();
        int estMax = estMin + weakOnly.size();

        String name = a.names.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(String.format(Locale.ROOT, "Area near %.3f, %.3f", lat, lng));

        List<Map.Entry<String, double[]>> prods = new ArrayList<>(a.products.entrySet());
        prods.sort((x, y) -> {
            int c = Double.compare(y.getValue()[0], x.getValue()[0]);
            return c != 0 ? c : x.getKey().compareTo(y.getKey());
        });
        List<ProductDemand> top = new ArrayList<>();
        for (int i = 0; i < Math.min(5, prods.size()); i++) {
            Map.Entry<String, double[]> e = prods.get(i);
            top.add(new ProductDemand(ProductNames.displayName(e.getKey()), round2(e.getValue()[0]),
                    (int) e.getValue()[1], ctx.carriedProducts().contains(e.getKey())));
        }

        Double distance = ctx.hasLocation()
                ? round1(GeoUtil.haversineKm(ctx.latitude(), ctx.longitude(), lat, lng))
                : null;

        int customers = a.allCustomers.size();
        Confidence confidence = (customers >= 5 && a.realSignals >= 10) ? Confidence.HIGH
                : (customers >= 2 && a.realSignals >= 4) ? Confidence.MEDIUM
                : Confidence.LOW;

        TimeWindow window = bestWindow(a.hourly);

        List<String> reasons = reasons(a, top, ctx, distance, customers);

        return new Opportunity(a.key, name, round5(lat), round5(lng), distance, score, level, estMin, estMax,
                List.copyOf(top), window, a.requestCount, a.realSignals, customers, confidence,
                List.copyOf(reasons));
    }

    private List<String> reasons(Area a, List<ProductDemand> top, VendorContext ctx, Double distance, int customers) {
        List<String> r = new ArrayList<>();
        r.add(a.realSignals + (a.realSignals == 1 ? " demand signal" : " demand signals")
                + " from " + customers + (customers == 1 ? " customer" : " customers")
                + " in the last " + config.windowDays() + " days");
        if (a.requestCount > 0) {
            r.add(a.requestCount + (a.requestCount == 1 ? " product request" : " product requests")
                    + " or pre-orders this week");
        }
        if (!a.replenishProducts.isEmpty()) {
            String best = null;
            int bestCount = 0;
            for (Map.Entry<String, Set<Long>> e : a.replenishProducts.entrySet()) {
                if (e.getValue().size() > bestCount
                        || (e.getValue().size() == bestCount && best != null && e.getKey().compareTo(best) < 0)) {
                    best = e.getKey();
                    bestCount = e.getValue().size();
                }
            }
            r.add(bestCount + (bestCount == 1 ? " customer is" : " customers are")
                    + " likely due to re-buy " + ProductNames.displayName(best)
                    + " (based on past purchase cycles)");
        }
        if (!ctx.carriedProducts().isEmpty() && !top.isEmpty()) {
            long carried = top.stream().filter(ProductDemand::vendorCarries).count();
            r.add("You carry " + carried + " of the top " + top.size() + " requested products");
            for (ProductDemand p : top) {
                if (!p.vendorCarries()) {
                    r.add(p.name() + " is in demand here but not on your product list");
                    break;
                }
            }
        }
        if (distance != null) {
            r.add(distance + " km from your current location");
        }
        return r;
    }

    private TimeWindow bestWindow(double[] hourly) {
        double total = 0;
        for (double h : hourly) {
            total += h;
        }
        if (total >= MIN_TIMING_WEIGHT) {
            int bestStart = -1;
            double bestSum = -1;
            // Only consider realistic vending hours: 6 AM to 10 PM.
            for (int start = 6; start + WINDOW_HOURS <= 22; start++) {
                double sum = 0;
                for (int h = start; h < start + WINDOW_HOURS; h++) {
                    sum += hourly[h];
                }
                if (sum > bestSum) {
                    bestSum = sum;
                    bestStart = start;
                }
            }
            if (bestStart >= 0 && bestSum > 0) {
                return new TimeWindow(bestStart, bestStart + WINDOW_HOURS,
                        label(bestStart, bestStart + WINDOW_HOURS), true);
            }
        }
        return new TimeWindow(17, 20, label(17, 20), false);
    }

    static String label(int startHour, int endHour) {
        return hour12(startHour) + " – " + hour12(endHour);
    }

    private static String hour12(int hour24) {
        int h = hour24 % 24;
        String suffix = h >= 12 ? "PM" : "AM";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + " " + suffix;
    }

    private double rankValue(Opportunity o, boolean hasLocation) {
        if (!hasLocation || o.distanceKm() == null) {
            return o.score();
        }
        return o.score() / (1.0 + o.distanceKm() / config.distanceSoftKm());
    }

    private double decay(Duration age) {
        double days = Math.max(0, age.toMinutes() / 1440.0);
        return Math.pow(0.5, days / config.halfLifeDays());
    }

    private static double ageDays(Instant at, Instant now) {
        return Math.max(0, Duration.between(at, now).toMinutes() / 1440.0);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round5(double v) {
        return Math.round(v * 100000.0) / 100000.0;
    }
}
