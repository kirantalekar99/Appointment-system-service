# Appointment System Service

Spring Boot appointment system with MongoDB, OTP email, admin management, doctor/patient appointment flows, and static frontend pages served by Spring Boot.

## Local Run

Fill your local private values in `backend/src/main/resources/application.properties`.
This file is ignored by Git, so your MongoDB and Brevo secrets stay only on your PC.

Then run:

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
PORT=10000
SERVER_PORT=10000
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
```

MongoDB Atlas must allow Render connections. For first testing, add `0.0.0.0/0` in Atlas Network Access.
