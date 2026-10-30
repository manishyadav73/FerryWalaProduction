package backend.concierge;

import backend.model.MeetingUpdateType;
import backend.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingPolicyTest {

    @Test
    void coordinationOnlyWhileAcceptedOrOnTheWay() {
        assertFalse(MeetingPolicy.coordinationOpen(OrderStatus.PENDING));
        assertTrue(MeetingPolicy.coordinationOpen(OrderStatus.ACCEPTED));
        assertTrue(MeetingPolicy.coordinationOpen(OrderStatus.ON_THE_WAY));
        assertFalse(MeetingPolicy.coordinationOpen(OrderStatus.COMPLETED));
        assertFalse(MeetingPolicy.coordinationOpen(OrderStatus.CANCELLED));
        assertFalse(MeetingPolicy.coordinationOpen(OrderStatus.REJECTED));
    }

    @Test
    void messagingAlsoOpenWhilePending() {
        assertTrue(MeetingPolicy.messagingOpen(OrderStatus.PENDING));
        assertFalse(MeetingPolicy.messagingOpen(OrderStatus.COMPLETED));
    }

    @Test
    void shareDurationIsClampedToFiveToSixtyMinutes() {
        assertEquals(15, MeetingPolicy.clampShareMinutes(null));
        assertEquals(5, MeetingPolicy.clampShareMinutes(1));
        assertEquals(60, MeetingPolicy.clampShareMinutes(10_000));
        assertEquals(30, MeetingPolicy.clampShareMinutes(30));
    }

    @Test
    void etaIsRequiredAndBoundedOnlyForTypesThatCarryOne() {
        assertEquals(10, MeetingPolicy.validateEta(MeetingUpdateType.RUNNING_LATE, 10));
        assertThrows(IllegalArgumentException.class, () -> MeetingPolicy.validateEta(MeetingUpdateType.RUNNING_LATE, null));
        assertThrows(IllegalArgumentException.class, () -> MeetingPolicy.validateEta(MeetingUpdateType.RUNNING_LATE, 0));
        assertThrows(IllegalArgumentException.class, () -> MeetingPolicy.validateEta(MeetingUpdateType.PROPOSE_TIME, 999));
        assertNull(MeetingPolicy.validateEta(MeetingUpdateType.ARRIVED, 50));
        assertThrows(IllegalArgumentException.class, () -> MeetingPolicy.validateEta(null, 5));
    }

    @Test
    void shareIsActiveOnlyBeforeExpiry() {
        Instant now = Instant.parse("2026-10-30T10:00:00Z");
        assertTrue(MeetingPolicy.shareActive(now.plusSeconds(1), now));
        assertFalse(MeetingPolicy.shareActive(now, now));
        assertFalse(MeetingPolicy.shareActive(now.minusSeconds(60), now));
        assertFalse(MeetingPolicy.shareActive(null, now));
    }

    @Test
    void distanceIsStraightLineRoundedToTenMetres() {
        assertEquals(0, MeetingPolicy.distanceMeters(13.0827, 80.2707, 13.0827, 80.2707));
        // 0.001 degree of latitude is about 111 m
        long d = MeetingPolicy.distanceMeters(13.0000, 80.0000, 13.0010, 80.0000);
        assertEquals(0, d % 10);
        assertTrue(d >= 100 && d <= 120, "was " + d);
    }

    @Test
    void sharedCoordinatesAreRoundedToAboutOneMetre() {
        assertEquals(13.08271, MeetingPolicy.round5(13.0827149999));
    }
}
