# FerryWala: audit, changes and launch decision

Scope: the existing FerryWala repository was audited and improved in place (no rewrite).

## 0. What was and was NOT verified (read this first)
| Check | Result |
|---|---|
| Frontend `npm ci`, `npm run lint`, `npm run build` | **Run, pass** (before and after changes) |
| Landing page at 360 / 390 / 430 / 768 / 1280 px | **Run in headless Chromium**: no horizontal overflow, no page errors; animation stages and reduced-motion screenshots reviewed |
| Backend `mvn test` / `mvn package` | **NOT RUN.** The build sandbox could not reach Maven Central (blocked by network policy). Java sources were only syntax-checked with `javac` (no syntax errors; dependency symbols unresolved by design). **Run `.\mvnw.cmd test` before trusting the backend changes.** |
| New backend tests (`AuthServicePasswordResetTest`, new `DemandEngineTest` case) | Written, **not executed** |
| Real email delivery, S3/object storage, MySQL, production deployment | **Not done** (no credentials / services). Nothing is deployed. |
| Authenticated browser flows against a live backend | **Not run** (backend could not be started) |

## 1. Architecture
React 19 + Vite SPA (Leaflet map, STOMP/SockJS live updates, EN/HI/TA i18n) behind nginx, which proxies `/api` and `/ws` to a Spring Boot 3.5 / Java 17 API (JPA, MySQL, stateless JWT, bcrypt). Demand engine is rule-based and explainable. Photos live on disk under `UPLOAD_DIR`.

## 2. Audit: existing state
Already good: bcrypt, hashed verification tokens, role rules in `SecurityConfig`, user reloaded on every request (suspension takes effect immediately), CORS allow-list with no wildcard, magic-byte image validation + re-encode (strips EXIF), rate limiting, no hard-coded secrets (app refuses to start without `JWT_SECRET`), no default admin, stock validation, ownership checks, WebSocket subscription locking, explainable demand with confidence levels.

Defects found:
| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | **High (privacy)** | An area with one customer returned that customer's exact coordinates to vendors in the opportunity | **Fixed**: coordinates snap to the ~1 km grid cell centre; regression test added |
| 2 | High | No password-reset flow | **Added** (single-use, hashed, 1 h, no account enumeration, rate limited) |
| 3 | High | Stock reserve/restore was read-modify-write: two simultaneous accepts could oversell | **Fixed** with row locks taken in id order |
| 4 | Medium | Rate limiter trusted the first (client-controlled) `X-Forwarded-For` entry, so it could be bypassed | **Fixed** (uses nginx `X-Real-IP`) |
| 5 | Medium | Schema managed by `ddl-auto=update` only | Documented below; migrations not added (could not compile/test here) |
| 6 | Medium | JWTs stay valid up to 24 h after a password reset/logout (stateless) | Open, see section 17 |
| 7 | Medium | Order placement has no idempotency key (double tap = two requests); UI disables button while sending | Open |
| 8 | Low | Photos on a single-node disk volume | Open: fine for one server with a backed-up volume |
| 9 | Low | Admin screen and backend validation messages English only | Open |
| 10 | Low | In-memory rate limiter is per instance | Open (single instance is fine) |

## 3. Design system and UI/UX
The existing identity was kept because it already fits a local Indian marketplace: warm paper background, leaf green, saffron and terracotta accents, Fraunces display + Plus Jakarta Sans body, 44px+ tap targets, mobile-first with bottom navigation, visible focus rings, reduced-motion support. The UI/UX Pro Max skill and the 21st.dev MCP are **not installed in this environment** (the PDF is a setup guide for them), so they were not used; no third-party components were added. To use them yourself, follow the PDF, then run Claude Code in this repo.

Added: FAQ section (accessible `<details>`), professional footer with real links, Privacy page, forgot/reset password screens, "Illustration" tag so demo content is labelled.

## 4. Living vendor animation
`LivingScene.jsx` (pure SVG + CSS, no libraries): customers' "needed" chips appear, signal lines converge, houses pulse (3 times), an area lights up with a demand card and bar, a vendor with a cart (wheels turning, vendor bobbing) drives the route, and "Customers see the vendor" appears. Pauses off-screen and in background tabs. Shows the finished picture without motion when `prefers-reduced-motion` is set, **or** on low-power devices (data-saver, `deviceMemory <= 2`, or `hardwareConcurrency <= 2`). Essential information is also in the text step list under the scene. Scene content is labelled as illustration.

## 5. Features fixed or added
Password reset (backend + 2 pages + 3 languages), FAQ, footer, privacy page, stock locking, privacy snapping of demand coordinates, proxy-safe rate limiting.

## 6. Files changed
Backend: `User`, `UserRepository`, `AuthDtos`, `AuthService`, `AuthController`, `MailService`, `SecurityConfig`, `RateLimitFilter`, `ProductRepository`, `OrderService`, `DemandEngine`; tests `AuthServicePasswordResetTest` (new), `DemandEngineTest`.
Frontend: `App.jsx`, `api.js`, `Login.jsx`, `Landing.jsx`, `LivingScene.jsx`, `global.css`, `i18n/en|hi|ta.js`; new `ForgotPassword.jsx`, `ResetPassword.jsx`, `Privacy.jsx`.
Other: `README.md`, `docs/API.md`, `docs/WINDOWS.md`, `docs/LAUNCH_REPORT.md`, `deploy/nginx.conf` (HSTS note).

## 7. Database changes
Two nullable columns on `users`: `password_reset_token` (varchar 64, indexed) and `password_reset_expires_at`. With `ddl-auto=update` Hibernate adds them. For a real database apply manually first (MySQL):
```sql
ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(64) NULL, ADD COLUMN password_reset_expires_at DATETIME(6) NULL;
CREATE INDEX idx_users_password_reset_token ON users (password_reset_token);
```
Take a backup first. Once you hold real data, switch to `JPA_DDL_AUTO=validate` and adopt Flyway/Liquibase; do not rely on `update` long term.

## 8. Security fixes
See table in section 2 (items 1, 3, 4) plus the reset flow design: only a SHA-256 hash of the token is stored, one hour expiry, cleared on use, identical response for unknown/suspended accounts, rate limited, token never logged.

## 9. Tests actually executed
Frontend lint: pass. Frontend production build: pass. Headless-browser landing checks: pass (above). **Backend tests: not executed** (see section 0).

## 10. Environment variables
See `.env.example` and the README table. Required: `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` (32+ random chars). Recommended: `APP_PUBLIC_URL` (https), SMTP settings, `ADMIN_EMAIL`/`ADMIN_PASSWORD` for the first admin, `TRUST_PROXY=true` (already set in compose).

## 11. Email setup
Set `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` (587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` (a sender your provider has verified; e.g. Brevo, Amazon SES, Mailgun or Gmail app password for testing). Email is skipped silently-but-logged while the host is empty. Then set `REQUIRE_EMAIL_VERIFICATION=true`. **Test it:** register, confirm the link arrives, and use Forgot password. Accepting a send in code does not prove delivery; check the inbox and spam folder and your provider's dashboard.

## 12. Image storage
Photos are stored under `UPLOAD_DIR` (`/data/uploads` volume in Docker) and only the file name is in MySQL. Back up that volume with the database. If you move to several servers or a host with an ephemeral disk, switch to object storage (S3/R2); not implemented.

## 13. Windows
See `docs/WINDOWS.md`.

## 14. Deployment (single small server, e.g. a VPS)
1. Install Docker. `git clone`/copy the project; `cp .env.example .env` and fill secrets (`openssl rand -base64 48`).
2. `docker compose up -d --build`. The database is not published to the internet (no `ports:` on `db`).
3. Put HTTPS in front (Caddy `reverse_proxy localhost:8081`, or Cloudflare). Set `APP_PUBLIC_URL=https://your-domain` and enable the HSTS header (comment in `nginx.conf`).
4. Open the firewall for 80/443 only.
5. Smoke test: `/actuator/health` returns UP via `https://your-domain/actuator/health` only if you proxy it (it is not proxied by default; use `docker compose exec backend wget -qO- localhost:8080/actuator/health`), register a customer and vendor, upload a photo, send and accept a request, reset a password.

## 15. Production smoke tests
**Not performed.** Nothing has been deployed.

## 16. Backup and rollback
Back up nightly: `docker compose exec db mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" ferrywala > ferrywala-$(date +%F).sql` and copy the `uploads` volume (`docker run --rm -v ferrywala_uploads:/d -v $PWD:/b alpine tar czf /b/uploads.tgz -C /d .`). Keep copies off the server and test a restore. Rollback: keep the previous image tag (`docker tag`), redeploy it with `docker compose up -d`, and restore the SQL dump only if a migration damaged data.

## 17. Remaining issues (see also the addendum at the end)
- **Critical (launch blockers until done):** run `mvn test` and fix anything that fails; test real email delivery; deploy to a staging URL and run the smoke test.
- **High:** none known beyond the above.
- **Medium:** token revocation after password change (add a `passwordChangedAt` check in `JwtAuthFilter`, or shorten `JWT_EXPIRATION_MS`); order idempotency; Flyway migrations; translations need native-speaker review; privacy page needs legal review; dependency vulnerability scan (`npm audit`, OWASP dependency-check) could not be run here.
- **Low:** admin screen English only; per-instance rate limiter; local disk for photos; vendor stats "completed today" loads up to 200 rows.

## 18. Launch recommendation
**NO-GO for a public launch today; GO for a staging deployment.** Reason: the backend changes are unverified by a compiler and tests in this environment and nothing has been tested against real email or a live server. After the three critical items in section 17 pass, a small pilot launch (one locality, real vendors you know) is reasonable. The product does not claim guaranteed demand or payments, which matches what the code does.

---

# Addendum: AI Concierge, meeting coordination, support (this delivery)

## A. Verification table (run on the exact source in the ZIP)

| Check | Result |
|---|---|
| Frontend `npm run lint` | **Pass** |
| Frontend `npm test` (vitest + testing-library, 4 files, 14 tests: i18n parity, Dialog, Concierge panel, Meet page consent flow) | **Pass** |
| Frontend `npm run build` | **Pass** |
| Headless Chromium + axe-core (WCAG 2 A/AA), 375 px and 1280 px, Concierge / Meet / Support screens | **0 violations, no horizontal scroll, no console errors**. Backend was a **mock** (Playwright route stubs), so this proves the UI, not the integration |
| Backend pure-logic tests (intent classifier, prompt guard, meeting policy, reply composer, rate-limit path matching): 37 tests | **Pass**, but run through a small hand-written JUnit-compatible runner, **not Maven/Surefire** |
| Backend new code compiled with `javac --release 17` against hand-written API stubs for Spring/JPA/Jackson | **0 errors**. Stubs are not the real libraries |
| `./mvnw test` (including `OrderAccessTest`, `MeetingServiceTest`, which use Mockito) | **NOT EXECUTED**: Maven Central was blocked in the build environment. Those two test classes have never run |
| Backend start-up, Hibernate schema creation, real HTTP endpoints, WebSocket, MySQL | **NOT EXECUTED** |
| Migration `db/migrations/V2__concierge_meeting_support.sql` | **NOT EXECUTED**; reference only |
| Real LLM provider call | **NOT EXECUTED** |
| Real-device geolocation, screen readers, Hindi/Tamil review by native speakers | **NOT DONE** |

## B. Decision
Do **not** treat this as production-ready. Run `./mvnw test` and a real end-to-end pass (two accounts, one accepted request, location sharing on two phones) first.

## C. Scope note
The brief described a ferry-booking product. FerryWala is a street-vendor marketplace, so the brief was applied to that: the "meeting point" is the request location, there are no booking/checkout/payment flows, and the ferry-specific 3D scene was not built.
