# CES Cinema Backend — Local Setup Guide

## Prerequisites

- **JDK 17 or newer** (project targets Java 17; newer JDKs work fine too)
- **Docker Desktop** — running, for the MySQL database
- **A free [Mailtrap](https://mailtrap.io) account** — lets you test the registration confirmation email without it going to a real inbox
- No separate Maven install needed — the repo includes `./mvnw` (Maven Wrapper)

## 1. Start the database

From the folder containing `docker-compose.yml`:

```bash
docker compose up -d
```

This starts a MySQL container named `ces-mysql` at `localhost:33306`, database `ces_db` (user `root`, password `mysqlpass`).

**First time only:** the SQL files in `db/init/` (`01_movies.sql`, `02_users.sql`) create and seed every table — but only run automatically against a *fresh* Docker volume. If you ever pull schema changes and the tables don't update, reset with:
```bash
docker compose down -v
docker compose up -d
```
(`-v` deletes the volume so the init scripts run again — wipes local data, which is fine for a dev DB.)

**"Container name already in use" error:** a `ces-mysql` container already exists from an earlier run.
```bash
docker rm -f ces-mysql
docker compose up -d
```

## 2. Set up Mailtrap

Registration sends a real confirmation email — Mailtrap catches it in a private test inbox instead of delivering it anywhere.

1. Sign up at [mailtrap.io](https://mailtrap.io) (free tier is enough)
2. It auto-creates a sandbox called **My Sandbox** —> go to Email Testing -> Sandboxes -> open it
3. Open the **Integration** tab -> select **SMTP** -> copy the Host, Port, Username, and Password shown there

That username/password is *not* your Mailtrap login — it's a key generated per sandbox. Everyone should grab their own (it's free) so you're not all seeing each other's test emails in one inbox.

## 3. Set environment variables

The app needs these 4 set before it'll start properly:

**macOS / Linux (bash/zsh):**
```bash
export MAIL_HOST=sandbox.smtp.mailtrap.io
export MAIL_PORT=2525
export MAIL_USERNAME=your_sandbox_username
export MAIL_PASSWORD=your_sandbox_password
```

**Windows (PowerShell):**
```powershell
$env:MAIL_HOST="sandbox.smtp.mailtrap.io"
$env:MAIL_PORT="2525"
$env:MAIL_USERNAME="your_sandbox_username"
$env:MAIL_PASSWORD="your_sandbox_password"
```

**These only last for that one terminal session.** New window/tab = gone, and the app fails to start with `Could not resolve placeholder 'MAIL_HOST'` until you export them again. To avoid retyping every time:
- Add the 4 lines to your shell profile (`~/.zshrc`, `~/.bash_profile`, or PowerShell `$PROFILE`) so every new terminal has them automatically, **or**
- **If you run the app from an IDE run button** instead of a terminal: terminal exports won't reach it at all — set these in the IDE's Run/Debug Configuration → Environment Variables instead

## 4. Run the backend

From the backend project root (same folder as `pom.xml`), in a terminal that has the env vars set:

```bash
./mvnw spring-boot:run
```

This runs in the foreground and has to stay open. **Open a second terminal** for testing — if you close or reuse this one to type other commands, the server stops.

Wait for `Started BackendApplication` with no error underneath it. The API is now live at `http://localhost:8080`.

## 5. Make sure that it's working

In your second terminal:

```bash
curl http://localhost:8080/api/movies
```
Should return the movie list as JSON.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cinema.com","password":"Password123!"}'
```
Should return `{"id":1,"email":"admin@cinema.com","firstName":"Admin","lastName":"User","role":"ADMIN"}`.

If both return proper output then you're fully set up!

## Test accounts (already seeded, all password `Password123!`)

| Email | Role | Status | Useful for |
|---|---|---|---|
| `admin@cinema.com` | ADMIN | ACTIVE | Admin-only behavior |
| `customer@cinema.com` | CUSTOMER | ACTIVE | Normal logged-in flows |
| `inactive@cinema.com` | CUSTOMER | INACTIVE | The "please confirm your email" error path |

## Auth API quick reference

**`POST /api/auth/register`**
```json
{"email":"test@example.com","password":"TestPass123!","firstName":"Test","lastName":"User","phone":"555-555-5555","promotionsOptIn":false}
```
`phone` and `promotionsOptIn` are optional. New accounts start `INACTIVE` until the emailed link is clicked.

**`POST /api/auth/login`**
```json
{"email":"test@example.com","password":"TestPass123!"}
```
Returns `id`/`email`/`firstName`/`lastName`/`role`, sets a session cookie. Distinct messages for unknown email / wrong password / unconfirmed account (401 / 401 / 403).

**`POST /api/auth/logout`** — no body, invalidates the session.

**`GET /api/auth/confirm?token=...`** — open this in an actual browser tab, not curl — it renders an HTML page, not JSON.

**Frontend integration note:** this backend only serves JSON (except `/api/auth/confirm`) — there's no UI here. The frontend is expected to run on `localhost:5173` or `localhost:3000` (already CORS-allowed) and needs `credentials: 'include'` on every `fetch()` call for the session cookie to survive the cross-origin request.
