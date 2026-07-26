# Pavilion API (Java)

A Java rewrite of the Pavilion community app's backend (originally Node/Express/Drizzle), built with **Spring Boot**, **Spring Data JPA**, and **SQLite**. The React frontend (from the `Society-App` repo, branch `claude/society-application-0mlog9`) talks to this backend — mostly unmodified, with a couple of small, backend-agnostic additions (see below) to support this backend's email-OTP verification steps.

Currently implemented:
- **signup / login / logout / session** (`/api/auth/*`), protected by Google reCAPTCHA v2. Signup is two-step here: `POST /signup` stages the account and emails a 6-digit code; the account isn't actually created until `POST /signup/verify` is called with the correct code. (Login stays single-step.)
- **Entry / visitor OTP** (`/api/visits/*`) — a resident creates a visit (guest, cab/delivery, or household help). If a visitor email is given, the visit starts as `awaiting_verification`: an OTP is emailed to the visitor, and the resident must confirm it via `POST /visits/{id}/confirm` before the visit becomes `pending` and usable at the gate — that's what proves the resident is actually in touch with a real visitor at that address. Without an email, the visit goes straight to `pending` (old behavior, OTP shown immediately). A guard or admin looks the visit up by OTP at the gate and approves or denies it.

Because these two flows don't exist on the Node backend, the shared frontend's `signup.tsx` and `entry.tsx` branch on the *shape* of the response rather than which backend is active — Node's plain, immediate responses take the old path unchanged; this backend's staged responses trigger the new OTP-entry screens. The new endpoints are called with a small hand-written `apiPost` helper (`src/lib/api-fetch.ts` in the frontend) instead of the generated OpenAPI client, since they aren't part of the shared Node/Java API contract.

More features (Maintenance, Complain, Emergency alerts, Amenities+Stripe) are being ported over next, one at a time.

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
| `MAIL_USERNAME` | unset (OTP emails are skipped until set) | Gmail address used to send visitor OTP emails |
| `MAIL_PASSWORD` | unset | Gmail **App Password** (not your regular password) — generate one at myaccount.google.com/apppasswords, and enter it here with no spaces |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port (STARTTLS) |

### Setting the mail env vars (Windows PowerShell)

```powershell
$env:MAIL_USERNAME="yourname@gmail.com"
$env:MAIL_PASSWORD="16characterapppassword"
.\mvnw.cmd spring-boot:run
```

If these aren't set, the server still starts fine:
- Visits without a visitor email still work exactly as before (OTP returned immediately, no verification step).
- Visits **with** a visitor email still get staged as `awaiting_verification`, but since no email actually goes out, check `data/pavilion.db`'s `visits` table (or the Java console logs) for the `otp_code` to complete the confirm step manually.
- Signup OTPs behave the same way — check the `pending_signups` table for the code if mail isn't configured.
