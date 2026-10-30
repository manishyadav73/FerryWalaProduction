package backend.concierge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentClassifierTest {

    private static void is(Intent expected, String text) {
        assertEquals(expected, IntentClassifier.classify(text), "text: " + text);
    }

    @Test
    void englishProblemsAreRecognised() {
        is(Intent.CANT_FIND, "I can't find the meeting point");
        is(Intent.CANT_FIND, "I am lost, wrong place");
        is(Intent.RUNNING_LATE, "I'm running late, sorry");
        is(Intent.NO_RESPONSE, "the vendor is not answering my calls");
        is(Intent.OTHER_PARTY_LATE, "vendor hasn't arrived and I'm still waiting");
        is(Intent.ARRIVED, "I have arrived at the spot");
        is(Intent.CANCEL_REFUND, "I want to cancel and get a refund");
        is(Intent.PAYMENT, "I paid by UPI but he says no");
        is(Intent.ORDER_MISMATCH, "I got the wrong item");
        is(Intent.ACCESSIBILITY, "I use a wheelchair, can he come closer?");
        is(Intent.APP_PROBLEM, "the app keeps crashing");
        is(Intent.HUMAN, "I want to talk to a real person");
        is(Intent.DIRECTIONS, "give me directions");
        is(Intent.ORDER_STATUS, "what is the status of my order");
        is(Intent.MEETING_HELP, "where is the meeting point");
        is(Intent.FIND_VENDOR, "find a vendor near me");
    }

    @Test
    void wholeWordMatchingAvoidsFalsePositives() {
        is(Intent.UNKNOWN, "I like chocolate");
        is(Intent.UNKNOWN, "translate this please");
        is(Intent.GREETING, "hello");
        is(Intent.GREETING, "Hi!");
    }

    @Test
    void hindiAndHinglishAreRecognised() {
        is(Intent.CANT_FIND, "मुझे जगह नहीं मिल रही");
        is(Intent.CANT_FIND, "mujhe jagah nahi mil rahi");
        is(Intent.RUNNING_LATE, "मुझे देर हो रही है");
        is(Intent.RUNNING_LATE, "main late hoon");
        is(Intent.NO_RESPONSE, "विक्रेता का जवाब नहीं आ रहा");
        is(Intent.CANCEL_REFUND, "मुझे ऑर्डर रद्द करना है");
        is(Intent.PAYMENT, "पेमेंट का सवाल है");
        is(Intent.HUMAN, "मुझे शिकायत करनी है");
        is(Intent.GREETING, "नमस्ते");
    }

    @Test
    void tamilIsRecognised() {
        is(Intent.RUNNING_LATE, "நான் தாமதமாக வருகிறேன்");
        is(Intent.CANT_FIND, "இடத்தை கண்டுபிடிக்க முடியவில்லை");
        is(Intent.CANCEL_REFUND, "கோரிக்கையை ரத்து செய்ய வேண்டும்");
        is(Intent.GREETING, "வணக்கம்");
    }

    @Test
    void emptyOrNullIsUnknown() {
        is(Intent.UNKNOWN, "");
        is(Intent.UNKNOWN, null);
        is(Intent.UNKNOWN, "   ?!  ");
    }

    @Test
    void humanRequestBeatsEverythingElse() {
        is(Intent.HUMAN, "I'm late and want a real person to help with the refund");
    }

    @Test
    void problemIntentsMapToSupportCategories() {
        assertEquals(backend.model.SupportCategory.MEETING_POINT, Intent.CANT_FIND.category());
        assertEquals(backend.model.SupportCategory.PAYMENT, Intent.PAYMENT.category());
        assertEquals(null, Intent.GREETING.category());
    }
}
