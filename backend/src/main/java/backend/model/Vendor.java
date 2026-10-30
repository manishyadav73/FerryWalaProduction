package backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "vendors", indexes = {
        @Index(name = "idx_vendors_location", columnList = "latitude,longitude"),
        @Index(name = "idx_vendors_category", columnList = "category")
})
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 80)
    private String shopName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.OTHER;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String address;

    /** Locality name shown to customers, e.g. "Green Park Colony". */
    @Column(length = 80)
    private String area;

    private Double latitude;

    private Double longitude;

    private Instant locationUpdatedAt;

    @Column(nullable = false)
    private boolean online = false;

    @Column(nullable = false)
    private boolean verified = false;

    /** Generated file name inside the upload dir; never a user-supplied name. */
    @Column(length = 80)
    private String imageFilename;

    @Column(nullable = false)
    private int ratingSum = 0;

    @Column(nullable = false)
    private int ratingCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public double getAverageRating() {
        return ratingCount == 0 ? 0.0 : Math.round((ratingSum * 10.0) / ratingCount) / 10.0;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(Instant locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }
    public int getRatingSum() { return ratingSum; }
    public void setRatingSum(int ratingSum) { this.ratingSum = ratingSum; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
