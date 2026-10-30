package backend.service;

import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.OrderStatus;
import backend.model.Role;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.OrderRepository;
import backend.repository.VendorRepository;
import backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Object-level authorisation: only the customer and the owning vendor are parties to a request. */
class OrderAccessTest {

    private OrderAccess access;

    @BeforeEach
    void setUp() {
        OrderRepository orders = mock(OrderRepository.class);
        VendorRepository vendors = mock(VendorRepository.class);
        access = new OrderAccess(orders, vendors);

        User customer = user(1L, Role.CUSTOMER);
        User vendorUser = user(2L, Role.VENDOR);
        Vendor vendor = new Vendor();
        vendor.setId(10L);
        vendor.setUser(vendorUser);
        vendor.setShopName("Ravi Chaat");
        CustomerOrder order = new CustomerOrder();
        order.setId(100L);
        order.setCustomer(customer);
        order.setVendor(vendor);
        order.setStatus(OrderStatus.ACCEPTED);
        when(orders.findWithPartiesById(100L)).thenReturn(Optional.of(order));
        when(vendors.findWithUserById(10L)).thenReturn(Optional.of(vendor));
    }

    static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setName("User " + id);
        u.setRole(role);
        return u;
    }

    private static void assertNotFound(Runnable r) {
        ApiException ex = assertThrows(ApiException.class, r::run);
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void customerOfTheRequestIsAParty() {
        OrderAccess.Party p = access.require(new UserPrincipal(1L, "c@example.test", Role.CUSTOMER), 100L);
        assertTrue(p.customer());
        assertEquals(1L, p.me().getId());
        assertEquals(2L, p.other().getId());
    }

    @Test
    void owningVendorIsAParty() {
        OrderAccess.Party p = access.require(new UserPrincipal(2L, "v@example.test", Role.VENDOR), 100L);
        assertFalse(p.customer());
        assertEquals(2L, p.me().getId());
        assertEquals(1L, p.other().getId());
    }

    @Test
    void anotherCustomerGetsNotFound() {
        assertNotFound(() -> access.require(new UserPrincipal(99L, "x@example.test", Role.CUSTOMER), 100L));
    }

    @Test
    void anotherVendorGetsNotFound() {
        assertNotFound(() -> access.require(new UserPrincipal(98L, "y@example.test", Role.VENDOR), 100L));
    }

    @Test
    void adminIsNotAPartyToPrivateMeetingData() {
        assertNotFound(() -> access.require(new UserPrincipal(3L, "a@example.test", Role.ADMIN), 100L));
    }

    @Test
    void aVendorUserIdEqualToTheCustomerIdDoesNotGrantCustomerAccess() {
        // Same numeric id as the customer (1) but the VENDOR role: must not be treated as the customer.
        assertNotFound(() -> access.require(new UserPrincipal(1L, "z@example.test", Role.VENDOR), 100L));
    }

    @Test
    void missingOrNullRequestIsNotFound() {
        assertNotFound(() -> access.require(new UserPrincipal(1L, "c@example.test", Role.CUSTOMER), 555L));
        assertNotFound(() -> access.require(new UserPrincipal(1L, "c@example.test", Role.CUSTOMER), null));
    }
}
