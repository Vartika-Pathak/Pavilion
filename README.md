# Pavilion API (Java)

The backend for Pavilion, a residential society management app — sign-up/verification, visitor entry with OTPs and standing passes, maintenance and complaints, amenity bookings and parking passes, maintenance billing and collections, notices/events/gallery, emergency alerts, an audit trail, and a Gemini-powered help chatbot. Built with **Spring Boot**, **Spring Data JPA**, and **SQLite** (or MySQL — see below), with real Spring Security (JWT session cookies, role-based `@PreAuthorize` checks) rather than hand-rolled per-controller auth.

This backend replaced an earlier Node/Express/Drizzle prototype; the React frontend (from the `Society-App` repo) is unmodified except for a handful of backend-agnostic additions to support flows the old backend didn't have (email-OTP verification, the chat widget).

## What's implemented

**Accounts & access**
- Two-step signup (`/api/auth/*`) protected by Google reCAPTCHA v2 — `POST /signup` stages the account and emails a 6-digit code, `POST /signup/verify` creates it once that code is confirmed. Optionally accepts a `familyMembers` array, created once the account itself exists.
- A first-time resident applies with just a name + flat number (`POST /api/auth/verification-requests`); an admin manually confirms documents/payment were handled some other way and approves or rejects the request (`/api/admin/verification-requests/*`) before that person can even reach the signup form. No self-service path to the `admin` role — set directly in the database.
- Role-based access throughout: `resident`, `guard`, `admin`, enforced server-side with `@PreAuthorize`, not just hidden in the UI.

**Gate & visitor entry** (`/api/visits/*`)
- A resident logs a visitor (guest, cab/delivery, household help, or maintenance staff) and gets a 6-digit OTP to share; a guard or admin looks it up at the gate and approves or denies it. If a visitor email is given, the OTP is emailed and the resident must confirm it before the entry is usable — proof they're actually in touch with that visitor.
- Household help gets a **90-day standing pass** instead of a one-time code: the same OTP is re-checked and re-approved at the gate every day rather than being consumed after one use, and the resident can revoke it early if staff changes.
- A society-wide **Entry Log** (guard/admin) shows every visit ever logged, who invited whom, and its current status, with a page-size selector.

**Maintenance, complaints & emergencies**
- Maintenance requests and complaints, each with photo uploads, category/status tracking, and a close-the-loop step where the resident confirms a "resolved" request before it's actually closed (or reopens it if it isn't).
- One-tap emergency alerts, visible to every resident/guard/admin until the reporter, a guard, or an admin resolves them.

**Amenities, billing & payments** (Stripe)
- Free amenity bookings (clubhouse, pool) and paid ones (tennis court, party hall) via Stripe Checkout, in INR.
- Vehicle registration and paid parking passes (Stripe), maintenance dues payable online per flat (Stripe), plus the underlying admin-side bookkeeping: maintenance settings/discounts/rates, special contributions, vendor bills and bill payments, and a manually-logged collections ledger (cash/cheque/UPI/bank transfer) — none of that is a live payment gateway, it's accounting records for money that moved outside the app.
- Reports: maintenance due list, monthly collections/expenditure, income vs. expense trend, balance sheet, income statement.

**Masters & content**
- Society/Building/Flat masters, with flats independently assignable to resident accounts (and independently marked occupied — see the app's own "why does this flat show vacant" edge case if you're digging into the code).
- Society notices, events, a member directory, and a **Gallery** the admin manages by uploading photo files directly (stored server-side, not pasted URLs) — they show up on the public Gallery page immediately.
- A full audit log of every admin create/update/delete action.

**Chat assistant** (`/api/chat/*`, open to signed-out visitors too)
- A floating widget on every page, backed by Google Gemini's free-tier API, scoped to answering questions about using the app. Conversation history persists per browser-tab session and is replayed as context on every request. Rate-limited per client IP since it's reachable by anyone.

**Field validation** (`jakarta.validation` on the request DTOs in `dto/`, enforced server-side regardless of what the frontend does): names allow letters and spaces only; any 10-digit mobile number field must start with 6–9 (India's valid mobile prefix range) when one is given; a flat number is a letter, a hyphen, then 1–3 digits (e.g. `A-100`); passwords are capped at 72 characters (BCrypt silently truncates longer ones, which would otherwise let two different passwords collide) on top of an 8-character minimum; every OTP field requires exactly 6 digits. A bad value gets a specific `field: message` 400 response — see `GlobalExceptionHandler`.

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
4. Add environment variables (Settings → Environment) — see the table below. At minimum set `JWT_SECRET` (any long random string), `RECAPTCHA_SECRET_KEY`, `BREVO_API_KEY`, `MAIL_FROM`, `GEMINI_API_KEY`, `STRIPE_SECRET_KEY`. Leave `PORT` alone — Render sets it automatically.
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

298 tests: full-context integration tests for every controller — one test class each — that go
through `MockMvc` and the real Spring Security filter chain (the same JWT cookie auth,
`@PreAuthorize` role checks, and JSON error responses a real request hits) against a throwaway
in-memory SQLite database, plus focused unit tests for `JwtService`, `ChatRateLimiter`, amenity
slot logic, and `GlobalExceptionHandler` (which pins down exception-handling edge cases so they
can't silently regress). External calls (Gemini, reCAPTCHA, Stripe) are mocked — nothing in the
suite makes a real network call.

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
| `STRIPE_SECRET_KEY` | unset (paid amenity/parking/maintenance checkout fails until set) | Secret key from the [Stripe dashboard](https://dashboard.stripe.com/apikeys) — use a **test** key (`sk_test_...`) unless you actually intend to take real payments |

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
