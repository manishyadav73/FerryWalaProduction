package backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "demand_signals", indexes = {
        @Index(name = "idx_signals_area_time", columnList = "area_key,created_at"),
        @Index(name = "idx_signals_geo_time", columnList = "latitude,longitude,created_at"),
        @Index(name = "idx_signals_customer_time", columnList = "customer_id,created_at")
})
public class DemandSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** Read-only mirror of the FK so we can read the id without loading the user. */
    @Column(name = "customer_id", insertable = false, updatable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignalType type;

    /** Normalised (lower-case, singular, Hinglish mapped) product key. */
    @Column(nullable = false, length = 80)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Category category;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "area_key", nullable = false, length = 20)
    private String areaKey;

    @Column(length = 80)
    private String areaName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Long getCustomerId() { return customerId; }
    public SignalType getType() { return type; }
    public void setType(SignalType type) { this.type = type; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAreaKey() { return areaKey; }
    public void setAreaKey(String areaKey) { this.areaKey = areaKey; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
