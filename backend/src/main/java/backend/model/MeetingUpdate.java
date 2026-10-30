package backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/** An arrival/coordination update one party sent the other (running late, arrived, can't find...). */
@Entity
@Table(name = "meeting_updates", indexes = {
        @Index(name = "idx_meeting_updates_order", columnList = "order_id,created_at")
})
public class MeetingUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingUpdateType type;

    private Integer etaMinutes;

    @Column(length = 200)
    private String note;

    /** True when the user confirmed it from a Concierge suggestion. Informational only. */
    @Column(nullable = false)
    private boolean viaConcierge = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public MeetingUpdateType getType() { return type; }
    public void setType(MeetingUpdateType type) { this.type = type; }
    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isViaConcierge() { return viaConcierge; }
    public void setViaConcierge(boolean viaConcierge) { this.viaConcierge = viaConcierge; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
