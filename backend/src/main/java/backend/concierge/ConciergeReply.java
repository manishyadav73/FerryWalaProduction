package backend.concierge;

import java.util.List;
import java.util.Map;

/**
 * What the Concierge answers with. {@code actions} are only proposals: the app shows them as
 * buttons and the user confirms; the click then goes through the normal, authorised API. The
 * Concierge itself never changes a booking, price, refund or setting.
 */
public record ConciergeReply(String text, List<Card> cards, List<Action> actions, List<String> suggestions,
                             boolean verified) {

    /** A block of facts read from live records. */
    public record Card(String type, String title, List<String> lines) {
    }

    /**
     * @param kind    SEND_MEETING_UPDATE, SEND_MESSAGE, CREATE_CASE, OPEN_DIRECTIONS, OPEN_MEETING, OPEN_VENDORS, START_SHARE
     * @param payload parameters for the normal API call (never secrets)
     */
    public record Action(String kind, String label, Map<String, Object> payload) {
    }

    public ConciergeReply withText(String newText) {
        return new ConciergeReply(newText, cards, actions, suggestions, verified);
    }
}
