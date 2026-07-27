# Pavilion API (Java)

A Java rewrite of the Pavilion community app's backend (originally Node/Express/Drizzle), built with **Spring Boot**, **Spring Data JPA**, and **SQLite**. The React frontend (from the `Society-App` repo, branch `claude/society-application-0mlog9`) talks to this backend — mostly unmodified, with a couple of small, backend-agnostic additions (see below) to support this backend's email-OTP verification steps.

Currently implemented:
- **signup / login / logout / session** (`/api/auth/*`), protected by Google reCAPTCHA v2. Signup is two-step here: `POST /signup` stages the account and emails a 6-digit code; the account isn't actually created until `POST /signup/verify` is called with the correct code. (Login stays single-step.)
- **Entry / visitor OTP** (`/api/visits/*`) — a resident creates a visit (guest, cab/delivery, or household help). If a visitor email is given, the visit starts as `awaiting_verification`: an OTP is emailed to the visitor, and the resident must confirm it via `POST /visits/{id}/confirm` before the visit becomes `pending` and usable at the gate — that's what proves the resident is actually in touch with a real visitor at that address. Without an email, the visit goes straight to `pending` (old behavior, OTP shown immediately). A guard or admin looks the visit up by OTP at the gate and approves or denies it.
- **Emergency / Alerts** (`/api/emergency-alerts/*`) — matches the Node API exactly, no frontend changes needed. A resident raises a one-tap alert (idempotent — raising twice while one's already active just returns the existing one); every other resident, the guard, and admin can see it in the active-alerts list; the reporting resident, a guard, or an admin can resolve it.

Because the signup/entry OTP flows don't exist on the Node backend, the shared frontend's `signup.tsx` and `entry.tsx` branch on the *shape* of the response rather than which backend is active — Node's plain, immediate responses take the old path unchanged; this backend's staged responses trigger the new OTP-entry screens. Those two endpoints are called with a small hand-written `apiPost` helper (`src/lib/api-fetch.ts` in the frontend) instead of the generated OpenAPI client, since they aren't part of the shared Node/Java API contract. Emergency/Alerts, by contrast, has no Java-only fields or flow differences, so it works through the existing generated hooks with zero frontend changes.

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

The API starts on **port 8081** by default (override with `PORT`). A SQLite file is created automatically at `data/pavilion.db` — open it in DB Browser for SQLite to inspect data, same as with the Node version.

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
4. Add environment variables (Settings → Environment) — see the table below. At minimum set `JWT_SECRET` (any long random string), `RECAPTCHA_SECRET_KEY`, `SENDGRID_API_KEY`, `MAIL_FROM`. Leave `PORT` alone — Render sets it automatically.
5. Deploy. Render gives you a URL like `https://pavilion-api-xxxx.onrender.com` — that's your live backend.
6. Once the frontend is deployed too (see its own repo), come back and set `ALLOWED_ORIGIN` to the frontend's URL, and add that same URL to the allowed domains list in the [Google reCAPTCHA admin console](https://www.google.com/recaptcha/admin).

**Free-tier tradeoffs worth knowing:**
- The disk is **ephemeral** — every redeploy or restart wipes the SQLite database (`data/pavilion.db`) and it starts empty again. Fine for testing; if you want data to persist long-term, that needs a paid plan with a persistent disk.
- The free service **spins down after 15 minutes of inactivity** and takes ~30–50 seconds to wake back up on the next request — the first request after a quiet period will feel slow, that's normal.
- **Outbound SMTP is blocked** on Render's free tier (and most free hosts) as an anti-spam measure — that's why email is sent over SendGrid's HTTPS API instead of Gmail SMTP (see below). Plain HTTPS isn't blocked.

## Project layout

```
src/main/java/com/pavilion/api/
  entity/        JPA entities (database tables)
  repository/     Spring Data repositories
  dto/            Request/response records (the API contract)
  controller/     REST endpoints
  security/       JWT signing/verification, session cookie resolution, reCAPTCHA verification
  service/        Email sending (visitor OTP)
  config/         CORS, password encoder
  exception/      Consistent JSON error responses
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8081` | Port the API listens on |
| `JWT_SECRET` | insecure dev default (warns on startup) | Signs session cookies |
| `DATABASE_PATH` | `./data/pavilion.db` | SQLite file location |
| `ALLOWED_ORIGIN` | `http://localhost:5173` | CORS origin allowed to send credentialed requests |
| `RECAPTCHA_SECRET_KEY` | unset (signup/login fail until set) | Google reCAPTCHA v2 server-side secret key |
| `MAILERSEND_API_KEY` | unset (OTP emails are skipped until set) | API key from [mailersend.com](https://mailersend.com) used to send OTP emails |
| `MAIL_FROM` | unset | "From" address for OTP emails — must be an address on your MailerSend trial/verified domain (see below). No default; both this and `MAILERSEND_API_KEY` must be set for email to send at all |

### Setting up email (MailerSend)

Email is sent over MailerSend's HTTPS API rather than SMTP — SMTP ports are blocked outbound on Render's free tier (and most free hosts), but plain HTTPS always works.

1. Sign up at [mailersend.com](https://mailersend.com) (no phone verification required, unlike some competitors).
2. In the dashboard, find your **trial domain** (something like `test-xxxxxx.mlsender.net`) under **Domains**. Set `MAIL_FROM` to an address on it, e.g. `MS_XXXXXX@test-xxxxxx.mlsender.net` (the dashboard shows the exact address to use).
3. Create an API token (**Integrations → API tokens**) and set it as `MAILERSEND_API_KEY`.

```powershell
$env:MAILERSEND_API_KEY="mlsn.xxxxxxxxxxxx"
$env:MAIL_FROM="MS_XXXXXX@test-xxxxxx.mlsender.net"
.\mvnw.cmd spring-boot:run
```

**Known limitation:** MailerSend's trial tier only allows sending to a small set of recipients you explicitly add as "trial recipients" in the dashboard (Domains → your trial domain → Recipients) — it can't email arbitrary visitors yet. Add your own email (and any test addresses you want to try) there. Sending to truly arbitrary recipients (real visitors, other residents signing up) requires verifying your own domain, which needs a domain you own.

If these aren't set, the server still starts fine:
- Visits without a visitor email still work exactly as before (OTP returned immediately, no verification step).
- Visits **with** a visitor email still get staged as `awaiting_verification`, but since no email actually goes out, check `data/pavilion.db`'s `visits` table (or the Java console logs) for the `otp_code` to complete the confirm step manually.
- Signup OTPs behave the same way — check the `pending_signups` table for the code if mail isn't configured.
