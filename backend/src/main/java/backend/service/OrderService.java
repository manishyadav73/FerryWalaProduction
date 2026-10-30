package backend.service;

import backend.demand.GeoUtil;
import backend.dto.OrderDtos.OrderDto;
import backend.dto.OrderDtos.OrderItemDto;
import backend.dto.OrderDtos.OrderItemRequest;
import backend.dto.OrderDtos.PlaceOrderRequest;
import backend.dto.OrderDtos.ReviewRequest;
import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.NotificationType;
import backend.model.OrderItem;
import backend.model.OrderStatus;
import backend.model.Product;
import backend.model.Review;
import backend.model.Role;
import backend.model.SignalType;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.OrderRepository;
import backend.repository.ProductRepository;
import backend.repository.ReviewRepository;
import backend.repository.UserRepository;
import backend.repository.VendorRepository;
import backend.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    public record VendorStats(long pending, long open, long completedToday, BigDecimal revenueToday,
                              BigDecimal revenueLast7Days, double rating, int ratingCount) {
    }

    private static final Set<OrderStatus> OPEN = EnumSet.of(OrderStatus.PENDING, OrderStatus.ACCEPTED,
            OrderStatus.ON_THE_WAY);

    private final OrderRepository orders;
    private final ProductRepository products;
    private final VendorRepository vendors;
    private final UserRepository users;
    private final ReviewRepository reviews;
    private final NotificationService notifications;
    private final DemandService demand;
    private final VendorService vendorService;

    public OrderService(OrderRepository orders, ProductRepository products, VendorRepository vendors,
                        UserRepository users, ReviewRepository reviews, NotificationService notifications,
                        DemandService demand, VendorService vendorService) {
        this.orders = orders;
        this.products = products;
        this.vendors = vendors;
        this.users = users;
        this.reviews = reviews;
        this.notifications = notifications;
        this.demand = demand;
        this.vendorService = vendorService;
    }

    // ------------------------------------------------------------ customer

    @Transactional
    public OrderDto place(Long customerId, PlaceOrderRequest req) {
        if (!GeoUtil.validLatLng(req.latitude(), req.longitude())) {
            throw ApiException.badRequest("INVALID_LOCATION", "Choose where the vendor should meet you.");
        }
        Vendor vendor = vendors.findWithUserById(req.vendorId())
                .filter(v -> v.getUser().isEnabled())
                .orElseThrow(() -> ApiException.notFound("Vendor not found."));
        User customer = users.findById(customerId)
                .orElseThrow(() -> ApiException.unauthorized("Please sign in again."));

        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (OrderItemRequest item : req.items()) {
            wanted.merge(item.productId(), item.quantity(), Integer::sum);
        }
        List<Product> found = products.findAllById(wanted.keySet());
        if (found.size() != wanted.size()) {
            throw ApiException.badRequest("PRODUCT_GONE", "One of those products is no longer available.");
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setVendor(vendor);
        order.setLatitude(req.latitude());
        order.setLongitude(req.longitude());
        order.setAreaName(clean(req.areaName(), 80));
        order.setNote(clean(req.note(), 300));

        BigDecimal total = BigDecimal.ZERO;
        for (Product p : found) {
            int qty = wanted.get(p.getId());
            if (!p.getVendor().getId().equals(vendor.getId())) {
                throw ApiException.badRequest("WRONG_VENDOR", "Those products belong to a different vendor.");
            }
            if (!p.isAvailable()) {
                throw ApiException.conflict("PRODUCT_UNAVAILABLE", p.getName() + " isn't available right now.");
            }
            if (qty > 50) {
                throw ApiException.badRequest("QUANTITY_TOO_HIGH", "Quantity is too high for " + p.getName() + ".");
            }
            if (p.getStock() < qty) {
                throw ApiException.conflict("OUT_OF_STOCK", p.getStock() == 0
                        ? p.getName() + " is sold out."
                        : "Only " + p.getStock() + " " + p.getName() + " left.");
            }
            OrderItem item = new OrderItem();
            item.setProductId(p.getId());
            item.setProductName(p.getName());
            item.setCategory(p.getCategory());
            item.setUnit(p.getUnit());
            item.setUnitPrice(p.getPrice());
            item.setQuantity(qty);
            order.addItem(item);
            total = total.add(item.lineTotal());
        }
        order.setTotalAmount(total);
        CustomerOrder saved = orders.save(order);

        demand.recordFromOrder(customer, saved, SignalType.PRE_ORDER);

        notifications.notifyUser(vendor.getUser(), NotificationType.ORDER_NEW,
                "New request from " + firstName(customer.getName()),
                summary(saved) + " · ₹" + total.stripTrailingZeros().toPlainString(), "/vendor");
        notifications.pushOrderEvent(vendor.getUser().getId(), saved.getId(), saved.getStatus());
        notifications.pushOrderEvent(customerId, saved.getId(), saved.getStatus());
        return toDto(saved, false);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> mine(Long customerId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, 100)).stream()
                .map(o -> toDto(o, o.getStatus() == OrderStatus.COMPLETED && reviews.existsByOrderId(o.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(UserPrincipal who, Long orderId) {
        CustomerOrder o = orders.findWithPartiesById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found."));
        boolean customer = o.getCustomer().getId().equals(who.getUserId());
        boolean vendorOwner = who.getRole() == Role.VENDOR && vendors.findByUserId(who.getUserId())
                .map(v -> v.getId().equals(o.getVendor().getId())).orElse(false);
        if (!customer && !vendorOwner && who.getRole() != Role.ADMIN) {
            throw ApiException.notFound("Order not found."); // don't reveal that it exists
        }
        return toDto(o, o.getStatus() == OrderStatus.COMPLETED && reviews.existsByOrderId(o.getId()));
    }

    @Transactional
    public OrderDto cancelByCustomer(Long customerId, Long orderId) {
        CustomerOrder o = orders.findWithPartiesById(orderId)
                .filter(x -> x.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> ApiException.notFound("Order not found."));
        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.ACCEPTED) {
            throw ApiException.conflict("INVALID_TRANSITION", "This request can't be cancelled any more.");
        }
        boolean wasAccepted = o.getStatus() == OrderStatus.ACCEPTED;
        o.setStatus(OrderStatus.CANCELLED);
        o.setUpdatedAt(Instant.now());
        if (wasAccepted) {
            restoreStock(o);
        }
        User vendorUser = vendors.findWithUserById(o.getVendor().getId()).orElseThrow().getUser();
        notifications.notifyUser(vendorUser, NotificationType.ORDER_UPDATE,
                firstName(o.getCustomer().getName()) + " cancelled a request", summary(o), "/vendor");
        notifications.pushOrderEvent(vendorUser.getId(), o.getId(), o.getStatus());
        notifications.pushOrderEvent(customerId, o.getId(), o.getStatus());
        return toDto(o, false);
    }

    @Transactional
    public void review(Long customerId, Long orderId, ReviewRequest req) {
        CustomerOrder o = orders.findWithPartiesById(orderId)
                .filter(x -> x.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> ApiException.notFound("Order not found."));
        if (o.getStatus() != OrderStatus.COMPLETED) {
            throw ApiException.conflict("NOT_COMPLETED", "You can review a request once it's completed.");
        }
        if (reviews.existsByOrderId(orderId)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "You've already reviewed this request.");
        }
        Review r = new Review();
        r.setOrder(o);
        r.setCustomer(o.getCustomer());
        r.setVendor(o.getVendor());
        r.setRating(req.rating());
        r.setComment(clean(req.comment(), 500));
        reviews.save(r);

        Vendor v = vendors.findWithUserById(o.getVendor().getId()).orElseThrow();
        v.setRatingSum(v.getRatingSum() + req.rating());
        v.setRatingCount(v.getRatingCount() + 1);
        notifications.notifyUser(v.getUser(), NotificationType.REVIEW_NEW,
                firstName(o.getCustomer().getName()) + " rated you " + req.rating() + "/5",
                r.getComment(), "/vendor");
    }

    // ------------------------------------------------------------ vendor

    @Transactional(readOnly = true)
    public List<OrderDto> forVendor(Long vendorUserId, String filter) {
        Vendor vendor = vendorService.vendorOf(vendorUserId);
        List<CustomerOrder> list;
        PageRequest page = PageRequest.of(0, 100);
        if (filter == null || filter.isBlank() || filter.equalsIgnoreCase("open")) {
            list = orders.findByVendorIdAndStatusInOrderByCreatedAtDesc(vendor.getId(), OPEN, page);
        } else if (filter.equalsIgnoreCase("all")) {
            list = orders.findByVendorIdOrderByCreatedAtDesc(vendor.getId(), page);
        } else {
            OrderStatus status;
            try {
                status = OrderStatus.valueOf(filter.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("BAD_REQUEST", "Unknown status filter.");
            }
            list = orders.findByVendorIdAndStatusInOrderByCreatedAtDesc(vendor.getId(), Set.of(status), page);
        }
        return list.stream().map(o -> toDto(o, false)).toList();
    }

    @Transactional
    public OrderDto transition(Long vendorUserId, Long orderId, OrderStatus next) {
        Vendor vendor = vendorService.vendorOf(vendorUserId);
        CustomerOrder o = orders.findWithPartiesById(orderId)
                .filter(x -> x.getVendor().getId().equals(vendor.getId()))
                .orElseThrow(() -> ApiException.notFound("Order not found."));
        OrderStatus current = o.getStatus();
        if (!allowed(current, next)) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "You can't move a " + label(current) + " request to " + label(next) + ".");
        }

        if (next == OrderStatus.ACCEPTED) {
            reserveStock(o);
        } else if (next == OrderStatus.CANCELLED && (current == OrderStatus.ACCEPTED || current == OrderStatus.ON_THE_WAY)) {
            restoreStock(o);
        } else if (next == OrderStatus.COMPLETED) {
            demand.recordFromOrder(o.getCustomer(), o, SignalType.PURCHASE);
        }
        o.setStatus(next);
        o.setUpdatedAt(Instant.now());

        String shop = vendor.getShopName();
        notifications.notifyUser(o.getCustomer(), NotificationType.ORDER_UPDATE,
                customerMessage(next, shop), summary(o), "/orders");
        notifications.pushOrderEvent(o.getCustomer().getId(), o.getId(), next);
        notifications.pushOrderEvent(vendorUserId, o.getId(), next);
        return toDto(o, false);
    }

    @Transactional(readOnly = true)
    public VendorStats vendorStats(Long vendorUserId) {
        Vendor vendor = vendorService.vendorOf(vendorUserId);
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Instant startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant sevenDays = Instant.now().minusSeconds(7L * 24 * 3600);
        long pending = orders.countByVendorIdAndStatus(vendor.getId(), OrderStatus.PENDING);
        long open = pending + orders.countByVendorIdAndStatus(vendor.getId(), OrderStatus.ACCEPTED)
                + orders.countByVendorIdAndStatus(vendor.getId(), OrderStatus.ON_THE_WAY);
        BigDecimal today = orders.sumVendorTotalSince(vendor.getId(), OrderStatus.COMPLETED, startOfToday);
        BigDecimal week = orders.sumVendorTotalSince(vendor.getId(), OrderStatus.COMPLETED, sevenDays);
        long completedToday = orders.findByVendorIdAndStatusInOrderByCreatedAtDesc(vendor.getId(),
                        Set.of(OrderStatus.COMPLETED), PageRequest.of(0, 200)).stream()
                .filter(x -> !x.getUpdatedAt().isBefore(startOfToday)).count();
        return new VendorStats(pending, open, completedToday, today, week, vendor.getAverageRating(),
                vendor.getRatingCount());
    }

    // ------------------------------------------------------------ helpers

    private static boolean allowed(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.ACCEPTED || to == OrderStatus.REJECTED;
            case ACCEPTED -> to == OrderStatus.ON_THE_WAY || to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED;
            case ON_THE_WAY -> to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED;
            default -> false;
        };
    }

    private void reserveStock(CustomerOrder o) {
        for (OrderItem item : itemsByProductId(o)) {
            Product p = products.findByIdForUpdate(item.getProductId()).orElse(null);
            if (p == null) {
                continue; // deleted since the request was placed; nothing to reserve
            }
            if (p.getStock() < item.getQuantity()) {
                throw ApiException.conflict("OUT_OF_STOCK", "Not enough " + p.getName()
                        + " in stock (" + p.getStock() + " left). Update your stock first.");
            }
            p.setStock(p.getStock() - item.getQuantity());
        }
    }

    private void restoreStock(CustomerOrder o) {
        for (OrderItem item : itemsByProductId(o)) {
            products.findByIdForUpdate(item.getProductId())
                    .ifPresent(p -> p.setStock(p.getStock() + item.getQuantity()));
        }
    }

    /** Locks are taken in id order so two orders touching the same products can't deadlock. */
    private static List<OrderItem> itemsByProductId(CustomerOrder o) {
        return o.getItems().stream()
                .filter(i -> i.getProductId() != null)
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .toList();
    }

    private static String customerMessage(OrderStatus s, String shop) {
        return switch (s) {
            case ACCEPTED -> shop + " accepted your request";
            case ON_THE_WAY -> shop + " is on the way";
            case COMPLETED -> "Your request from " + shop + " is complete";
            case REJECTED -> shop + " couldn't take your request";
            case CANCELLED -> shop + " cancelled your request";
            default -> "Update from " + shop;
        };
    }

    private static String label(OrderStatus s) {
        return s.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String summary(CustomerOrder o) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (OrderItem i : o.getItems()) {
            if (shown == 3) {
                sb.append(", +").append(o.getItems().size() - 3).append(" more");
                break;
            }
            if (shown > 0) {
                sb.append(", ");
            }
            sb.append(i.getQuantity()).append(" × ").append(i.getProductName());
            shown++;
        }
        return sb.toString();
    }

    public OrderDto toDto(CustomerOrder o, boolean reviewed) {
        List<OrderItemDto> items = o.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getProductName(), i.getUnit(), i.getUnitPrice(),
                        i.getQuantity(), i.lineTotal()))
                .toList();
        Vendor v = o.getVendor();
        return new OrderDto(o.getId(), o.getStatus(), o.getTotalAmount(), o.getNote(), o.getLatitude(),
                o.getLongitude(), o.getAreaName(), v.getId(), v.getShopName(),
                vendorService.imageUrl(v.getImageFilename()), o.getCustomer().getId(),
                o.getCustomer().getName(), items, reviewed, o.getCreatedAt(), o.getUpdatedAt());
    }

    private static String clean(String s, int max) {
        if (s == null) {
            return null;
        }
        String c = s.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (c.isEmpty()) {
            return null;
        }
        return c.length() > max ? c.substring(0, max) : c;
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "A customer";
        }
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
