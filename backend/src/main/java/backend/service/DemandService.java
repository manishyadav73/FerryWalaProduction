package backend.service;

import backend.demand.DemandEngine;
import backend.demand.DemandEngine.Opportunity;
import backend.demand.DemandEngine.Result;
import backend.demand.DemandEngine.VendorContext;
import backend.demand.GeoUtil;
import backend.demand.ProductNames;
import backend.demand.SignalInput;
import backend.dto.DemandDtos.DataQuality;
import backend.dto.DemandDtos.OpportunityResponse;
import backend.dto.DemandDtos.SignalRequest;
import backend.error.ApiException;
import backend.model.Category;
import backend.model.CustomerOrder;
import backend.model.DemandSignal;
import backend.model.OrderItem;
import backend.model.OrderStatus;
import backend.model.Product;
import backend.model.SignalType;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.DemandSignalRepository;
import backend.repository.OrderRepository;
import backend.repository.ProductRepository;
import backend.repository.UserRepository;
import backend.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DemandService {

    public static final String DISCLAIMER =
            "Estimates from FerryWala customer activity, not guarantees. Real demand can differ.";

    private static final int WINDOW_DAYS = 30;
    private static final int MAX_SIGNALS_PER_DAY = 200;
    private static final double DEFAULT_RADIUS_KM = 12.0;

    private final DemandSignalRepository signals;
    private final VendorRepository vendors;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;
    private final DemandEngine engine;
    private final DemandEngine adminEngine;

    public DemandService(DemandSignalRepository signals, VendorRepository vendors, ProductRepository products,
                         OrderRepository orders, UserRepository users,
                         @Value("${app.demand.zone:Asia/Kolkata}") String zone) {
        this.signals = signals;
        this.vendors = vendors;
        this.products = products;
        this.orders = orders;
        this.users = users;
        ZoneId zoneId = ZoneId.of(zone);
        DemandEngine.Config base = DemandEngine.Config.defaults();
        this.engine = new DemandEngine(new DemandEngine.Config(base.halfLifeDays(), WINDOW_DAYS,
                base.scoreScale(), base.minRawWeight(), base.maxAreas(), base.distanceSoftKm(), zoneId));
        this.adminEngine = new DemandEngine(new DemandEngine.Config(base.halfLifeDays(), WINDOW_DAYS,
                base.scoreScale(), base.minRawWeight(), 20, base.distanceSoftKm(), zoneId));
    }

    /** A customer says "I need X around here" (or searched / viewed it). */
    @Transactional
    public void recordSignal(Long customerId, SignalRequest req) {
        if (req.type() != SignalType.REQUEST && req.type() != SignalType.SEARCH && req.type() != SignalType.VIEW) {
            throw ApiException.badRequest("INVALID_SIGNAL", "That kind of signal can't be sent directly.");
        }
        if (!GeoUtil.validLatLng(req.latitude(), req.longitude())) {
            throw ApiException.badRequest("INVALID_LOCATION", "That location doesn't look right.");
        }
        String product = ProductNames.normalize(req.productName());
        if (product.length() < 2) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Tell us which product you need.");
        }
        Instant now = Instant.now();
        if (signals.countByCustomerIdAndCreatedAtAfter(customerId, now.minus(Duration.ofDays(1)))
                >= MAX_SIGNALS_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "That's a lot of requests for today. Please try again tomorrow.");
        }
        // Idempotent: the same customer asking for the same thing within an hour is one signal.
        if (signals.existsByCustomerIdAndProductNameAndTypeAndCreatedAtAfter(
                customerId, product, req.type(), now.minus(Duration.ofHours(1)))) {
            return;
        }
        save(users.getReferenceById(customerId), req.type(), product, ProductNames.inferCategory(product),
                req.latitude(), req.longitude(), clean(req.areaName()), now);
    }

    /** Called by OrderService when an order is placed (PRE_ORDER) or completed (PURCHASE). */
    @Transactional
    public void recordFromOrder(User customer, CustomerOrder order, SignalType type) {
        Instant now = Instant.now();
        for (OrderItem item : order.getItems()) {
            String product = ProductNames.normalize(item.getProductName());
            if (product.isEmpty()) {
                continue;
            }
            save(customer, type, product, item.getCategory(), order.getLatitude(), order.getLongitude(),
                    order.getAreaName(), now);
        }
    }

    private void save(User customer, SignalType type, String product, Category category, double lat, double lng,
                      String areaName, Instant at) {
        DemandSignal s = new DemandSignal();
        s.setCustomer(customer);
        s.setType(type);
        s.setProductName(product);
        s.setCategory(category);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setAreaKey(GeoUtil.areaKey(lat, lng));
        s.setAreaName(areaName);
        s.setCreatedAt(at);
        signals.save(s);
    }

    /** "Where should I go today?" for the signed-in vendor. */
    @Transactional(readOnly = true)
    public OpportunityResponse opportunities(Long vendorUserId, Double lat, Double lng, Double radiusKm) {
        Vendor vendor = vendors.findByUserId(vendorUserId)
                .orElseThrow(() -> ApiException.forbidden("No vendor profile found for this account."));

        Double refLat = lat;
        Double refLng = lng;
        if (!GeoUtil.validLatLng(refLat, refLng)) {
            refLat = vendor.getLatitude();
            refLng = vendor.getLongitude();
        }
        if (!GeoUtil.validLatLng(refLat, refLng)) {
            throw ApiException.conflict("LOCATION_REQUIRED",
                    "Share your location so we can look for demand around you.");
        }
        double radius = radiusKm == null ? DEFAULT_RADIUS_KM : Math.max(2.0, Math.min(25.0, radiusKm));
        double[] box = GeoUtil.boundingBox(refLat, refLng, radius);
        Instant now = Instant.now();
        Instant since = now.minus(Duration.ofDays(WINDOW_DAYS));

        List<SignalInput> inputs = signals
                .findInBox(since, box[0], box[1], box[2], box[3], PageRequest.of(0, 5000)).stream()
                .map(DemandService::toInput)
                .toList();

        Set<String> carried = new HashSet<>();
        for (Product p : products.findByVendorIdAndAvailableTrueOrderByNameAsc(vendor.getId())) {
            String n = ProductNames.normalize(p.getName());
            if (!n.isEmpty()) {
                carried.add(n);
            }
        }

        Result result = engine.compute(inputs, new VendorContext(refLat, refLng, carried), now);
        List<Opportunity> ranked = result.ranked();
        Opportunity best = ranked.isEmpty() ? null : ranked.get(0);
        List<Opportunity> others = ranked.size() > 1 ? ranked.subList(1, ranked.size()) : List.of();

        return new OpportunityResponse(now, DISCLAIMER, refLat, refLng, vendor.isOnline(),
                orders.countByVendorIdAndStatus(vendor.getId(), OrderStatus.PENDING),
                quality(result.signalsConsidered()), best, others);
    }

    /** Admin view: strongest demand areas across the whole platform. */
    @Transactional(readOnly = true)
    public List<Opportunity> platformAreas() {
        Instant now = Instant.now();
        List<SignalInput> inputs = signals
                .findSince(now.minus(Duration.ofDays(WINDOW_DAYS)), PageRequest.of(0, 20000)).stream()
                .map(DemandService::toInput)
                .toList();
        return adminEngine.compute(inputs, VendorContext.none(), now).ranked();
    }

    private static DataQuality quality(int considered) {
        if (considered < 5) {
            return new DataQuality(considered, WINDOW_DAYS, "LOW",
                    "Early estimates: not much customer activity near you yet.");
        }
        if (considered < 30) {
            return new DataQuality(considered, WINDOW_DAYS, "MEDIUM", "Based on a modest amount of activity.");
        }
        return new DataQuality(considered, WINDOW_DAYS, "HIGH", "Based on plenty of recent activity.");
    }

    private static SignalInput toInput(DemandSignal s) {
        return new SignalInput(s.getType(), s.getProductName(), s.getCategory(), s.getLatitude(),
                s.getLongitude(), s.getAreaKey(), s.getAreaName(), s.getCreatedAt(), s.getCustomerId());
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String c = s.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (c.isEmpty()) {
            return null;
        }
        return c.length() > 80 ? c.substring(0, 80) : c;
    }
}
