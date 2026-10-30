package backend.dto;

import backend.demand.DemandEngine;
import backend.model.SignalType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class DemandDtos {

    private DemandDtos() {
    }

    public record SignalRequest(
            @NotNull(message = "Signal type is required.") SignalType type,

            @NotBlank(message = "Tell us which product you need.")
            @Size(min = 2, max = 40, message = "Product name must be 2-40 characters.")
            String productName,

            @NotNull(message = "Latitude is required.")
            @DecimalMin(value = "-90", message = "Latitude is out of range.")
            @DecimalMax(value = "90", message = "Latitude is out of range.")
            Double latitude,

            @NotNull(message = "Longitude is required.")
            @DecimalMin(value = "-180", message = "Longitude is out of range.")
            @DecimalMax(value = "180", message = "Longitude is out of range.")
            Double longitude,

            @Size(max = 80, message = "Area must be under 80 characters.")
            String areaName
    ) {
    }

    public record DataQuality(int signalsConsidered, int windowDays, String level, String note) {
    }

    public record OpportunityResponse(
            Instant generatedAt,
            String disclaimer,
            Double vendorLatitude,
            Double vendorLongitude,
            boolean vendorOnline,
            long pendingOrders,
            DataQuality dataQuality,
            DemandEngine.Opportunity best,
            List<DemandEngine.Opportunity> others
    ) {
    }
}
