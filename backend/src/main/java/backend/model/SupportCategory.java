package backend.model;

/** What a support case is about. Mirrors the problem types the AI Concierge can recognise. */
public enum SupportCategory {
    MEETING_POINT,
    RUNNING_LATE,
    NO_RESPONSE,
    ORDER_MISMATCH,
    PAYMENT,
    CANCELLATION_REFUND,
    SERVICE_DELAY,
    ACCESSIBILITY,
    APP_PROBLEM,
    OTHER
}
