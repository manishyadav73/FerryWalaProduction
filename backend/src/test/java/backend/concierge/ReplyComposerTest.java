package backend.concierge;

import backend.concierge.ConciergeReply.Action;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplyComposerTest {

    private static ConciergeFacts customer(String status, boolean open, boolean mine, boolean theirs, Long dist) {
        return new ConciergeFacts("CUSTOMER", "Asha", 42L, status, "Ravi Chaat", "Ravi", "Green Park",
                open, mine, theirs, dist, null, null, 0, 0, null, null);
    }

    private static ConciergeFacts noOrder(String role) {
        return new ConciergeFacts(role, "Asha", null, null, null, null, null,
                false, false, false, null, null, null, 2, 3, null, null);
    }

    private static Set<String> kinds(ConciergeReply r) {
        return r.actions().stream().map(Action::kind).collect(Collectors.toSet());
    }

    @Test
    void statusRepliesUseOnlyKnownFactsAndAreMarkedVerified() {
        ConciergeReply r = ReplyComposer.compose(Intent.ORDER_STATUS, customer("ACCEPTED", true, false, false, null), "en");
        assertTrue(r.text().contains("#42"));
        assertTrue(r.text().contains("Ravi Chaat"));
        assertTrue(r.text().contains("accepted"));
        assertTrue(r.verified());
        assertEquals(1, r.cards().size());
    }

    @Test
    void withoutAnOrderTheConciergeSaysItCannotSeeOne() {
        ConciergeReply r = ReplyComposer.compose(Intent.CANT_FIND, noOrder("CUSTOMER"), "en");
        assertTrue(r.text().contains("can't see a request"));
        assertFalse(r.verified());
        assertFalse(kinds(r).contains("SEND_MEETING_UPDATE"));
    }

    @Test
    void vendorStatusWithoutOrderShowsCounts() {
        ConciergeReply r = ReplyComposer.compose(Intent.ORDER_STATUS, noOrder("VENDOR"), "en");
        assertTrue(r.text().contains("2 new and 3 open"));
    }

    @Test
    void cantFindNeverClaimsToKnowTheOtherPersonsLocation() {
        ConciergeReply r = ReplyComposer.compose(Intent.CANT_FIND, customer("ACCEPTED", true, true, false, null), "en");
        assertTrue(r.text().contains("can't verify where Ravi Chaat is"));
        assertFalse(r.text().contains(" m apart"));
        assertTrue(kinds(r).contains("CREATE_CASE"));
        assertTrue(kinds(r).contains("SEND_MEETING_UPDATE"));
    }

    @Test
    void distanceIsQuotedOnlyWhenBothSharedAndIsLabelledStraightLine() {
        ConciergeReply r = ReplyComposer.compose(Intent.CANT_FIND, customer("ON_THE_WAY", true, true, true, 240L), "en");
        assertTrue(r.text().contains("about 240 m apart in a straight line"));
        assertTrue(r.text().contains("not a walking route"));
    }

    @Test
    void meetingToolsStayClosedUntilTheVendorAccepts() {
        ConciergeReply r = ReplyComposer.compose(Intent.RUNNING_LATE, customer("PENDING", false, false, false, null), "en");
        assertTrue(r.text().contains("open once the vendor accepts"));
        assertTrue(r.actions().isEmpty());
    }

    @Test
    void runningLateOffersConfirmableUpdatesOnly() {
        ConciergeReply r = ReplyComposer.compose(Intent.RUNNING_LATE, customer("ACCEPTED", true, false, false, null), "en");
        assertEquals(3, r.actions().size());
        for (Action a : r.actions()) {
            assertEquals("SEND_MEETING_UPDATE", a.kind());
            assertEquals("RUNNING_LATE", a.payload().get("type"));
            assertEquals(42L, a.payload().get("orderId"));
        }
        assertTrue(r.text().contains("Nothing is sent until you confirm"));
    }

    @Test
    void otherPartyLateReportsOnlyWhatWasActuallySent() {
        ConciergeFacts none = customer("ACCEPTED", true, false, false, null);
        assertTrue(ReplyComposer.compose(Intent.OTHER_PARTY_LATE, none, "en").text().contains("hasn't sent a delay"));
        ConciergeFacts late = new ConciergeFacts("CUSTOMER", "Asha", 42L, "ACCEPTED", "Ravi Chaat", "Ravi", "Green Park",
                true, false, false, null, "RUNNING_LATE", 15, 0, 0, null, null);
        assertTrue(ReplyComposer.compose(Intent.OTHER_PARTY_LATE, late, "en").text().contains("about 15 min"));
    }

    @Test
    void paymentAnswerNeverPretendsToSeePayments() {
        ConciergeReply r = ReplyComposer.compose(Intent.PAYMENT, customer("ACCEPTED", true, false, false, null), "en");
        assertTrue(r.text().contains("does not take payments"));
        assertTrue(r.text().contains("can't see, confirm or refund"));
    }

    @Test
    void humanRequestOffersACaseAndShowsConfiguredContactOnly() {
        ConciergeFacts f = new ConciergeFacts("CUSTOMER", "Asha", null, null, null, null, null,
                false, false, false, null, null, null, 0, 0, "help@example.test", null);
        ConciergeReply r = ReplyComposer.compose(Intent.HUMAN, f, "en");
        assertTrue(r.text().contains("automated assistant"));
        assertTrue(r.text().contains("help@example.test"));
        assertEquals(Set.of("CREATE_CASE"), kinds(r));
        assertFalse(ReplyComposer.compose(Intent.HUMAN, noOrder("CUSTOMER"), "en").text().contains("@"));
    }

    @Test
    void draftIsOfferedAsAMessageTheUserSendsThemselves() {
        ConciergeReply r = ReplyComposer.compose(Intent.DRAFT_MESSAGE, customer("ACCEPTED", true, false, false, null), "en");
        assertTrue(r.text().contains("YOUR message"));
        Action a = r.actions().get(0);
        assertEquals("SEND_MESSAGE", a.kind());
        assertTrue(((String) a.payload().get("draft")).contains("request #42"));
    }

    @Test
    void repliesExistInHindiAndTamil() {
        ConciergeFacts f = customer("ACCEPTED", true, false, false, null);
        for (Intent i : Intent.values()) {
            for (String lang : new String[]{"hi", "ta"}) {
                ConciergeReply r = ReplyComposer.compose(i, f, lang);
                assertFalse(r.text().isBlank(), i + "/" + lang);
                assertFalse(r.text().matches(".*\\b(greet|status|meeting|noresp|cant)\\.[a-z]+.*"), "untranslated key in " + i + "/" + lang + ": " + r.text());
            }
        }
        assertTrue(ReplyComposer.compose(Intent.RUNNING_LATE, f, "hi").text().contains("देर"));
    }

    @Test
    void userSuppliedNamesCannotInjectPlaceholders() {
        ConciergeFacts f = new ConciergeFacts("CUSTOMER", "Asha", 7L, "ACCEPTED", "{1} {0}", "x", null,
                true, false, false, null, null, null, 0, 0, null, null);
        String t = ReplyComposer.compose(Intent.ORDER_STATUS, f, "en").text();
        assertTrue(t.contains("{1} {0}"));
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        assertEquals(ReplyComposer.compose(Intent.GREETING, noOrder("CUSTOMER"), "en").text(),
                ReplyComposer.compose(Intent.GREETING, noOrder("CUSTOMER"), "xx").text());
    }

    @Test
    void proposedActionsNeverIncludeAnythingThatChangesBookingsPricesOrRefunds() {
        Set<String> allowed = Set.of("SEND_MEETING_UPDATE", "SEND_MESSAGE", "CREATE_CASE", "OPEN_DIRECTIONS",
                "OPEN_MEETING", "OPEN_VENDORS", "START_SHARE");
        for (String role : new String[]{"CUSTOMER", "VENDOR"}) {
            for (Intent i : Intent.values()) {
                ConciergeFacts f = new ConciergeFacts(role, "A", 1L, "ACCEPTED", "Shop", "Cust", "Area",
                        true, false, false, null, null, null, 1, 1, null, null);
                for (Action a : ReplyComposer.compose(i, f, "en").actions()) {
                    assertTrue(allowed.contains(a.kind()), a.kind());
                }
            }
        }
    }
}
