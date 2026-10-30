package backend.service;

import backend.demand.DemandEngine.Opportunity;
import backend.dto.AdminDtos.AdminUserDto;
import backend.dto.AdminDtos.AdminVendorDto;
import backend.dto.AdminDtos.PageDto;
import backend.dto.AdminDtos.Stats;
import backend.dto.AdminDtos.SuspiciousFlag;
import backend.dto.OrderDtos.OrderDto;
import backend.dto.ProductDtos.ProductDto;
import backend.error.ApiException;
import backend.model.OrderStatus;
import backend.model.Role;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.DemandSignalRepository;
import backend.repository.OrderRepository;
import backend.repository.ProductRepository;
import backend.repository.ReviewRepository;
import backend.repository.UserRepository;
import backend.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository users;
    private final VendorRepository vendors;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final DemandSignalRepository signals;
    private final ReviewRepository reviews;
    private final DemandService demand;
    private final OrderService orderService;

    public AdminService(UserRepository users, VendorRepository vendors, ProductRepository products,
                        OrderRepository orders, DemandSignalRepository signals, ReviewRepository reviews,
                        DemandService demand, OrderService orderService) {
        this.users = users;
        this.vendors = vendors;
        this.products = products;
        this.orders = orders;
        this.signals = signals;
        this.reviews = reviews;
        this.demand = demand;
        this.orderService = orderService;
    }

    public Stats stats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), orders.countByStatus(s));
        }
        return new Stats(users.count(), users.countByRole(Role.CUSTOMER), users.countByRole(Role.VENDOR),
                vendors.countByVerifiedTrue(), vendors.countByOnlineTrue(), products.count(), orders.count(),
                byStatus, orders.sumTotalByStatus(OrderStatus.COMPLETED),
                signals.countByCreatedAtAfter(Instant.now().minus(Duration.ofDays(7))), reviews.count());
    }

    public PageDto<AdminUserDto> users(String q, int page, int size) {
        String like = "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
        Page<User> p = users.search(like, PageRequest.of(page(page), size(size), Sort.by("createdAt").descending()));
        return new PageDto<>(p.getContent().stream().map(AdminService::toDto).toList(), p.getNumber(), p.getSize(),
                p.getTotalElements());
    }

    @Transactional
    public AdminUserDto setUserEnabled(Long actingAdminId, Long userId, boolean enabled) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        if (u.getId().equals(actingAdminId)) {
            throw ApiException.conflict("SELF_ACTION", "You can't suspend your own account.");
        }
        if (u.getRole() == Role.ADMIN) {
            throw ApiException.conflict("ADMIN_PROTECTED", "Admin accounts can't be suspended here.");
        }
        u.setEnabled(enabled);
        return toDto(u);
    }

    public PageDto<AdminVendorDto> vendors(int page, int size) {
        int pg = page(page);
        int sz = size(size);
        List<AdminVendorDto> items = vendors.findAllWithUser(PageRequest.of(pg, sz, Sort.by("createdAt").descending()))
                .stream().map(AdminService::toDto).toList();
        return new PageDto<>(items, pg, sz, vendors.count());
    }

    @Transactional
    public AdminVendorDto setVendorVerified(Long vendorId, boolean verified) {
        Vendor v = vendors.findWithUserById(vendorId).orElseThrow(() -> ApiException.notFound("Vendor not found."));
        v.setVerified(verified);
        return toDto(v);
    }

    public PageDto<ProductDto> products(int page, int size) {
        int pg = page(page);
        int sz = size(size);
        Page<backend.model.Product> p = products.findAllBy(PageRequest.of(pg, sz, Sort.by("createdAt").descending()));
        return new PageDto<>(p.getContent().stream().map(ProductService::toDto).toList(), pg, sz, p.getTotalElements());
    }

    @Transactional
    public void deleteProduct(Long productId) {
        if (!products.existsById(productId)) {
            throw ApiException.notFound("Product not found.");
        }
        products.deleteById(productId);
    }

    public List<OrderDto> orders(String status) {
        PageRequest page = PageRequest.of(0, 100);
        List<backend.model.CustomerOrder> list;
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            list = orders.findAllByOrderByCreatedAtDesc(page);
        } else {
            OrderStatus s;
            try {
                s = OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("BAD_REQUEST", "Unknown status filter.");
            }
            list = orders.findByStatusOrderByCreatedAtDesc(s, page);
        }
        return list.stream().map(o -> orderService.toDto(o, false)).toList();
    }

    public List<Opportunity> demandAreas() {
        return demand.platformAreas();
    }

    /** Simple, explainable heuristics. A flag is a prompt to look, not a verdict. */
    public List<SuspiciousFlag> suspicious() {
        List<SuspiciousFlag> flags = new ArrayList<>();
        Instant now = Instant.now();

        for (Object[] row : signals.heavySignalUsers(now.minus(Duration.ofDays(1)), 60, PageRequest.of(0, 20))) {
            flags.add(new SuspiciousFlag("SIGNAL_SPAM", "MEDIUM", "user #" + row[0],
                    row[1] + " demand signals in the last 24 hours"));
        }
        for (Object[] row : orders.repeatCancellers(OrderStatus.CANCELLED, now.minus(Duration.ofDays(7)), 5,
                PageRequest.of(0, 20))) {
            flags.add(new SuspiciousFlag("REPEAT_CANCELLER", "LOW", "user #" + row[0],
                    row[1] + " cancelled requests in the last 7 days"));
        }
        for (Vendor v : vendors.findAllWithUser(PageRequest.of(0, 200))) {
            long total = orders.countByVendorId(v.getId());
            if (total < 5) {
                continue;
            }
            long bad = orders.countByVendorIdAndStatus(v.getId(), OrderStatus.REJECTED)
                    + orders.countByVendorIdAndStatus(v.getId(), OrderStatus.CANCELLED);
            if (bad * 100 / total >= 60) {
                flags.add(new SuspiciousFlag("HIGH_CANCELLATION", "MEDIUM", v.getShopName() + " (#" + v.getId() + ")",
                        bad + " of " + total + " requests rejected or cancelled"));
            }
        }
        return flags;
    }

    private static AdminUserDto toDto(User u) {
        return new AdminUserDto(u.getId(), u.getName(), u.getUsername(), u.getEmail(), u.getRole(), u.isEnabled(),
                u.isEmailVerified(), u.getCreatedAt());
    }

    private static AdminVendorDto toDto(Vendor v) {
        return new AdminVendorDto(v.getId(), v.getShopName(), v.getCategory(), v.getUser().getName(),
                v.getUser().getEmail(), v.isVerified(), v.isOnline(), v.getUser().isEnabled(),
                v.getAverageRating(), v.getRatingCount(), v.getArea(), v.getCreatedAt());
    }

    private static int page(int page) {
        return Math.max(0, page);
    }

    private static int size(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
