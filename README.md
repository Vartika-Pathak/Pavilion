# Pavilion API (Java)

A Java rewrite of the Pavilion community app's backend (originally Node/Express/Drizzle), built with **Spring Boot**, **Spring Data JPA**, and **SQLite**. The existing React frontend (from the `Society-App` repo) talks to this backend unmodified — only the API server changes.

Currently implemented:
- **signup / login / logout / session** (`/api/auth/*`), matching the original Node API's exact request/response shapes, protected by Google reCAPTCHA v2. Verified against the real, unmodified React frontend — signup, dashboard, session persistence, logout, and re-login all work.
- **Entry / visitor OTP** (`/api/visits/*`) — a resident creates a visit (guest, cab/delivery, or household help), gets a 6-digit OTP that's automatically emailed to the visitor if an email was provided, and a guard or admin looks the visit up by OTP at the gate and approves or denies it.

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

If these aren't set, the server still starts fine and visits can still be created — the OTP just won't be emailed (it's still returned in the API response and visible to the resident/guard).
