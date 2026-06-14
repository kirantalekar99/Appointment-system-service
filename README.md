# Appointment System Service

Spring Boot appointment system with MongoDB, OTP email, admin management, doctor/patient appointment flows, and static frontend pages served by Spring Boot.

## Configuration

The project uses one Spring configuration file:

```text
backend/src/main/resources/application.properties
```

This file is committed with environment-variable placeholders. Do not paste real MongoDB, Brevo, or admin passwords into source code before pushing to GitHub.

## Local Run

Set your private values in IntelliJ Run Configuration environment variables, then run:

```powershell
cd backend
mvn spring-boot:run
```

Local app URL:

```text
http://localhost:1010
```

## Render Deployment

Deploy the repository as a Docker web service from branch `kiran`.

Required Render environment variables:

```text
SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_DATA_MONGODB_URI=your MongoDB Atlas URI
SPRING_DATA_MONGODB_DATABASE=appointment_db
SPRING_DATA_MONGODB_AUTO_INDEX_CREATION=true
SPRING_JACKSON_SERIALIZATION_WRITE_DATES_AS_TIMESTAMPS=false
SERVER_ERROR_INCLUDE_MESSAGE=always
MAIL_BREVO_API_KEY=your Brevo API key
APP_MAIL_FROM=your verified sender email
APP_MAIL_SENDER_NAME=CareBridge
MAIL_BREVO_BASE_URL=https://api.brevo.com
APP_OTP_EXPIRY_MINUTES=10
APP_ADMIN_EMAIL=your admin login email
APP_ADMIN_PASSWORD=your admin login password
APP_ADMIN_NAME=CareBridge Admin
APP_ADMIN_PHONE=9999999999
```

Do not add `SPRING_PROFILES_ACTIVE`; there is no separate Render profile file now. Render provides `PORT` automatically, and the app binds to `${PORT}`. If your Render dashboard already has `PORT=1010`, keep it as-is.

MongoDB Atlas must allow Render connections. For first testing, add `0.0.0.0/0` in Atlas Network Access.

Only the configured admin account is seeded automatically. Doctors and patients are created through the application.
