package backend.controller;

import backend.dto.NotificationDtos.NotificationList;
import backend.security.CurrentUser;
import backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public NotificationList list() {
        return notifications.list(CurrentUser.require().getUserId());
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        notifications.markAllRead(CurrentUser.require().getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable("id") Long id) {
        notifications.markRead(CurrentUser.require().getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
