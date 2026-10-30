package backend.concierge;

import backend.demand.GeoUtil;
import backend.model.MeetingUpdateType;
import backend.model.OrderStatus;

import java.time.Instant;

/** Pure rules for meeting coordination and consented location sharing. No Spring, easy to test. */
public final class MeetingPolicy {

    public static final int MIN_SHARE_MINUTES = 5;
    public static final int MAX_SHARE_MINUTES = 60;
    public static final int DEFAULT_SHARE_MINUTES = 15;
    public static final int MAX_ETA_MINUTES = 180;

    private MeetingPolicy() {
    }

    /** Meeting updates and location sharing only make sense once the vendor accepted and until it ends. */
    public static boolean coordinationOpen(OrderStatus status) {
        return status == OrderStatus.ACCEPTED || status == OrderStatus.ON_THE_WAY;
    }

    /** Chatting is also allowed while the request is still waiting for the vendor. */
    public static boolean messagingOpen(OrderStatus status) {
        return status == OrderStatus.PENDING || coordinationOpen(status);
    }

    public static int clampShareMinutes(Integer requested) {
        if (requested == null) {
            return DEFAULT_SHARE_MINUTES;
        }
        return Math.max(MIN_SHARE_MINUTES, Math.min(MAX_SHARE_MINUTES, requested));
    }

    /** @return the validated ETA, or null when the update type does not carry one. */
    public static Integer validateEta(MeetingUpdateType type, Integer eta) {
        if (type == null) {
            throw new IllegalArgumentException("Choose what you want to tell the other person.");
        }
        if (!type.needsEta()) {
            return null;
        }
        if (eta == null || eta < 1 || eta > MAX_ETA_MINUTES) {
            throw new IllegalArgumentException("Enter a time between 1 and " + MAX_ETA_MINUTES + " minutes.");
        }
        return eta;
    }

    public static boolean shareActive(Instant expiresAt, Instant now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }

    /**
     * Straight-line distance between two shared points, rounded to 10 m. It is NOT a walking route
     * and is only meaningful while both parties are sharing; callers must say so.
     */
    public static long distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double meters = GeoUtil.haversineKm(lat1, lng1, lat2, lng2) * 1000.0;
        return Math.round(meters / 10.0) * 10L;
    }

    /** 5 decimal places is about 1 m: enough for the person who chose to share, no false precision. */
    public static double round5(double v) {
        return Math.round(v * 1e5) / 1e5;
    }
}
