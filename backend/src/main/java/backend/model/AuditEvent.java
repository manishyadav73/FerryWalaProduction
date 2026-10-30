package backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Append-only record of consequential actions (support cases, location sharing, meeting updates). */
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_events_actor", columnList = "actor_id,created_at"),
        @Index(name = "idx_audit_events_order", columnList = "order_id,created_at")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "order_id")
    private Long orderId;

    /** Short, non-sensitive description. Never coordinates, message text or personal data. */
    @Column(length = 200)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
