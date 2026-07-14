# Cinema E-Booking System

## Start the app

1. Start the MySQL database:

```powershell
docker compose up -d
```

2. Configure Mailtrap SMTP credentials in the same terminal where you will start Spring Boot.

These two values are required for registration confirmation emails:

```powershell
$env:MAIL_USERNAME="your_mailtrap_smtp_username"
$env:MAIL_PASSWORD="your_mailtrap_smtp_password"
```

Important: `MAIL_USERNAME` and `MAIL_PASSWORD` are the SMTP credentials from your Mailtrap sandbox. They are not your Mailtrap login email or login password.

Where to find them in Mailtrap:

1. Open Mailtrap.
2. Go to Email Testing.
3. Open your Sandbox inbox.
4. Open the SMTP / Integration settings.
5. Copy the SMTP `Username` and `Password`.

The app already has default local values for:

```properties
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_FROM=no-reply@ces-cinema.test
```

You only need to override them if your Mailtrap settings are different:

```powershell
$env:MAIL_HOST="sandbox.smtp.mailtrap.io"
$env:MAIL_PORT="2525"
$env:MAIL_FROM="no-reply@ces-cinema.test"
```

3. Start Spring Boot:

```powershell
./mvnw spring-boot:run
```

4. Open the app:

```text
http://localhost:8080
```

## Notes

- Do not commit real Mailtrap usernames or passwords.
- Environment variables set with `$env:...` only apply to the current PowerShell terminal.
- If you start the app from IntelliJ, add `MAIL_USERNAME` and `MAIL_PASSWORD` to the Run Configuration environment variables.
