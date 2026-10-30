package backend.concierge;

import java.util.Optional;

/**
 * Optional language-model backend. It may only REWRITE a reply the server already composed from
 * verified facts. It is never given tools, never chooses actions and its text is shown as plain text.
 */
public interface LlmProvider {

    boolean enabled();

    /** Empty on any failure or timeout; the caller then keeps the rule-based reply. */
    Optional<String> rewrite(String systemPrompt, String userPrompt);
}
