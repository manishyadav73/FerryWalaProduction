package backend.concierge;

import backend.model.SupportCategory;

/** What the user is asking the Concierge for. Decided by rules on the server, never by the model. */
public enum Intent {
    GREETING(null),
    ORDER_STATUS(null),
    MEETING_HELP(SupportCategory.MEETING_POINT),
    RUNNING_LATE(SupportCategory.RUNNING_LATE),
    OTHER_PARTY_LATE(SupportCategory.SERVICE_DELAY),
    CANT_FIND(SupportCategory.MEETING_POINT),
    NO_RESPONSE(SupportCategory.NO_RESPONSE),
    ARRIVED(null),
    DIRECTIONS(null),
    CANCEL_REFUND(SupportCategory.CANCELLATION_REFUND),
    PAYMENT(SupportCategory.PAYMENT),
    ORDER_MISMATCH(SupportCategory.ORDER_MISMATCH),
    ACCESSIBILITY(SupportCategory.ACCESSIBILITY),
    APP_PROBLEM(SupportCategory.APP_PROBLEM),
    DRAFT_MESSAGE(null),
    FIND_VENDOR(null),
    HUMAN(SupportCategory.OTHER),
    UNKNOWN(null);

    private final SupportCategory category;

    Intent(SupportCategory category) {
        this.category = category;
    }

    /** The support category a case about this intent would use, or null if the intent is not a problem. */
    public SupportCategory category() {
        return category;
    }
}
