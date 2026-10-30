package backend.service;

import backend.demand.GeoUtil;
import backend.dto.NotificationDtos.VendorStatusEvent;
import backend.dto.VendorDtos.LocationRequest;
import backend.dto.VendorDtos.ReviewDto;
import backend.dto.VendorDtos.UpdateVendorRequest;
import backend.dto.VendorDtos.VendorDetail;
import backend.dto.VendorDtos.VendorSummary;
import backend.error.ApiException;
import backend.model.Category;
import backend.model.Vendor;
import backend.repository.ReviewRepository;
import backend.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class VendorService {

    private static final double DEFAULT_RADIUS_KM = 5.0;
    private static final double MAX_RADIUS_KM = 25.0;
    private static final int MAX_NEARBY = 100;

    private final VendorRepository vendors;
    private final ReviewRepository reviews;
    private final ProductService productService;
    private final MediaStorageService media;
    private final NotificationService notifications;
    private final String apiBase;

    public VendorService(VendorRepository vendors, ReviewRepository reviews, ProductService productService,
                         MediaStorageService media, NotificationService notifications,
                         @Value("${app.public-api-url:}") String publicApiUrl) {
        this.vendors = vendors;
        this.reviews = reviews;
        this.productService = productService;
        this.media = media;
        this.notifications = notifications;
        this.apiBase = publicApiUrl == null ? "" : publicApiUrl.replaceAll("/+$", "");
    }

    // ------------------------------------------------------------- public

    @Transactional(readOnly = true)
    public List<VendorSummary> nearby(double lat, double lng, Double radiusKm, Category category, String q,
                                      boolean onlineOnly) {
        if (!GeoUtil.validLatLng(lat, lng)) {
            throw ApiException.badRequest("INVALID_LOCATION", "That location doesn't look right.");
        }
        double radius = radiusKm == null ? DEFAULT_RADIUS_KM : Math.max(0.5, Math.min(MAX_RADIUS_KM, radiusKm));
        double[] box = GeoUtil.boundingBox(lat, lng, radius);
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        return vendors.findInBox(box[0], box[1], box[2], box[3], PageRequest.of(0, 500)).stream()
                .filter(v -> category == null || v.getCategory() == category)
                .filter(v -> !onlineOnly || v.isOnline())
                .filter(v -> needle.isEmpty()
                        || v.getShopName().toLowerCase(Locale.ROOT).contains(needle)
                        || (v.getArea() != null && v.getArea().toLowerCase(Locale.ROOT).contains(needle)))
                .map(v -> toSummary(v, GeoUtil.haversineKm(lat, lng, v.getLatitude(), v.getLongitude())))
                .filter(s -> s.distanceKm() <= radius)
                .sorted(Comparator.comparing(VendorSummary::online).reversed()
                        .thenComparing(VendorSummary::distanceKm))
                .limit(MAX_NEARBY)
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorDetail detail(Long vendorId) {
        Vendor v = vendors.findWithUserById(vendorId)
                .filter(x -> x.getUser().isEnabled())
                .orElseThrow(() -> ApiException.notFound("Vendor not found."));
        return new VendorDetail(toSummary(v, null), productService.listPublic(vendorId));
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> reviewsFor(Long vendorId) {
        return reviews.findByVendorIdOrderByCreatedAtDesc(vendorId, PageRequest.of(0, 50)).stream()
                .map(r -> new ReviewDto(r.getId(), r.getRating(), r.getComment(),
                        firstName(r.getCustomer().getName()), r.getCreatedAt()))
                .toList();
    }

    // ------------------------------------------------------------- vendor self

    @Transactional(readOnly = true)
    public VendorSummary me(Long userId) {
        return toSummary(vendorOf(userId), null);
    }

    @Transactional
    public VendorSummary updateProfile(Long userId, UpdateVendorRequest req) {
        Vendor v = vendorOf(userId);
        v.setShopName(clean(req.shopName(), 80));
        v.setCategory(req.category());
        v.setDescription(blankToNull(clean(req.description(), 500)));
        v.setAddress(blankToNull(clean(req.address(), 200)));
        v.setArea(blankToNull(clean(req.area(), 80)));
        if (v.getShopName() == null || v.getShopName().length() < 2) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Shop name must be at least 2 characters.");
        }
        return toSummary(v, null);
    }

    @Transactional
    public VendorSummary setOnline(Long userId, boolean online) {
        Vendor v = vendorOf(userId);
        if (online && !v.hasLocation()) {
            throw ApiException.conflict("LOCATION_REQUIRED",
                    "Share your location before going online so customers can find you.");
        }
        v.setOnline(online);
        broadcast(v);
        return toSummary(v, null);
    }

    @Transactional
    public VendorSummary updateLocation(Long userId, LocationRequest req) {
        if (!GeoUtil.validLatLng(req.latitude(), req.longitude())) {
            throw ApiException.badRequest("INVALID_LOCATION", "That location doesn't look right.");
        }
        Vendor v = vendorOf(userId);
        v.setLatitude(req.latitude());
        v.setLongitude(req.longitude());
        v.setLocationUpdatedAt(Instant.now());
        String area = blankToNull(clean(req.area(), 80));
        if (area != null) {
            v.setArea(area);
        }
        String address = blankToNull(clean(req.address(), 200));
        if (address != null) {
            v.setAddress(address);
        }
        broadcast(v);
        return toSummary(v, null);
    }

    @Transactional
    public VendorSummary replaceImage(Long userId, MultipartFile file) {
        Vendor v = vendorOf(userId);
        String newName = media.storeVendorImage(file); // validates; throws before we touch the old one
        String old = v.getImageFilename();
        v.setImageFilename(newName);
        AfterCommit.run(() -> media.deleteQuietly(old));
        return toSummary(v, null);
    }

    @Transactional
    public VendorSummary removeImage(Long userId) {
        Vendor v = vendorOf(userId);
        String old = v.getImageFilename();
        v.setImageFilename(null);
        AfterCommit.run(() -> media.deleteQuietly(old));
        return toSummary(v, null);
    }

    /** Resolves the vendor owned by this user. */
    public Vendor vendorOf(Long userId) {
        return vendors.findByUserId(userId)
                .orElseThrow(() -> ApiException.forbidden("No vendor profile found for this account."));
    }

    // ------------------------------------------------------------- mapping

    public VendorSummary toSummary(Vendor v, Double distanceKm) {
        return new VendorSummary(
                v.getId(), v.getShopName(), v.getCategory(), v.getDescription(), v.getArea(), v.getAddress(),
                v.getLatitude(), v.getLongitude(),
                distanceKm == null ? null : Math.round(distanceKm * 10.0) / 10.0,
                v.isOnline(), v.isVerified(), v.getAverageRating(), v.getRatingCount(),
                imageUrl(v.getImageFilename()), v.getLocationUpdatedAt());
    }

    public String imageUrl(String filename) {
        return filename == null ? null : apiBase + "/api/media/vendors/" + filename;
    }

    private void broadcast(Vendor v) {
        notifications.broadcastVendorStatus(new VendorStatusEvent(v.getId(), v.isOnline(),
                v.getLatitude(), v.getLongitude(), Instant.now()));
    }

    private static String clean(String s, int max) {
        if (s == null) {
            return null;
        }
        String c = s.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        return c.length() > max ? c.substring(0, max) : c;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "Customer";
        }
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
