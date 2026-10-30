# AI Concierge, meeting coordination and support

FerryWala is a hyperlocal vendor marketplace, so "meeting" means a customer and a street vendor meeting at the spot the customer pinned on a request. There are no in-app payments; the Concierge never moves money.

## What the Concierge does
- Answers from the caller's own requests: status, who accepted, meeting point, latest update from the other person, vendor's pending/open request counts.
- Understands English, Hinglish, Hindi and Tamil keywords (`IntentClassifier`) and replies in the chosen app language (`Messages`).
- Proposes actions as buttons: tell the other person you are late / arrived / cannot find the spot, send a drafted message (editable, sent as *your* message), open directions, open the meet page, start location sharing, open a support case.
- Payment, refund/cancellation and "talk to a person" questions are answered with fixed text and a route to a human case; no model wording is used for them.

## How it is kept safe
1. **Server decides.** Intent routing and the facts come from the server (`ConciergeContextService`, `OrderAccess`). The browser cannot choose a different user or request; a request the caller is not a party to returns the same 404 as a missing one.
2. **The model only rewords.** If `CONCIERGE_LLM_API_KEY` and `CONCIERGE_LLM_MODEL` are both set, the verified reply text may be reworded. The model gets no tools, no secrets, no email/phone/coordinates. Its output is discarded (rule text kept) if it contains links or markup, drops any number from the original, or the user message looked like prompt injection. Never used for payment/refund/human replies.
3. **Untrusted text is data.** Messages and notes are sanitised (control/zero-width/bidi characters, length caps) and wrapped as untrusted before any prompt (`PromptGuard`).
4. **Actions need the user.** The Concierge cannot change anything. Each proposed action opens a confirmation, then calls the same endpoint any user action would, with the user's own permissions.
5. **Limits and audit.** Per-user and per-IP rate limits; `audit_events` records chat, updates, shares and cases (no message content).
6. **Retention.** Chat history is deleted after `CONCIERGE_RETENTION_DAYS` (default 30) by `RetentionJob`, or immediately by the user (trash icon / `DELETE /api/concierge/history`).

## Location sharing
Optional, per request. The user confirms in a dialog, then the browser asks permission. Duration 15/30/60 minutes (server accepts 5-60). The page sends a refresh about every 15 s but a refresh never extends the expiry. Stop deletes the stored row; expired rows are purged by `RetentionJob`. The other party sees coordinates only while their share is active and the request is ACCEPTED/ON_THE_WAY. Distance is a straight line between the two shared points and is labelled as not a walking route. Declining or denying permission leaves updates, messages and directions to the pinned spot fully working.

## Configuration
See `.env.example` (`CONCIERGE_*`, `SUPPORT_*`). The built-in provider (`HttpLlmProvider`) calls the Anthropic Messages API. To use another provider implement `LlmProvider`.

## Known limitations
- Location refresh uses polling (about 15 s) while the page is open; there is no background tracking by design.
- Rule-based intent detection covers common phrasing only; unknown questions get a helpful fallback with a "talk to a person" option.
- Hindi/Tamil strings are machine-assisted and need native-speaker review.
- Support cases are stored and visible to admins; there is no email/SMS notification to staff yet.
- The model-reword path was not exercised against a live provider in this delivery.
