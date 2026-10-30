package backend.concierge;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Keyword intent router for English, Hinglish, Hindi (Devanagari) and Tamil.
 *
 * Latin keywords match whole words (so "late" does not fire on "chocolate"); a trailing '*'
 * means "word starts with". Non-Latin keywords match as substrings because those scripts do
 * not separate suffixes with spaces. Order matters: the first group that matches wins.
 */
public final class IntentClassifier {

    private record Rule(Intent intent, List<String> keywords) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(Intent.HUMAN, List.of("human", "real person", "agent", "complaint", "escalate", "talk to someone",
                    "speak to someone", "support team", "insaan", "shikayat",
                    "इंसान", "शिकायत", "मनुष्य", "सपोर्ट", "மனிதர்", "புகார்", "ஆதரவு")),
            new Rule(Intent.ACCESSIBILITY, List.of("wheelchair", "disab*", "elderly", "accessib*", "special assistance",
                    "special help", "blind", "व्हीलचेयर", "विकलांग", "दिव्यांग", "बुजुर्ग", "சக்கர நாற்காலி", "மாற்றுத்திறன்", "முதியவர்")),
            new Rule(Intent.CANT_FIND, List.of("cant find", "cannot find", "can not find", "unable to find", "not able to find",
                    "wrong place", "wrong location", "i am lost", "im lost", "lost", "nahi mil", "nahin mil", "nahi mila",
                    "नहीं मिल", "नही मिल", "रास्ता भूल", "ढूंढ", "கண்டுபிடிக்க", "கிடைக்கவில்லை", "வழி தவறி")),
            new Rule(Intent.NO_RESPONSE, List.of("not responding", "no response", "not answering", "doesnt reply", "dont reply",
                    "not replying", "no reply", "not picking", "phone off", "unreachable", "jawab nahi", "reply nahi",
                    "जवाब नहीं", "फोन नहीं उठ", "फ़ोन नहीं उठ", "रिप्लाई नहीं", "பதில் இல்லை", "பதிலளிக்கவில்லை", "எடுக்கவில்லை")),
            new Rule(Intent.OTHER_PARTY_LATE, List.of("hasnt arrived", "has not arrived", "not arrived", "not come", "hasnt come",
                    "still waiting", "vendor is late", "customer is late", "seller is late", "he is late", "she is late",
                    "abhi tak nahi aaya", "abhi tak nahi aaye", "अभी तक नहीं आ", "इंतज़ार कर", "इंतजार कर", "இன்னும் வரவில்லை", "காத்திருக்கிறேன்")),
            new Rule(Intent.RUNNING_LATE, List.of("running late", "be late", "am late", "im late", "late", "delayed", "delay",
                    "late hoon", "late ho", "der ho", "देर", "लेट", "தாமத", "நேரமாகும்")),
            new Rule(Intent.ARRIVED, List.of("i have arrived", "ive arrived", "i arrived", "im here", "i am here", "reached",
                    "pahunch", "पहुँच", "पहुंच", "पहुँच गया", "வந்துவிட்டேன்", "வந்துள்ளேன்", "வந்தேன்")),
            new Rule(Intent.ORDER_MISMATCH, List.of("wrong item", "not what i ordered", "missing item", "wrong quantity",
                    "different item", "galat saman", "गलत", "कम सामान", "தவறான", "குறைவாக")),
            new Rule(Intent.CANCEL_REFUND, List.of("cancel*", "refund*", "money back", "return my money", "रद्द", "कैंसल", "रिफंड",
                    "पैसे वापस", "ரத்து", "பணம் திரும்ப", "ரீஃபண்ட்")),
            new Rule(Intent.PAYMENT, List.of("payment*", "paid", "pay", "upi", "charged", "overcharged", "paisa", "paise",
                    "पेमेंट", "भुगतान", "पैसे", "पैसा", "கட்டண", "பணம்", "பணத்தை")),
            new Rule(Intent.APP_PROBLEM, List.of("bug", "crash*", "not working", "error", "glitch", "stuck", "app problem",
                    "kaam nahi", "काम नहीं", "एरर", "ऐप", "பிழை", "வேலை செய்யவில்லை")),
            new Rule(Intent.DIRECTIONS, List.of("direction*", "navigate", "navigation", "route", "how to reach", "how do i get",
                    "how to get there", "map", "rasta", "रास्ता", "दिशा", "नक्शा", "வழி", "திசை", "வரைபடம்")),
            new Rule(Intent.DRAFT_MESSAGE, List.of("draft", "write a message", "write message", "what should i say",
                    "message to", "tell the customer", "tell the vendor", "tell the seller", "संदेश लिख", "मैसेज लिख", "மெசேஜ்", "செய்தி எழுது")),
            new Rule(Intent.MEETING_HELP, List.of("meeting", "meet", "pickup", "pick up", "meeting point", "milna", "milne",
                    "मिलना", "मिलने", "मीटिंग", "சந்திப்பு", "சந்தி")),
            new Rule(Intent.ORDER_STATUS, List.of("status", "my order*", "my request*", "where is my", "track*", "new request*",
                    "pending", "स्थिति", "ऑर्डर", "अनुरोध", "நிலை", "ஆர்டர்", "கோரிக்கை")),
            new Rule(Intent.FIND_VENDOR, List.of("find vendor", "find a vendor", "nearby", "near me", "who sells", "vendor near",
                    "पास", "आसपास", "आस-पास", "அருகில்", "அருகிலுள்ள"))
    );

    private static final List<String> GREETINGS = List.of("hi", "hello", "hey", "namaste", "namaskar", "good morning",
            "good evening", "नमस्ते", "हैलो", "हेलो", "வணக்கம்");

    private IntentClassifier() {
    }

    public static Intent classify(String raw) {
        String text = normalize(raw);
        if (text.isEmpty()) {
            return Intent.UNKNOWN;
        }
        String padded = " " + text + " ";
        for (Rule rule : RULES) {
            for (String kw : rule.keywords()) {
                if (matches(text, padded, kw)) {
                    return rule.intent();
                }
            }
        }
        if (text.split(" ").length <= 4) {
            for (String g : GREETINGS) {
                if (matches(text, padded, g)) {
                    return Intent.GREETING;
                }
            }
        }
        return Intent.UNKNOWN;
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        s = s.replace("'", "").replace("’", "");
        StringBuilder out = new StringBuilder(s.length());
        boolean space = true;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            boolean keep = Character.isLetterOrDigit(cp) || Character.getType(cp) == Character.NON_SPACING_MARK
                    || Character.getType(cp) == Character.COMBINING_SPACING_MARK;
            if (keep) {
                out.appendCodePoint(cp);
                space = false;
            } else if (!space) {
                out.append(' ');
                space = true;
            }
        }
        return out.toString().trim();
    }

    private static boolean matches(String text, String padded, String kw) {
        if (isLatin(kw)) {
            if (kw.endsWith("*")) {
                return padded.contains(" " + kw.substring(0, kw.length() - 1));
            }
            return padded.contains(" " + kw + " ");
        }
        return text.contains(kw);
    }

    private static boolean isLatin(String kw) {
        for (int i = 0; i < kw.length(); i++) {
            char c = kw.charAt(i);
            if (c > 127) {
                return false;
            }
        }
        return true;
    }
}
