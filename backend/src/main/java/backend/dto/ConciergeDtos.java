package backend.dto;

import backend.concierge.ConciergeReply;
import backend.model.MeetingUpdateType;
import backend.model.OrderStatus;
import backend.model.SupportCategory;
import backend.model.SupportStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ConciergeDtos {

    private ConciergeDtos() {
    }

    // ---------------------------------------------------------------- chat

    public record ChatRequest(
            @NotBlank(message = "Type a message first.")
            @Size(max = 800, message = "Message must be under 800 characters.")
            String message,
            Long orderId,
            @Size(max = 5, message = "Unsupported language.")
            String language
    ) {
    }

    public record ChatResponse(ConciergeReply reply, String provider, Instant at) {
    }

    public record HistoryItem(Long id, String author, String text, Instant createdAt) {
    }

    public record ConciergeStatus(String name, boolean llmEnabled, String provider, int retentionDays,
                                  String supportEmail, String supportPhone) {
    }

    // ---------------------------------------------------------------- meeting

    public record MeetingUpdateRequest(
            @NotNull(message = "Choose what to tell the other person.") MeetingUpdateType type,
            Integer etaMinutes,
            @Size(max = 200, message = "Note must be under 200 characters.") String note,
            @Size(max = 12) String source
    ) {
    }

    public record MeetingUpdateDto(Long id, boolean mine, String senderName, MeetingUpdateType type,
                                   Integer etaMinutes, String note, boolean viaConcierge, Instant createdAt) {
    }

    public record MessageRequest(
            @NotBlank(message = "Type a message first.")
            @Size(max = 500, message = "Message must be under 500 characters.")
            String body
    ) {
    }

    public record MessageDto(Long id, boolean mine, String senderName, String body, Instant createdAt) {
    }

    public record ShareRequest(
            @NotNull(message = "Location is required.")
            @DecimalMin(value = "-90", message = "Latitude is out of range.")
            @DecimalMax(value = "90", message = "Latitude is out of range.")
            Double latitude,
            @NotNull(message = "Location is required.")
            @DecimalMin(value = "-180", message = "Longitude is out of range.")
            @DecimalMax(value = "180", message = "Longitude is out of range.")
            Double longitude,
            Integer durationMinutes
    ) {
    }

    public record MyShare(boolean active, Instant expiresAt) {
    }

    /** The other party's shared position. Present with coordinates only while THEY have an active share. */
    public record PeerShare(boolean active, Instant expiresAt, Double latitude, Double longitude, Instant updatedAt) {
    }

    public record MeetingDto(
            Long orderId,
            OrderStatus status,
            boolean coordinationOpen,
            boolean messagingOpen,
            String viewerRole,
            String otherName,
            String shopName,
            double latitude,
            double longitude,
            String areaName,
            String note,
            MyShare myShare,
            PeerShare otherShare,
            Long distanceMeters,
            List<MeetingUpdateDto> updates,
            List<MessageDto> messages
    ) {
    }

    // ---------------------------------------------------------------- support

    public record CreateCaseRequest(
            @NotNull(message = "Choose what the problem is about.") SupportCategory category,
            Long orderId,
            @NotBlank(message = "Describe the problem.")
            @Size(min = 5, max = 1000, message = "Describe the problem in 5 to 1000 characters.")
            String summary,
            @Size(max = 12) String source
    ) {
    }

    public record CaseDto(Long id, SupportCategory category, SupportStatus status, String summary, String source,
                          Long orderId, String resolutionNote, String creatorName, String creatorRole,
                          Instant createdAt, Instant updatedAt) {
    }

    public record UpdateCaseRequest(
            @NotNull(message = "Status is required.") SupportStatus status,
            @Size(max = 500, message = "Note must be under 500 characters.") String resolutionNote
    ) {
    }
}
