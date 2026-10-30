package backend.dto;

import backend.model.NotificationType;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationDto(Long id, NotificationType type, String title, String body, String link,
                                  boolean read, Instant createdAt) {
    }

    public record NotificationList(List<NotificationDto> items, long unread) {
    }

    /** Public live feed payload when a vendor goes online/offline or moves. */
    public record VendorStatusEvent(Long vendorId, boolean online, Double latitude, Double longitude, Instant at) {
    }
}
