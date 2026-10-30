package backend.concierge;

import backend.concierge.ConciergeReply.Action;
import backend.concierge.ConciergeReply.Card;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the reply for an intent from verified facts only. Every statement about a booking comes
 * from ConciergeFacts; where a fact is unknown the text says so instead of guessing.
 */
public final class ReplyComposer {

    private ReplyComposer() {
    }

    public static ConciergeReply compose(Intent intent, ConciergeFacts f, String langCode) {
        String lang = Messages.lang(langCode);
        String other = otherName(f);
        List<Card> cards = new ArrayList<>();
        List<Action> actions = new ArrayList<>();
        String text;
        boolean verified = false;

        switch (intent) {
            case GREETING -> text = Messages.t(lang, f.isVendor() ? "greet.vendor" : "greet.customer", first(f));
            case ORDER_STATUS -> {
                if (f.isVendor() && !f.hasOrder()) {
                    text = Messages.t(lang, "vendor.counts", f.pendingRequests(), f.openRequests());
                    verified = true;
                } else if (!f.hasOrder()) {
                    text = Messages.t(lang, "noOrder");
                } else {
                    text = Messages.t(lang, "status.line", f.orderId(), other, statusWord(lang, f.orderStatus()));
                    if (f.isVendor()) {
                        text += " " + Messages.t(lang, "vendor.counts", f.pendingRequests(), f.openRequests());
                    }
                    cards.add(orderCard(lang, f, other));
                    actions.add(openMeeting(lang, f));
                    verified = true;
                }
            }
            case MEETING_HELP -> {
                if (!f.hasOrder()) {
                    text = Messages.t(lang, "noOrder");
                } else if (!f.coordinationOpen()) {
                    text = Messages.t(lang, "meeting.closed", statusWord(lang, f.orderStatus()));
                    cards.add(orderCard(lang, f, other));
                    verified = true;
                } else {
                    text = Messages.t(lang, "meeting.info", f.orderId(), area(lang, f)) + " "
                            + Messages.t(lang, "meeting.help", other);
                    cards.add(meetingCard(lang, f, other));
                    actions.add(openMeeting(lang, f));
                    actions.add(directions(lang, f));
                    actions.add(startShare(lang, f));
                    verified = true;
                }
            }
            case RUNNING_LATE -> {
                if (!usable(f)) {
                    text = noMeetingTools(lang, f);
                } else {
                    text = Messages.t(lang, "late.ask", other);
                    for (int minutes : new int[]{10, 20, 30}) {
                        actions.add(meetingUpdate(lang, f, "RUNNING_LATE", minutes,
                                Messages.t(lang, "act.late", other, minutes)));
                    }
                }
            }
            case ARRIVED -> {
                if (!usable(f)) {
                    text = noMeetingTools(lang, f);
                } else {
                    text = Messages.t(lang, "arrived.ask", other);
                    actions.add(meetingUpdate(lang, f, "ARRIVED", null, Messages.t(lang, "act.arrived", other)));
                }
            }
            case CANT_FIND -> {
                if (!usable(f)) {
                    text = noMeetingTools(lang, f);
                } else {
                    String proof = f.distanceMeters() != null
                            ? Messages.t(lang, "cant.dist", f.distanceMeters())
                            : Messages.t(lang, "cant.noshare", other);
                    text = Messages.t(lang, "cant.find", other) + " " + proof;
                    cards.add(meetingCard(lang, f, other));
                    actions.add(directions(lang, f));
                    if (!f.myShareActive()) {
                        actions.add(startShare(lang, f));
                    }
                    actions.add(meetingUpdate(lang, f, "CANT_FIND", null, Messages.t(lang, "act.cant", other)));
                    actions.add(caseAction(lang, f, Intent.CANT_FIND));
                    verified = true;
                }
            }
            case NO_RESPONSE -> {
                text = Messages.t(lang, "noresp", other);
                if (usable(f)) {
                    actions.add(openMeeting(lang, f));
                }
                actions.add(caseAction(lang, f, Intent.NO_RESPONSE));
            }
            case OTHER_PARTY_LATE -> {
                if (!f.hasOrder()) {
                    text = Messages.t(lang, "noOrder");
                } else if ("RUNNING_LATE".equals(f.otherLastUpdateType()) && f.otherLastUpdateEta() != null) {
                    text = Messages.t(lang, "otherLate.reported", other, f.otherLastUpdateEta())
                            + " " + Messages.t(lang, "otherLate.next");
                    verified = true;
                } else {
                    text = Messages.t(lang, "otherLate.none", other) + " " + Messages.t(lang, "otherLate.next");
                    verified = true;
                }
                if (f.hasOrder()) {
                    actions.add(openMeeting(lang, f));
                    actions.add(caseAction(lang, f, Intent.OTHER_PARTY_LATE));
                }
            }
            case DIRECTIONS -> {
                if (!f.hasOrder()) {
                    text = Messages.t(lang, "noOrder");
                } else {
                    text = Messages.t(lang, "directions");
                    actions.add(directions(lang, f));
                }
            }
            case CANCEL_REFUND -> {
                text = Messages.t(lang, f.isVendor() ? "cancel.vendor" : "cancel.customer");
                actions.add(caseAction(lang, f, Intent.CANCEL_REFUND));
            }
            case PAYMENT -> {
                text = Messages.t(lang, "payment");
                actions.add(caseAction(lang, f, Intent.PAYMENT));
            }
            case ORDER_MISMATCH -> {
                text = Messages.t(lang, "mismatch", other);
                actions.add(caseAction(lang, f, Intent.ORDER_MISMATCH));
            }
            case ACCESSIBILITY -> {
                text = Messages.t(lang, "access", other);
                if (f.hasOrder() && MessagingOpen.is(f)) {
                    actions.add(messageDraft(lang, f, other, "draft.access"));
                }
                actions.add(caseAction(lang, f, Intent.ACCESSIBILITY));
            }
            case APP_PROBLEM -> {
                text = Messages.t(lang, "app");
                actions.add(caseAction(lang, f, Intent.APP_PROBLEM));
            }
            case HUMAN -> {
                text = Messages.t(lang, "human");
                if (f.supportEmail() != null || f.supportPhone() != null) {
                    String contact = f.supportEmail() != null && f.supportPhone() != null
                            ? f.supportEmail() + " / " + f.supportPhone()
                            : (f.supportEmail() != null ? f.supportEmail() : f.supportPhone());
                    text += " " + Messages.t(lang, "human.contact", contact);
                }
                actions.add(caseAction(lang, f, Intent.HUMAN));
            }
            case DRAFT_MESSAGE -> {
                if (!f.hasOrder()) {
                    text = Messages.t(lang, "noDraftOrder");
                } else {
                    text = Messages.t(lang, "draft");
                    actions.add(messageDraft(lang, f, other, f.isVendor() ? "draft.vendor" : "draft.customer"));
                }
            }
            case FIND_VENDOR -> {
                text = Messages.t(lang, f.isVendor() ? "findVendor.vendor" : "findVendor.customer");
                if (!f.isVendor()) {
                    actions.add(new Action("OPEN_VENDORS", Messages.t(lang, "act.vendors"), Map.of()));
                }
            }
            default -> text = Messages.t(lang, "unknown");
        }

        if (f.supportEmail() != null && intent == Intent.CANT_FIND) {
            // Plain contact route that does not depend on the other party or on location.
            text += " " + Messages.t(lang, "human.contact", f.supportPhone() != null ? f.supportPhone() : f.supportEmail());
        }
        return new ConciergeReply(text, cards, actions, suggestions(f), verified);
    }

    // ------------------------------------------------------------------ helpers

    /** Local holder so ACCESSIBILITY can ask "can the user message the other party right now". */
    private static final class MessagingOpen {
        static boolean is(ConciergeFacts f) {
            return f.orderStatus() != null && !(f.orderStatus().equals("COMPLETED")
                    || f.orderStatus().equals("REJECTED") || f.orderStatus().equals("CANCELLED"));
        }
    }

    private static boolean usable(ConciergeFacts f) {
        return f.hasOrder() && f.coordinationOpen();
    }

    private static String noMeetingTools(String lang, ConciergeFacts f) {
        return f.hasOrder()
                ? Messages.t(lang, "meeting.closed", statusWord(lang, f.orderStatus()))
                : Messages.t(lang, "noOrder");
    }

    private static String first(ConciergeFacts f) {
        return f.firstName() == null || f.firstName().isBlank() ? "" : f.firstName();
    }

    private static String otherName(ConciergeFacts f) {
        if (f.isVendor()) {
            return f.otherFirstName() == null ? "the customer" : f.otherFirstName();
        }
        return f.shopName() == null ? "the vendor" : f.shopName();
    }

    private static String area(String lang, ConciergeFacts f) {
        return f.meetingArea() == null || f.meetingArea().isBlank()
                ? Messages.t(lang, "meeting.areaUnknown") : f.meetingArea();
    }

    private static String statusWord(String lang, String status) {
        String key = "st." + status;
        return status != null && Messages.has(key) ? Messages.t(lang, key) : String.valueOf(status);
    }

    private static Card orderCard(String lang, ConciergeFacts f, String other) {
        return new Card("ORDER", Messages.t(lang, "card.order", f.orderId()), List.of(
                Messages.t(lang, "card.status", statusWord(lang, f.orderStatus())),
                Messages.t(lang, "card.with", other)));
    }

    private static Card meetingCard(String lang, ConciergeFacts f, String other) {
        List<String> lines = new ArrayList<>();
        lines.add(Messages.t(lang, "card.point", area(lang, f)));
        lines.add(Messages.t(lang, "card.share",
                Messages.t(lang, f.myShareActive() ? "card.on" : "card.off"), other,
                Messages.t(lang, f.otherShareActive() ? "card.on" : "card.off")));
        if (f.distanceMeters() != null) {
            lines.add(Messages.t(lang, "card.dist", f.distanceMeters()));
        }
        return new Card("MEETING", Messages.t(lang, "card.meeting"), lines);
    }

    private static Action openMeeting(String lang, ConciergeFacts f) {
        return new Action("OPEN_MEETING", Messages.t(lang, "act.meeting"), orderPayload(f));
    }

    private static Action directions(String lang, ConciergeFacts f) {
        return new Action("OPEN_DIRECTIONS", Messages.t(lang, "act.directions"), orderPayload(f));
    }

    private static Action startShare(String lang, ConciergeFacts f) {
        Map<String, Object> p = orderPayload(f);
        p.put("durationMinutes", MeetingPolicy.DEFAULT_SHARE_MINUTES);
        return new Action("START_SHARE", Messages.t(lang, "act.share"), p);
    }

    private static Action meetingUpdate(String lang, ConciergeFacts f, String type, Integer eta, String label) {
        Map<String, Object> p = orderPayload(f);
        p.put("type", type);
        if (eta != null) {
            p.put("etaMinutes", eta);
        }
        return new Action("SEND_MEETING_UPDATE", label, p);
    }

    private static Action messageDraft(String lang, ConciergeFacts f, String other, String key) {
        Map<String, Object> p = orderPayload(f);
        p.put("draft", Messages.t(lang, key, other, first(f).isEmpty() ? "-" : first(f), f.orderId()));
        return new Action("SEND_MESSAGE", Messages.t(lang, "act.message", other), p);
    }

    /** Proposes a support case. The summary is only a starting point; the user edits it before sending. */
    private static Action caseAction(String lang, ConciergeFacts f, Intent intent) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (f.hasOrder()) {
            p.put("orderId", f.orderId());
        }
        p.put("category", intent.category() == null ? "OTHER" : intent.category().name());
        p.put("source", "CONCIERGE");
        return new Action("CREATE_CASE", Messages.t(lang, "act.case"), p);
    }

    private static Map<String, Object> orderPayload(ConciergeFacts f) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (f.hasOrder()) {
            p.put("orderId", f.orderId());
        }
        return p;
    }

    private static List<String> suggestions(ConciergeFacts f) {
        return f.isVendor()
                ? List.of("requests", "meeting", "late", "cant_find", "human")
                : List.of("status", "meeting", "late", "cant_find", "cancel", "human");
    }
}
