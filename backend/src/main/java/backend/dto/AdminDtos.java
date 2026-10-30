package backend.dto;

import backend.model.Category;
import backend.model.Role;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record PageDto<T>(List<T> items, int page, int size, long total) {
    }

    public record Stats(
            long users,
            long customers,
            long vendors,
            long verifiedVendors,
            long onlineVendors,
            long products,
            long orders,
            Map<String, Long> ordersByStatus,
            BigDecimal completedRevenue,
            long demandSignalsLast7Days,
            long reviews
    ) {
    }

    public record AdminUserDto(Long id, String name, String username, String email, Role role,
                               boolean enabled, boolean emailVerified, Instant createdAt) {
    }

    public record AdminVendorDto(Long id, String shopName, Category category, String ownerName, String ownerEmail,
                                 boolean verified, boolean online, boolean enabled, double rating, int ratingCount,
                                 String area, Instant createdAt) {
    }

    public record EnabledRequest(@NotNull(message = "enabled is required.") Boolean enabled) {
    }

    public record VerifiedRequest(@NotNull(message = "verified is required.") Boolean verified) {
    }

    public record SuspiciousFlag(String kind, String severity, String subject, String detail) {
    }
}
