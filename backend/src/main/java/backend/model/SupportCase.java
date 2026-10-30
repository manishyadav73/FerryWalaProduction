package backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/** A problem report handed to a human on the FerryWala team. */
@Entity
@Table(name = "support_cases", indexes = {
        @Index(name = "idx_support_cases_creator", columnList = "creator_id,created_at"),
        @Index(name = "idx_support_cases_status", columnList = "status,created_at")
})
public class SupportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** Optional: the request the problem is about. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SupportCategory category = SupportCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SupportStatus status = SupportStatus.OPEN;

    @Column(nullable = false, length = 1000)
    private String summary;

    /** APP or CONCIERGE: where the user filed it from. */
    @Column(nullable = false, length = 12)
    private String source = "APP";

    @Column(length = 500)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public SupportCategory getCategory() { return category; }
    public void setCategory(SupportCategory category) { this.category = category; }
    public SupportStatus getStatus() { return status; }
    public void setStatus(SupportStatus status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
