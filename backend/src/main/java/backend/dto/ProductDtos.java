package backend.dto;

import backend.model.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductDto(
            Long id,
            Long vendorId,
            String vendorName,
            String name,
            Category category,
            BigDecimal price,
            String unit,
            int stock,
            boolean available,
            String imageUrl
    ) {
    }

    public record ProductRequest(
            @NotBlank(message = "Product name is required.")
            @Size(max = 80, message = "Product name must be under 80 characters.")
            String name,

            @NotNull(message = "Choose a category.")
            Category category,

            @NotNull(message = "Price is required.")
            @DecimalMin(value = "0.01", message = "Price must be greater than 0.")
            @DecimalMax(value = "100000", message = "Price is too high.")
            BigDecimal price,

            @NotBlank(message = "Unit is required (for example kg or dozen).")
            @Size(max = 20, message = "Unit must be under 20 characters.")
            String unit,

            @NotNull(message = "Stock is required.")
            @Min(value = 0, message = "Stock cannot be negative.")
            @Max(value = 100000, message = "Stock is too high.")
            Integer stock,

            Boolean available,

            @Size(max = 500, message = "Image link is too long.")
            @Pattern(regexp = "^$|^https://\\S+$", message = "Image link must start with https://")
            String imageUrl
    ) {
    }

    public record AvailabilityRequest(@NotNull(message = "available is required.") Boolean available) {
    }
}
