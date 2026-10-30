package backend.dto;

import backend.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(
            @NotNull(message = "Product is required.") Long productId,
            @NotNull(message = "Quantity is required.")
            @Min(value = 1, message = "Quantity must be at least 1.")
            @Max(value = 50, message = "Quantity is too high.")
            Integer quantity
    ) {
    }

    public record PlaceOrderRequest(
            @NotNull(message = "Vendor is required.") Long vendorId,

            @NotEmpty(message = "Add at least one product.")
            @Size(max = 30, message = "Too many items in one request.")
            List<@Valid OrderItemRequest> items,

            @Size(max = 300, message = "Note must be under 300 characters.")
            String note,

            @NotNull(message = "Choose where the vendor should meet you.")
            @DecimalMin(value = "-90", message = "Latitude is out of range.")
            @DecimalMax(value = "90", message = "Latitude is out of range.")
            Double latitude,

            @NotNull(message = "Choose where the vendor should meet you.")
            @DecimalMin(value = "-180", message = "Longitude is out of range.")
            @DecimalMax(value = "180", message = "Longitude is out of range.")
            Double longitude,

            @Size(max = 80, message = "Area must be under 80 characters.")
            String areaName
    ) {
    }

    public record OrderItemDto(
            Long productId,
            String name,
            String unit,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
    }

    public record OrderDto(
            Long id,
            OrderStatus status,
            BigDecimal totalAmount,
            String note,
            double latitude,
            double longitude,
            String areaName,
            Long vendorId,
            String vendorName,
            String vendorImageUrl,
            Long customerId,
            String customerName,
            List<OrderItemDto> items,
            boolean reviewed,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record UpdateOrderStatusRequest(@NotNull(message = "Status is required.") OrderStatus status) {
    }

    public record ReviewRequest(
            @NotNull(message = "Rating is required.")
            @Min(value = 1, message = "Rating must be 1-5.")
            @Max(value = 5, message = "Rating must be 1-5.")
            Integer rating,

            @Size(max = 500, message = "Comment must be under 500 characters.")
            String comment
    ) {
    }
}
