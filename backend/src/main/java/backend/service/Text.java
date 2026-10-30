package backend.service;

import backend.concierge.PromptGuard;

/** Small text helpers shared by the meeting, messaging and support services. */
final class Text {

    private Text() {
    }

    /** Null when blank. Control and invisible characters removed, whitespace collapsed, length capped. */
    static String clean(String s, int max) {
        String c = PromptGuard.sanitize(s, max);
        return c.isEmpty() ? null : c;
    }

    static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "Someone";
        }
        String n = name.trim();
        int space = n.indexOf(' ');
        return space > 0 ? n.substring(0, space) : n;
    }
}
