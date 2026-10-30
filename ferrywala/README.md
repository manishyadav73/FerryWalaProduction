# FerryWala

Helping local vendors find where customers are. Customers find fresh vendors nearby and send requests; vendors open the app and see **today's best opportunity**: which area to go to, what to carry and when, based on explainable demand signals.

* `frontend/` React 19 + Vite, Leaflet/OpenStreetMap, STOMP for live updates
* `backend/backend/` Spring Boot 3.5, Java 17, JPA, MySQL (H2 for dev), JWT
* `docs/API.md` endpoint reference and the demand-scoring rules
* `docs/UI_UX_AUDIT.md` page-by-page animation / UI audit and what was verified
* `docs/AI_CONCIERGE.md` how the AI Concierge, meeting coordination and support cases work and what protects them
* `backend/backend/db/migrations/` reference SQL for the Concierge/meeting/support tables (optional; the app creates them by default)

## Run locally (no MySQL needed)

```bash
# terminal 1: backend with in-memory H2 and a dev-only JWT secret
cd backend/backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# terminal 2: frontend (proxies /api and /ws to localhost:8080)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Register as a customer or vendor. For an admin, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` before starting the backend; it is created once if missing.

## Deploy with Docker

```bash
cp .env.example .env      # fill in DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET (openssl rand -base64 48)
docker compose up --build # web on http://localhost:8081
```

nginx serves the app and proxies `/api` and `/ws` to the backend, so there is one origin and no CORS to configure. Put it behind HTTPS (Caddy, Cloudflare, your host's load balancer) and set `APP_PUBLIC_URL` to the https address. Uploaded photos live in the `uploads` volume; back it up with the database.

### Configuration (all via environment, nothing is hard-coded)

| Variable | Required | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | yes | MySQL connection (compose fills `DB_URL`) |
| `JWT_SECRET` | yes | 32+ random characters |
| `CORS_ALLOWED_ORIGINS` | only if API and web are on different origins | comma separated allow-list |
| `APP_PUBLIC_URL` | recommended | used for links in emails |
| `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD`, `MAIL_FROM` | for email | SMTP of your provider; email is disabled while the host is empty |
| `REQUIRE_EMAIL_VERIFICATION` | no | `true` blocks sign-in until the email link is clicked |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | bootstrap first admin |
| `TRUST_PROXY` | behind a proxy | use the proxy's `X-Real-IP` (set by the bundled nginx) for rate limiting; never trusts a client-supplied `X-Forwarded-For` first entry |
| `UPLOAD_DIR` | no | where shop photos are stored |
| `CONCIERGE_LLM_API_KEY`, `CONCIERGE_LLM_MODEL` (+ optional `CONCIERGE_LLM_BASE_URL`, `CONCIERGE_LLM_TIMEOUT_MS`) | no | turns on model wording for the AI Concierge (Anthropic Messages API). Empty = built-in rules only, clearly labelled in the UI. Both key and model must be set |
| `CONCIERGE_NAME`, `CONCIERGE_RETENTION_DAYS` | no | display name; days before chat history is auto-deleted (default 30) |
| `SUPPORT_EMAIL`, `SUPPORT_PHONE` | no | contact shown when someone asks for a person |
| `JPA_DDL_AUTO` | no | defaults to `update`; switch to `validate` plus migrations once you have real data |

## Languages, Sahayak and the simulator

- **Languages:** English, Hindi (हिन्दी), Tamil (தமிழ்). First-time visitors get a big language picker; the choice is saved in the browser (`fw_lang`) and can be changed from the top bar. Dictionaries live in `frontend/src/i18n/` (`en.js` is the source of truth).
- **Add a language:** copy `en.js` to `<code>.js` and translate the values (keep `{placeholders}`), import it in `i18n/index.jsx`, add it to `LANGUAGES`, and add a font import in `main.jsx` if the script needs one. Product names in native scripts are mapped in `i18n/products.js` (display) and `backend/.../demand/ProductNames.java` (so "टमाटर" and "tomato" count as the same product in demand).
- **Sahayak (सहायक) is rule-based, not an LLM.** It recognises questions in English, Hinglish, Devanagari and Tamil and answers from the vendor's own opportunity, request and profile data. It can read answers aloud and take voice input where the browser supports Web Speech; voices vary by phone, so text is always shown.
- **Backend validation messages** (for example field errors on signup) are still English; known error codes are translated on the client. The admin screen is English only.
- **Simulator + onboarding:** the landing page has a tap-the-houses demand demo that uses the same weighting idea as the real engine; vendors get a four-step "get your shop ready" checklist. All animation respects `prefers-reduced-motion`.
- **Translations should be reviewed by native speakers** before launch.

## AI Concierge, meeting up and support

- **AI Concierge** (button bottom-right for signed-in customers and vendors): answers about *your own* requests, helps you tell the other person you are late or cannot find the spot, and opens a support case. It is labelled as an AI, runs on server-side rules without any API key, and only optionally uses a language model to reword answers. It can propose actions; nothing is sent or changed until you confirm, and then it goes through the normal authorised API.
- **Meet page** (`/meet/:requestId`): pinned meeting spot, arrival updates, messages, and **optional** live location sharing for 15/30/60 minutes with explicit consent, stop-any-time and automatic deletion. Everything works without sharing location.
- **Support** (`/support`, admin tab "Support"): users open cases; admins set status and a note.
- Details, limits and the security model: [docs/AI_CONCIERGE.md](docs/AI_CONCIERGE.md). FerryWala has no in-app payments, so the Concierge cannot take or refund money; it routes those questions to a person.

## Windows quick start

See [docs/WINDOWS.md](docs/WINDOWS.md). Full audit, security notes, deployment and rollback: [docs/LAUNCH_REPORT.md](docs/LAUNCH_REPORT.md).

## Tests

```bash
cd frontend && npm install && npm run lint && npm test && npm run build   # 18 tests
cd backend/backend && ./mvnw test      # needs Maven Central access
```

## Notes

* Demand estimates are *estimates*. The UI says "estimated", "likely" and "early signal" and shows the data behind each recommendation.
* Payments are not handled: customers pay vendors directly.
* Schema changed from the earlier MVP. Start with an empty database (or drop the old tables) before the first run.
