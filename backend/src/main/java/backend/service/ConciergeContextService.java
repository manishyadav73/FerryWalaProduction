package backend.service;

import backend.concierge.ConciergeFacts;
import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.OrderStatus;
import backend.model.Role;
import backend.model.Vendor;
import backend.repository.OrderRepository;
import backend.repository.VendorRepository;
import backend.security.UserPrincipal;
import backend.service.OrderAccess.Party;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the verified facts a Concierge reply may use, strictly from records the caller is a party
 * to. If the caller names a request they are not part of, this throws the same 404 as a missing one.
 */
@Service
public class ConciergeContextService {

    private static final Set<OrderStatus> OPEN = EnumSet.of(OrderStatus.PENDING, OrderStatus.ACCEPTED,
            OrderStatus.ON_THE_WAY);

    private final OrderAccess access;
    private final OrderRepository orders;
    private final VendorRepository vendors;
    private final MeetingService meetings;
    private final String supportEmail;
    private final String supportPhone;

    public ConciergeContextService(OrderAccess access, OrderRepository orders, VendorRepository vendors,
                                   MeetingService meetings,
                                   @Value("${app.support.email:}") String supportEmail,
                                   @Value("${app.support.phone:}") String supportPhone) {
        this.access = access;
        this.orders = orders;
        this.vendors = vendors;
        this.meetings = meetings;
        this.supportEmail = blankToNull(supportEmail);
        this.supportPhone = blankToNull(supportPhone);
    }

    @Transactional(readOnly = true)
    public ConciergeFacts build(UserPrincipal who, String firstName, Long requestedOrderId) {
        if (who.getRole() != Role.CUSTOMER && who.getRole() != Role.VENDOR) {
            throw ApiException.forbidden("The Concierge is for customers and vendors.");
        }
        boolean vendorRole = who.getRole() == Role.VENDOR;
        Long orderId = requestedOrderId != null ? requestedOrderId : latestOpenOrderId(who);
        int pending = 0;
        int open = 0;
        if (vendorRole) {
            Vendor v = vendors.findByUserId(who.getUserId()).orElse(null);
            if (v != null) {
                pending = (int) orders.countByVendorIdAndStatus(v.getId(), OrderStatus.PENDING);
                open = pending + (int) orders.countByVendorIdAndStatus(v.getId(), OrderStatus.ACCEPTED)
                        + (int) orders.countByVendorIdAndStatus(v.getId(), OrderStatus.ON_THE_WAY);
            }
        }
        String role = vendorRole ? "VENDOR" : "CUSTOMER";
        if (orderId == null) {
            return new ConciergeFacts(role, firstName, null, null, null, null, null, false, false, false, null,
                    null, null, pending, open, supportEmail, supportPhone);
        }
        Party p = access.require(who, orderId); // 404 if not a party
        MeetingService.Snapshot s = meetings.snapshot(p);
        CustomerOrder o = p.order();
        String other = Text.firstName(p.other().getName());
        return new ConciergeFacts(role, firstName, o.getId(), o.getStatus().name(), p.vendor().getShopName(), other,
                o.getAreaName(), s.coordinationOpen(), s.myShareActive(), s.otherShareActive(), s.distanceMeters(),
                s.otherLastType() == null ? null : s.otherLastType().name(), s.otherLastEta(), pending, open,
                supportEmail, supportPhone);
    }

    private Long latestOpenOrderId(UserPrincipal who) {
        List<CustomerOrder> list;
        if (who.getRole() == Role.VENDOR) {
            Vendor v = vendors.findByUserId(who.getUserId()).orElse(null);
            if (v == null) {
                return null;
            }
            list = orders.findByVendorIdAndStatusInOrderByCreatedAtDesc(v.getId(), OPEN, PageRequest.of(0, 20));
        } else {
            list = orders.findByCustomerIdOrderByCreatedAtDesc(who.getUserId(), PageRequest.of(0, 20)).stream()
                    .filter(o -> o.getStatus().isOpen()).toList();
        }
        // Prefer a request where the meeting tools are open; otherwise the newest open one.
        for (CustomerOrder o : list) {
            if (o.getStatus() == OrderStatus.ACCEPTED || o.getStatus() == OrderStatus.ON_THE_WAY) {
                return o.getId();
            }
        }
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
