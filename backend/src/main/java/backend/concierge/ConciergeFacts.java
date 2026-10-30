package backend.concierge;

/**
 * The only live data a reply may be built from. Assembled on the server from records the caller is
 * allowed to see. Deliberately has no email, phone number or coordinates, so nothing sensitive can
 * reach a language-model prompt through it.
 */
public record ConciergeFacts(
        String role,              // "CUSTOMER" or "VENDOR"
        String firstName,
        Long orderId,             // null when no request is in context
        String orderStatus,       // OrderStatus name or null
        String shopName,
        String otherFirstName,
        String meetingArea,       // locality label the customer chose, may be null
        boolean coordinationOpen, // vendor accepted and request still running
        boolean myShareActive,
        boolean otherShareActive,
        Long distanceMeters,      // only when both are sharing
        String otherLastUpdateType, // latest meeting update from the other party, or null
        Integer otherLastUpdateEta,
        int pendingRequests,      // vendor only
        int openRequests,         // vendor only
        String supportEmail,      // configured contact, or null
        String supportPhone) {

    public boolean isVendor() {
        return "VENDOR".equals(role);
    }

    public boolean hasOrder() {
        return orderId != null;
    }
}
