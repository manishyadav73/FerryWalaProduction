package backend.model;

public enum SupportStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS;
    }
}
