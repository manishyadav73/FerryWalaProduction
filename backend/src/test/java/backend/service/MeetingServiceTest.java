package backend.service;

import backend.dto.ConciergeDtos.MeetingDto;
import backend.dto.ConciergeDtos.MeetingUpdateRequest;
import backend.dto.ConciergeDtos.MyShare;
import backend.dto.ConciergeDtos.ShareRequest;
import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.LocationShare;
import backend.model.MeetingUpdateType;
import backend.model.OrderStatus;
import backend.model.Role;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.LocationShareRepository;
import backend.repository.MeetingUpdateRepository;
import backend.repository.OrderMessageRepository;
import backend.repository.OrderRepository;
import backend.repository.VendorRepository;
import backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Meeting rules with mocked storage: consent, time limits, who can see what. */
class MeetingServiceTest {

    private static final UserPrincipal CUSTOMER = new UserPrincipal(1L, "c@example.test", Role.CUSTOMER);
    private static final UserPrincipal VENDOR = new UserPrincipal(2L, "v@example.test", Role.VENDOR);

    private LocationShareRepository shares;
    private MeetingService service;
    private CustomerOrder order;
    private User customer;
    private User vendorUser;

    @BeforeEach
    void setUp() {
        OrderRepository orders = mock(OrderRepository.class);
        VendorRepository vendors = mock(VendorRepository.class);
        shares = mock(LocationShareRepository.class);
        customer = OrderAccessTest.user(1L, Role.CUSTOMER);
        vendorUser = OrderAccessTest.user(2L, Role.VENDOR);
        Vendor vendor = new Vendor();
        vendor.setId(10L);
        vendor.setUser(vendorUser);
        vendor.setShopName("Ravi Chaat");
        order = new CustomerOrder();
        order.setId(100L);
        order.setCustomer(customer);
        order.setVendor(vendor);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setLatitude(13.0827);
        order.setLongitude(80.2707);
        when(orders.findWithPartiesById(100L)).thenReturn(Optional.of(order));
        when(vendors.findWithUserById(10L)).thenReturn(Optional.of(vendor));
        when(shares.findByOrderIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(shares.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new MeetingService(new OrderAccess(orders, vendors), shares, mock(MeetingUpdateRepository.class),
                mock(OrderMessageRepository.class), mock(NotificationService.class), mock(AuditService.class));
    }

    private static LocationShare share(User who, CustomerOrder o, double lat, double lng, Instant expires) {
        LocationShare s = new LocationShare();
        s.setId(who.getId() * 1000);
        s.setUser(who);
        s.setOrder(o);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setExpiresAt(expires);
        return s;
    }

    @Test
    void sharingIsRefusedUntilTheVendorAccepts() {
        order.setStatus(OrderStatus.PENDING);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.startOrRefreshShare(CUSTOMER, 100L, new ShareRequest(13.08, 80.27, 15)));
        assertEquals("MEETING_CLOSED", ex.getCode());
        verify(shares, never()).save(any());
    }

    @Test
    void invalidCoordinatesAreRejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.startOrRefreshShare(CUSTOMER, 100L, new ShareRequest(0.0, 0.0, 15)));
        assertEquals("INVALID_LOCATION", ex.getCode());
    }

    @Test
    void startingAShareCreatesATimeLimitedConsent() {
        MyShare res = service.startOrRefreshShare(CUSTOMER, 100L, new ShareRequest(13.08, 80.27, 30));
        assertTrue(res.active());
        ArgumentCaptor<LocationShare> saved = ArgumentCaptor.forClass(LocationShare.class);
        verify(shares).save(saved.capture());
        long minutes = Duration.between(Instant.now(), saved.getValue().getExpiresAt()).toMinutes();
        assertTrue(minutes >= 28 && minutes <= 30, "was " + minutes);
    }

    @Test
    void durationIsCappedAtOneHour() {
        service.startOrRefreshShare(CUSTOMER, 100L, new ShareRequest(13.08, 80.27, 100_000));
        ArgumentCaptor<LocationShare> saved = ArgumentCaptor.forClass(LocationShare.class);
        verify(shares).save(saved.capture());
        assertTrue(Duration.between(Instant.now(), saved.getValue().getExpiresAt()).toMinutes() <= 60);
    }

    @Test
    void refreshingAnActiveShareMovesThePointButNeverExtendsTheTimeLimit() {
        Instant expires = Instant.now().plus(Duration.ofMinutes(5));
        LocationShare existing = share(customer, order, 13.0, 80.0, expires);
        when(shares.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));

        service.startOrRefreshShare(CUSTOMER, 100L, new ShareRequest(13.09, 80.28, 60));

        assertEquals(expires, existing.getExpiresAt());
        assertEquals(13.09, existing.getLatitude());
    }

    @Test
    void stoppingDeletesTheStoredPosition() {
        LocationShare existing = share(customer, order, 13.0, 80.0, Instant.now().plus(Duration.ofMinutes(5)));
        when(shares.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));
        MyShare res = service.stopShare(CUSTOMER, 100L);
        assertFalse(res.active());
        verify(shares).delete(existing);
    }

    @Test
    void theOtherPartysPositionIsHiddenUnlessTheyAreSharing() {
        MeetingDto dto = service.view(CUSTOMER, 100L);
        assertFalse(dto.otherShare().active());
        assertNull(dto.otherShare().latitude());
        assertNull(dto.distanceMeters());
    }

    @Test
    void anExpiredShareIsNeverShown() {
        LocationShare expired = share(vendorUser, order, 13.0, 80.0, Instant.now().minusSeconds(30));
        when(shares.findByOrderIdAndUserId(100L, 2L)).thenReturn(Optional.of(expired));
        MeetingDto dto = service.view(CUSTOMER, 100L);
        assertFalse(dto.otherShare().active());
        assertNull(dto.otherShare().latitude());
    }

    @Test
    void positionsAreShownTogetherWithDistanceOnlyWhileBothShare() {
        Instant later = Instant.now().plus(Duration.ofMinutes(10));
        when(shares.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(share(customer, order, 13.0000, 80.0, later)));
        when(shares.findByOrderIdAndUserId(100L, 2L)).thenReturn(Optional.of(share(vendorUser, order, 13.0010, 80.0, later)));
        MeetingDto dto = service.view(CUSTOMER, 100L);
        assertTrue(dto.myShare().active());
        assertTrue(dto.otherShare().active());
        assertEquals(13.001, dto.otherShare().latitude());
        assertTrue(dto.distanceMeters() >= 100 && dto.distanceMeters() <= 120);
    }

    @Test
    void sharedPositionsDisappearWhenTheRequestEnds() {
        Instant later = Instant.now().plus(Duration.ofMinutes(10));
        when(shares.findByOrderIdAndUserId(100L, 2L)).thenReturn(Optional.of(share(vendorUser, order, 13.0010, 80.0, later)));
        order.setStatus(OrderStatus.COMPLETED);
        MeetingDto dto = service.view(CUSTOMER, 100L);
        assertFalse(dto.otherShare().active());
        assertNull(dto.otherShare().latitude());
    }

    @Test
    void strangersCannotReadOrSendAnything() {
        UserPrincipal stranger = new UserPrincipal(77L, "s@example.test", Role.CUSTOMER);
        assertEquals("NOT_FOUND", assertThrows(ApiException.class, () -> service.view(stranger, 100L)).getCode());
        assertEquals("NOT_FOUND", assertThrows(ApiException.class, () -> service.sendMessage(stranger, 100L, "hi")).getCode());
        assertEquals("NOT_FOUND", assertThrows(ApiException.class, () -> service.stopShare(stranger, 100L)).getCode());
    }

    @Test
    void runningLateNeedsAnEta() {
        ApiException ex = assertThrows(ApiException.class, () -> service.postUpdate(CUSTOMER, 100L,
                new MeetingUpdateRequest(MeetingUpdateType.RUNNING_LATE, null, null, "APP")));
        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void messagesAreClosedOnceTheRequestHasEnded() {
        order.setStatus(OrderStatus.CANCELLED);
        ApiException ex = assertThrows(ApiException.class, () -> service.sendMessage(CUSTOMER, 100L, "hello"));
        assertEquals("CHAT_CLOSED", ex.getCode());
    }
}
