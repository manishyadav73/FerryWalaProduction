package backend.dto;

import backend.model.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class VendorDtos {

    private VendorDtos() {
    }

    public record VendorSummary(
            Long id,
            String shopName,
            Category category,
            String description,
            String area,
            String address,
            Double latitude,
            Double longitude,
            Double distanceKm,
            boolean online,
            boolean verified,
            double rating,
            int ratingCount,
            String imageUrl,
            Instant locationUpdatedAt
    ) {
    }

    public record VendorDetail(VendorSummary vendor, List<ProductDtos.ProductDto> products) {
    }

    public record UpdateVendorRequest(
            @NotBlank(message = "Shop name is required.")
            @Size(min = 2, max = 80, message = "Shop name must be 2-80 characters.")
            String shopName,

            @NotNull(message = "Choose a category.")
            Category category,

            @Size(max = 500, message = "Description must be under 500 characters.")
            String description,

            @Size(max = 200, message = "Address must be under 200 characters.")
            String address,

            @Size(max = 80, message = "Area must be under 80 characters.")
            String area
    ) {
    }

    public record StatusRequest(@NotNull(message = "online is required.") Boolean online) {
    }

    public record LocationRequest(
            @NotNull(message = "Latitude is required.")
            @DecimalMin(value = "-90", message = "Latitude is out of range.")
            @DecimalMax(value = "90", message = "Latitude is out of range.")
            Double latitude,

            @NotNull(message = "Longitude is required.")
            @DecimalMin(value = "-180", message = "Longitude is out of range.")
            @DecimalMax(value = "180", message = "Longitude is out of range.")
            Double longitude,

            @Size(max = 80, message = "Area must be under 80 characters.")
            String area,

            @Size(max = 200, message = "Address must be under 200 characters.")
            String address
    ) {
    }

    public record ReviewDto(Long id, int rating, String comment, String customerName, Instant createdAt) {
    }
}
