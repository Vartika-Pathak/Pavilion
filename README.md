# Pavilion API (Java)

A Java rewrite of the Pavilion community app's backend (originally Node/Express/Drizzle), built with **Spring Boot**, **Spring Data JPA**, and **SQLite**. The React frontend (from the `Society-App` repo, branch `claude/society-application-0mlog9`) talks to this backend — mostly unmodified, with a couple of small, backend-agnostic additions (see below) to support this backend's email-OTP verification steps.

Currently implemented:
- **signup / login / logout / session** (`/api/auth/*`), protected by Google reCAPTCHA v2. Signup is two-step here: `POST /signup` stages the account and emails a 6-digit code; the account isn't actually created until `POST /signup/verify` is called with the correct code. (Login stays single-step.) Before that, a first-time resident submits `POST /api/auth/verification-requests` (just a name + flat number) instead of being auto-checked against a static list — an admin reviews it (see Admin below) and approves or rejects. The resident's "under review" screen polls `GET /api/auth/verification-requests/status` until it flips to `approved`. Both endpoints always return 200 (not 404 on "no match") so the frontend can tell "not approved yet" apart from "this backend doesn't have the endpoint at all" (the Node backend, which skips straight to the signup form instead of blocking everyone out). Signup optionally accepts a `familyMembers` array (name/relation/age each); those rows are created in `family_members` once the account itself is created via OTP verification — that's what powers the second, conditional "family details" page of the frontend signup flow.
- **Admin** (`/api/admin/*`, role `admin` only, enforced with `@PreAuthorize`) — `GET /verification-requests` lists the queue of resident verification requests (pending first); `PATCH /verification-requests/{id}` toggles `documentsVerified`/`paymentReceived` (both manual — confirmed some other way, not through the app) and, once both are true, can set `action: "approve"` (or `"reject"` any time). There's no self-service way to become an admin — a role has to be set to `admin` directly in the database.
- **Entry / visitor OTP** (`/api/visits/*`) — a resident creates a visit (guest, cab/delivery, or household help). If a visitor email is given, the visit starts as `awaiting_verification`: an OTP is emailed to the visitor, and the resident must confirm it via `POST /visits/{id}/confirm` before the visit becomes `pending` and usable at the gate — that's what proves the resident is actually in touch with a real visitor at that address. Without an email, the visit goes straight to `pending` (old behavior, OTP shown immediately). A guard or admin looks the visit up by OTP at the gate and approves or denies it.
- **Emergency / Alerts** (`/api/emergency-alerts/*`) — matches the Node API exactly, no frontend changes needed. A resident raises a one-tap alert (idempotent — raising twice while one's already active just returns the existing one); every other resident, the guard, and admin can see it in the active-alerts list; the reporting resident, a guard, or an admin can resolve it.
- **Chat assistant** (`/api/chat/*`, Java only, open to signed-out visitors too) — a floating widget on every page that answers questions about using the app, via Google Gemini's free-tier API. Conversation history is persisted server-side per browser-tab session (`chat_messages` table) and replayed as multi-turn context on every request, so the assistant can reference earlier turns rather than treating each message as a one-off. `GET /api/chat/suggestions` decides the quick-reply prompts server-side based on whether the caller is signed in, instead of the frontend hardcoding them. `POST /api/chat/message` is rate-limited per client IP (in-memory sliding window) since it's reachable by anyone, protecting Gemini's shared free-tier daily quota. Scoped with a system instruction to stick to app-usage questions, avoid markdown formatting, and decline anything else.

Because the signup/entry OTP flows and the chat assistant don't exist on the Node backend, the shared frontend's `signup.tsx`, `entry.tsx`, and the new `chat-widget.tsx` branch on the *shape* of the response (or just fail gracefully) rather than which backend is active — Node's plain, immediate responses take the old path unchanged; this backend's staged responses trigger the new OTP-entry screens, and a failed chat request just shows an inline "assistant unavailable" message. These endpoints are called with a small hand-written `apiPost` helper (`src/lib/api-fetch.ts` in the frontend) instead of the generated OpenAPI client, since they aren't part of the shared Node/Java API contract. Emergency/Alerts, by contrast, has no Java-only fields or flow differences, so it works through the existing generated hooks with zero frontend changes.

**Field validation** (`jakarta.validation` annotations on the request DTOs in `dto/`, enforced server-side regardless of what the frontend does): names (a resident's own, and a visitor's) allow letters and spaces only — no digits or symbols; a visitor's mobile number, if given at all, has to be exactly 10 digits (it stays optional — this only fires when something's actually entered); a flat number has to be a single letter, a hyphen, then 1-3 digits (e.g. `A-100`); passwords are capped at 72 characters in addition to the existing 8-character minimum, since BCrypt (what they're hashed with) silently truncates anything longer, which would otherwise let two different long passwords collide; every OTP field (signup verification, visit confirmation, gate lookup) requires exactly 6 digits. A bad value gets a specific `field: message` 400 response rather than a generic error — see `GlobalExceptionHandler`.

More features (Maintenance, Complain, Amenities+Stripe) are being ported over next, one at a time.

## Requirements

- Java 21+ (a JDK, not just a JRE)
- Nothing else — this project bundles a Maven wrapper (`mvnw.cmd` / `mvnw`), so a separate Maven install isn't needed

## Running it (Windows PowerShell)

```powershell
$env:JWT_SECRET="pick-any-long-random-string-here"
.\mvnw.cmd spring-boot:run
```

(Mac/Linux: `export JWT_SECRET=...` then `./mvnw spring-boot:run`)

The API starts on **port 8081** by default (override with `PORT`). By default, a SQLite file is created automatically at `data/pavilion.db` — open it in DB Browser for SQLite to inspect data. To use MySQL instead, see the next section.

## Using MySQL locally (XAMPP)

By default this runs on SQLite with zero setup. If you'd rather use MySQL (e.g. via XAMPP, so you can browse data in phpMyAdmin), set these env vars before starting — they override the SQLite defaults, and nothing else in the app changes:

```powershell
$env:JWT_SECRET="pick-any-long-random-string-here"
$env:DB_URL="jdbc:mysql://localhost:3306/pavilion?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
$env:DB_USERNAME="root"
$env:DB_PASSWORD=""
$env:DB_DIALECT="org.hibernate.dialect.MySQLDialect"
.\mvnw.cmd spring-boot:run
```

`root` / empty password matches XAMPP's default MySQL setup — adjust if you've changed it. `createDatabaseIfNotExist=true` means you don't need to create the `pavilion` database yourself in phpMyAdmin first; it's created automatically, and Flyway creates the tables in it on first startup (see "Database migrations" below).

If you already have a local `pavilion` database from before migrations existed (tables created by the old `ddl-auto=update` behavior), Flyway adopts it automatically the first time you start with this version — no manual steps, your existing data stays put. See "Database migrations" for what's actually happening there.

Just make sure XAMPP's MySQL service is actually running (via the XAMPP Control Panel) before starting the API — if it isn't, startup fails immediately with a clear "Access denied" or "Communications link failure" error rather than silently falling back to SQLite.

Switching back to SQLite later is just deleting/unsetting those five env vars, nothing else to undo. This is a local-only choice — the deployed Render backend keeps using SQLite regardless, since XAMPP only runs on your own machine.

## Running the existing React frontend against this backend

From the `Society-App-Live` repo's `artifacts/pavilion` folder, in PowerShell:

```powershell
$env:PORT=5173
$env:BASE_PATH="/"
$env:API_PORT=8081
pnpm run dev
```

`API_PORT=8081` points the frontend's dev proxy at this Java backend instead of the Node one. No frontend code changes needed.

## Deploying (free hosting on Render)

This repo includes a `Dockerfile` that builds and runs the API — Render (or any Docker host) can deploy it directly:

1. Sign up at [render.com](https://render.com) (no credit card needed for the free tier) and connect your GitHub account.
2. **New → Web Service** → pick this repo (`Pavilion`) → Render should auto-detect the `Dockerfile`. If it asks for a runtime, choose **Docker**.
3. Instance type: **Free**.
4. Add environment variables (Settings → Environment) — see the table below. At minimum set `JWT_SECRET` (any long random string), `RECAPTCHA_SECRET_KEY`, `BREVO_API_KEY`, `MAIL_FROM`, `GEMINI_API_KEY`. Leave `PORT` alone — Render sets it automatically.
5. Deploy. Render gives you a URL like `https://pavilion-api-xxxx.onrender.com` — that's your live backend.
6. Once the frontend is deployed too (see its own repo), come back and set `ALLOWED_ORIGIN` to the frontend's URL, and add that same URL to the allowed domains list in the [Google reCAPTCHA admin console](https://www.google.com/recaptcha/admin).

**Free-tier tradeoffs worth knowing:**
- The disk is **ephemeral** — every redeploy or restart wipes the SQLite database (`data/pavilion.db`) and it starts empty again. Fine for testing; if you want data to persist long-term, that needs a paid plan with a persistent disk.
- The free service **spins down after 15 minutes of inactivity** and takes ~30–50 seconds to wake back up on the next request — the first request after a quiet period will feel slow, that's normal.
- **Outbound SMTP is blocked** on Render's free tier (and most free hosts) as an anti-spam measure — that's why email is sent over Brevo's HTTPS API instead of SMTP (see below). Plain HTTPS isn't blocked.

## Project layout

```
src/main/java/com/pavilion/api/
  entity/        JPA entities (database tables)
  repository/     Spring Data repositories
  dto/            Request/response records (the API contract)
  controller/     REST endpoints
  security/       JWT signing/verification, the auth filter, reCAPTCHA verification
  service/        Email sending (visitor OTP), the chat assistant, rate limiting
  config/         Spring Security, CORS, password encoder, OpenAPI/Swagger
  exception/      Consistent JSON error responses
src/main/resources/db/migration/
  sqlite/         Flyway migrations for SQLite (the default)
  mysql/          The same migrations, in MySQL's dialect (for local XAMPP use)
src/test/java/com/pavilion/api/
  AbstractIntegrationTest.java   shared MockMvc + test-user setup for the controller tests below
  controller/     One test class per controller, through the real Spring Security filter chain
  security/, service/, exception/   focused unit tests for the lower-level pieces
```

## Database migrations

Schema changes are Flyway migrations under `src/main/resources/db/migration/{sqlite,mysql}/`, not
Hibernate auto-DDL — `spring.jpa.hibernate.ddl-auto=none`, so the app never alters your schema on
its own. Add a new one as `V2__whatever_it_does.sql` (bump the number each time) in *both* the
`sqlite/` and `mysql/` folders, since Flyway picks whichever one matches the database you're
actually connected to (`spring.flyway.locations=classpath:db/migration/{vendor}`) — Hibernate's
own entity-to-table mapping is the same either way, but the two databases need different SQL to
create the same shape (e.g. SQLite's `INTEGER PRIMARY KEY AUTOINCREMENT` vs MySQL's
`BIGINT AUTO_INCREMENT`).

A fresh database just gets every migration applied in order on first startup. A database that
already has these tables from before migrations existed (any local SQLite or MySQL setup that
predates this) gets **baselined** automatically instead (`spring.flyway.baseline-on-migrate=true`,
`baseline-version=1`) — Flyway records `V1` as already applied without re-running its
`CREATE TABLE`s, so your existing data is untouched. This was tested against both a pre-existing
local SQLite file and a pre-existing local MySQL database before shipping.

One dialect-specific note if you're ever debugging a startup schema error: `ddl-auto` is `none`
rather than `validate` on purpose. `hibernate-community-dialects`' `SQLiteDialect` generates
`integer` (not `bigint`) for identity/primary-key columns — SQLite requires exactly that type for
its rowid-alias autoincrement to work — but Hibernate's schema *validator* independently expects
`bigint` for any `Long`-mapped column, so `validate` mode fails against a perfectly correct SQLite
schema. `none` sidesteps that inconsistency; Flyway is the single source of truth for the schema
either way.

## Testing

```powershell
.\mvnw.cmd test
```

44 tests: full-context integration tests for every controller (`AuthControllerTest`,
`VisitControllerTest`, `EmergencyAlertControllerTest`, `ChatControllerTest`) that go through
`MockMvc` and the real Spring Security filter chain — the same JWT cookie auth, `@PreAuthorize`
role checks, and JSON error responses a real request hits — against a throwaway in-memory SQLite
database, plus focused unit tests for `JwtService`, `ChatRateLimiter`, and `GlobalExceptionHandler`
(the last one specifically pins down the two exception-handling edge cases the Spring Security
work above surfaced, so they can't silently regress). External calls (Gemini, reCAPTCHA) are
mocked — nothing in the suite makes a real network call.

## Security, health checks, and API docs

Authentication is real Spring Security, not hand-rolled per-controller checks: `JwtAuthenticationFilter`
reads the `session` cookie once per request and, if it's a valid token for a real user, populates
Spring Security's context with that `User` as the principal. Controllers then just declare what they
need — `@AuthenticationPrincipal User user` to require *some* signed-in user (the default for every
endpoint not listed below), or `@PreAuthorize("hasRole('GUARD') or hasRole('ADMIN')")` to also require
a specific role, as the visitor-lookup and approve/deny endpoints do. A handful of endpoints are
explicitly public: `/api/auth/signup`, `/signup/verify`, `/login`, `/logout`, `/api/healthz`, and the
chat endpoints (so the chat widget works for signed-out visitors too). Anything else returns a JSON
`401`/`403` — see `SecurityConfig`.

Two small operational extras came along with that:

- **Health check**: `GET /actuator/health` → `{"status":"UP"}`. Only `health` and `info` are exposed
  (`management.endpoints.web.exposure.include` in `application.properties`) — nothing that could leak
  config, like `/actuator/env` or `/actuator/beans`, is reachable.
- **API docs**: `GET /swagger-ui.html` renders an interactive, always-up-to-date list of every endpoint,
  generated from the actual controllers/DTOs (springdoc-openapi). Useful for exploring the API without
  reading the controller source. The raw OpenAPI JSON is at `/v3/api-docs`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8081` | Port the API listens on |
| `JWT_SECRET` | insecure dev default (warns on startup) | Signs session cookies |
| `DATABASE_PATH` | `./data/pavilion.db` | SQLite file location (ignored if `DB_URL` is set) |
| `DB_URL` | unset (falls back to the SQLite URL above) | Full JDBC URL — set to switch databases entirely, e.g. to MySQL (see "Using MySQL locally") |
| `DB_DRIVER` | `org.sqlite.JDBC` | JDBC driver class — set to `com.mysql.cj.jdbc.Driver` alongside `DB_URL` for MySQL |
| `DB_DIALECT` | `org.hibernate.community.dialect.SQLiteDialect` | Hibernate dialect — set to `org.hibernate.dialect.MySQLDialect` for MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | unset | Database credentials — SQLite ignores these; MySQL needs them |
| `ALLOWED_ORIGIN` | `http://localhost:5173` | CORS origin allowed to send credentialed requests |
| `RECAPTCHA_SECRET_KEY` | unset (signup/login fail until set) | Google reCAPTCHA v2 server-side secret key |
| `BREVO_API_KEY` | unset (OTP emails are skipped until set) | API key from [brevo.com](https://www.brevo.com) used to send OTP emails |
| `GEMINI_API_KEY` | unset (chat assistant returns 503 until set) | API key from [aistudio.google.com/apikey](https://aistudio.google.com/apikey), free tier, powers the chat widget |
| `MAIL_FROM` | unset | "From" address for OTP emails — must be single-sender-verified in Brevo (see below). No default; both this and `BREVO_API_KEY` must be set for email to send at all |

### Setting up email (Brevo)

Email is sent over Brevo's HTTPS API rather than SMTP — SMTP ports are blocked outbound on Render's free tier (and most free hosts), but plain HTTPS always works. Brevo was chosen over MailerSend (used previously) specifically because its free tier can send to **any recipient**, not just a pre-approved allowlist — no domain ownership needed, unlike MailerSend/Resend/Mailgun's sandbox modes.

1. Sign up at [brevo.com](https://www.brevo.com) (free tier: 300 emails/day, no credit card required).
2. **Verify a single sender address** — Settings → Senders, Domains & Dedicated IPs → Senders → Add a sender, using any email you actually control (e.g. your own Gmail address). Brevo emails you a confirmation link; click it. This is the address to set as `MAIL_FROM` — no domain/DNS setup required, just that one email confirmation.
3. Create an API key (**Settings → SMTP & API → API Keys → Generate a new API key**) and set it as `BREVO_API_KEY`.

```powershell
$env:BREVO_API_KEY="xkeysib-xxxxxxxxxxxx"
$env:MAIL_FROM="your-verified-sender@example.com"
.\mvnw.cmd spring-boot:run
```

Once the sender is verified, mail can go to any recipient — real visitors, other residents signing up, anyone — with no extra allowlisting step.

If these aren't set, the server still starts fine:
- Visits without a visitor email still work exactly as before (OTP returned immediately, no verification step).
- Visits **with** a visitor email still get staged as `awaiting_verification`, but since no email actually goes out, check `data/pavilion.db`'s `visits` table (or the Java console logs) for the `otp_code` to complete the confirm step manually.
- Signup OTPs behave the same way — check the `pending_signups` table for the code if mail isn't configured.

### Setting up the chat assistant (Gemini)

1. Go to [aistudio.google.com/apikey](https://aistudio.google.com/apikey), sign in with a Google account, and create an API key — free, no credit card required.
2. Set it as `GEMINI_API_KEY`.

```powershell
$env:GEMINI_API_KEY="your-key-here"
.\mvnw.cmd spring-boot:run
```

If unset, the chat widget still renders but shows "The assistant isn't available right now" when a message is sent — nothing else on the site is affected.
