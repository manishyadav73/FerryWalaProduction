package backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/** One line of a user's Concierge conversation. Deleted on request and after the retention period. */
@Entity
@Table(name = "concierge_messages", indexes = {
        @Index(name = "idx_concierge_messages_user", columnList = "user_id,created_at"),
        @Index(name = "idx_concierge_messages_created", columnList = "created_at")
})
public class ConciergeMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** USER or ASSISTANT. */
    @Column(nullable = false, length = 10)
    private String author;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
