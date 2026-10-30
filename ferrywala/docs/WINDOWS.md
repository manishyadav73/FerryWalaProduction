# Run FerryWala on Windows (no Docker, no MySQL)

Install once: **Node.js 22 LTS**, **JDK 17 or newer** (Temurin), and **Git** is optional. Check in PowerShell: `node -v` and `java -version`.

## 1. Backend (terminal 1)
```powershell
cd backend\backend
$env:SPRING_PROFILES_ACTIVE="dev"
# optional first admin (min 10 chars), created once:
$env:ADMIN_EMAIL="you@example.com"; $env:ADMIN_PASSWORD="a-long-password-123"
.\mvnw.cmd spring-boot:run
```
Wait for "Started BackendApplication". Dev mode uses an in-memory database: **data resets on every restart**. Without SMTP, no emails are sent (the log says so).

## 2. Frontend (terminal 2)
```powershell
cd frontend
npm install
npm run dev
```
Open http://localhost:5173.

## Try it
1. Register a **Vendor**, open Shop profile, share your location, add products (Tomato, Potato...), go online.
2. In a private window register a **Customer**, pick a locality, open the vendor, send a request, or tap "I need tomato here" to create demand signals.
3. Back as the vendor, open **Today** to see the recommended area and the reasons.
4. Password reset: Login > "Forgot your password?". Without SMTP configured the backend logs that email is disabled; configure SMTP (see LAUNCH_REPORT.md) to receive the link.

## Run the checks
```powershell
cd frontend; npm run lint; npm run build
cd ..\backend\backend; .\mvnw.cmd test
```
