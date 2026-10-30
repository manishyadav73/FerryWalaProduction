package backend.concierge;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Treats everything a user (or the other party) typed as untrusted data. Used before text goes
 * into a language-model prompt or the chat history. It cannot make an injection harmless on its
 * own; the real protection is that the model never chooses tools and its output is only shown
 * as text (see ConciergeService).
 */
public final class PromptGuard {

    private static final List<Pattern> INJECTION = List.of(
            Pattern.compile("ignore (all |any |the )?(previous|prior|above|earlier)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard (all |any |the )?(previous|prior|above|instructions|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(reveal|show|print|repeat|leak).{0,30}(system prompt|instructions|api key|secret|password|token)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bact as\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("developer mode|jailbreak|do anything now|\\bDAN\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?\\s*(system|assistant|untrusted|tool|function)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|\\n)\\s*(system|assistant)\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("tool_call|function_call|\"tool\"\\s*:", Pattern.CASE_INSENSITIVE)
    );

    private PromptGuard() {
    }

    /** Removes control, zero-width and bidi-override characters, collapses whitespace, caps length. */
    public static String sanitize(String s, int max) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.min(s.length(), max + 8));
        boolean space = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean invisible = (c >= '​' && c <= '‏') || (c >= '‪' && c <= '‮')
                    || (c >= '⁠' && c <= '⁩') || c == '﻿';
            if (invisible) {
                continue;
            }
            if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                if (!space) {
                    out.append(' ');
                    space = true;
                }
                continue;
            }
            out.append(c);
            space = false;
        }
        String r = out.toString().trim();
        return r.length() > max ? r.substring(0, max).trim() : r;
    }

    public static boolean looksLikeInjection(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (Pattern p : INJECTION) {
            if (p.matcher(s).find()) {
                return true;
            }
        }
        return false;
    }

    /** Wraps user-written text so it can sit inside a prompt without being able to close the wrapper. */
    public static String wrapUntrusted(String label, String text) {
        String safe = sanitize(text, 800).replace('<', '‹').replace('>', '›');
        return "<untrusted source=\"" + label.replaceAll("[^a-z_]", "") + "\">" + safe + "</untrusted>";
    }

    /**
     * Model output is shown as plain text only. Reject anything carrying links or markup so a
     * manipulated model cannot send users to another site or fake UI.
     */
    public static String cleanModelOutput(String s, int max) {
        String t = sanitize(s, max);
        if (t.isEmpty() || t.contains("http://") || t.contains("https://") || t.contains("www.")
                || t.indexOf('<') >= 0 || t.indexOf('>') >= 0 || t.contains("```") || t.contains("](")) {
            return null;
        }
        return t;
    }

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    /** A rewrite may change the wording but must keep every number (request ids, minutes, distances). */
    public static boolean preservesNumbers(String original, String rewritten) {
        if (original == null || rewritten == null) {
            return false;
        }
        java.util.regex.Matcher m = NUMBER.matcher(original);
        while (m.find()) {
            if (!rewritten.contains(m.group())) {
                return false;
            }
        }
        return true;
    }
}
