package backend.model;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    ON_THE_WAY,
    COMPLETED,
    REJECTED,
    CANCELLED;

    public boolean isOpen() {
        return this == PENDING || this == ACCEPTED || this == ON_THE_WAY;
    }
}
