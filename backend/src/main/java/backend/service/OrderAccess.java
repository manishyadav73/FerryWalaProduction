package backend.service;

import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.Role;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.OrderRepository;
import backend.repository.VendorRepository;
import backend.security.UserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that decides "is this caller one of the two parties of this request".
 * Everything private to a request (messages, meeting updates, shared locations, case attachments)
 * goes through here. A stranger, a different vendor or an admin gets the same 404 as a missing
 * request, so the existence of other people's requests is not revealed.
 */
@Component
public class OrderAccess {

    public record Party(CustomerOrder order, User me, User other, boolean customer, Vendor vendor) {
    }

    private final OrderRepository orders;
    private final VendorRepository vendors;

    public OrderAccess(OrderRepository orders, VendorRepository vendors) {
        this.orders = orders;
        this.vendors = vendors;
    }

    @Transactional(readOnly = true)
    public Party require(UserPrincipal who, Long orderId) {
        if (orderId == null) {
            throw ApiException.notFound("Request not found.");
        }
        CustomerOrder o = orders.findWithPartiesById(orderId)
                .orElseThrow(() -> ApiException.notFound("Request not found."));
        Vendor vendor = vendors.findWithUserById(o.getVendor().getId())
                .orElseThrow(() -> ApiException.notFound("Request not found."));
        boolean customer = who.getRole() == Role.CUSTOMER && o.getCustomer().getId().equals(who.getUserId());
        boolean vendorOwner = who.getRole() == Role.VENDOR && vendor.getUser().getId().equals(who.getUserId());
        if (!customer && !vendorOwner) {
            throw ApiException.notFound("Request not found.");
        }
        User me = customer ? o.getCustomer() : vendor.getUser();
        User other = customer ? vendor.getUser() : o.getCustomer();
        return new Party(o, me, other, customer, vendor);
    }
}
