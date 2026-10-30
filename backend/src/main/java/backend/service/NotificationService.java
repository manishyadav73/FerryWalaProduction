package backend.service;

import backend.dto.NotificationDtos.NotificationDto;
import backend.dto.NotificationDtos.NotificationList;
import backend.dto.NotificationDtos.VendorStatusEvent;
import backend.error.ApiException;
import backend.model.Notification;
import backend.model.NotificationType;
import backend.model.OrderStatus;
import backend.model.User;
import backend.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final SimpMessagingTemplate messaging;

    public NotificationService(NotificationRepository notifications, SimpMessagingTemplate messaging) {
        this.notifications = notifications;
        this.messaging = messaging;
    }

    /** Persist an in-app notification and push it live to that user, after commit. */
    @Transactional
    public void notifyUser(User user, NotificationType type, String title, String body, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(trim(title, 120));
        n.setBody(trim(body, 300));
        n.setLink(link);
        Notification saved = notifications.save(n);
        NotificationDto dto = toDto(saved);
        Long userId = user.getId();
        AfterCommit.run(() -> messaging.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", dto));
    }

    /** Tell a user's open screens that an order changed (they then refetch). */
    public void pushOrderEvent(Long userId, Long orderId, OrderStatus status) {
        Map<String, Object> payload = Map.of("orderId", orderId, "status", status.name());
        AfterCommit.run(() -> messaging.convertAndSendToUser(String.valueOf(userId), "/queue/orders", payload));
    }

    /** Public feed: vendor went online/offline or moved. */
    public void broadcastVendorStatus(VendorStatusEvent event) {
        AfterCommit.run(() -> messaging.convertAndSend("/topic/vendors/status", event));
    }

    @Transactional(readOnly = true)
    public NotificationList list(Long userId) {
        List<NotificationDto> items = notifications
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                .stream().map(NotificationService::toDto).toList();
        return new NotificationList(items, notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notification not found."));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
        }
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
