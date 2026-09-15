# ⚙️ PHC Doctor Attendance System - Backend Service

The backend module is a production-ready **Spring Boot 3** REST API written in **Java 21**, managing BCrypt authentication, Haversine geo-fencing, continuous presence heartbeat verification, anti-spoofing heuristics, offline batch synchronization, and district health surveillance analytics.

---

## 🛠️ Architecture & Core Components

- **Framework:** Spring Boot 3.5.x
- **Language:** Java 21 (Eclipse Temurin JDK)
- **Data Layer:** Spring Data JPA + Hibernate ORM
- **Database:** Aiven Cloud MySQL 8.x / Local MySQL 8.0+
- **Build Tool:** Apache Maven (`pom.xml`)
- **Containerization:** Multi-stage `Dockerfile`
- **Security:** BCrypt Password Hashing + Token Session Management

---

## 📁 Package Structure

```
backend/phcbackend/src/main/java/com/ranjith/phcbackend/
├── PhcbackendApplication.java     # Main Spring Boot application entrypoint
├── controller/
│   ├── AuthController.java        # Handles /auth/login, /auth/register, /auth/phcs
│   ├── AttendanceController.java  # Handles /attendance checkin, checkout, presence-ping, audit, alerts, offline-sync, anomalies
│   └── DashboardController.java   # Handles /dashboard/summary, phc-overview
├── model/
│   ├── Doctor.java                # JPA Entity for doctors and administrators
│   ├── PHC.java                   # JPA Entity for Primary Health Centres & configurable radius
│   ├── Attendance.java            # JPA Entity for daily attendance logs & presence pings
│   ├── AttendanceAuditLog.java   # JPA Entity for detailed GPS audit evidence
│   └── Division.java              # JPA Entity for district medical divisions
├── repository/
│   ├── DoctorRepository.java      # JPA Repository interface for Doctor entity
│   ├── PHCRepository.java         # JPA Repository interface for PHC entity
│   ├── AttendanceRepository.java  # JPA Repository interface for Attendance entity
│   ├── AttendanceAuditLogRepository.java # JPA Repository interface for AttendanceAuditLog entity
│   └── DivisionRepository.java    # JPA Repository interface for Division entity
├── security/
│   └── SecurityUtil.java          # BCrypt password hashing & session token generator
└── service/
    ├── AttendanceService.java     # Haversine engine, anti-spoofing velocity, offline sync & anomaly scanner
    ├── AuthService.java           # Authentication, password verification & PHC listing
    └── DashboardService.java      # Attendance aggregation & percentage calculation
```

---

## 📐 Geo-Fencing & Verification Logic

When a doctor submits a check-in request via `POST /attendance/checkin`:
1. The backend retrieves the assigned PHC building's latitude, longitude, and custom radius (`phc.getRadiusMeters()`).
2. The `AttendanceService` calculates the great-circle distance between doctor GPS coordinates and PHC coordinates using the Haversine formula:
   \[
   a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1) \cos(\phi_2) \sin^2\left(\frac{\Delta \lambda}{2}\right)
   \]
   \[
   c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right), \quad d = R \cdot c \quad (\text{where } R = 6,371,000 \text{ m})
   \]
3. **Anti-Spoofing & Velocity Evaluation**: Evaluates travel speed between recent fixes. If velocity exceeds 250 km/h for fixes >500m apart within 4 hours, the request is flagged as `FLAGGED_IMPOSSIBLE_SPEED` and rejected.
4. **Distance Check**:
   - If distance \(d \le \text{phc.getRadiusMeters()}\): Status is set to **`PRESENT`** (or **`LATE`** if checked in after 09:15 AM grace cutoff).
   - If distance \(d > \text{phc.getRadiusMeters()}\): Status is set to **`ABSENT`**.
5. **Audit Trail**: Every check-in, check-out, and ping attempt persists an `AttendanceAuditLog` entry.

---

## ⚙️ Environment Variables

The application can be configured using environment variables in production (e.g. Render / Docker):

| Environment Variable | Description | Example Value |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC connection string | `jdbc:mysql://mysql-1f6018f4-br3843311-c379.l.aivencloud.com:10531/defaultdb?sslMode=REQUIRED` |
| `SPRING_DATASOURCE_USERNAME` | MySQL database user | `avnadmin` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL database password | `secret_password` |
| `PORT` | Dynamic HTTP Server Port | `10000` |

---

## 🧪 Automated Unit Testing (JUnit 5 + Mockito)

```bash
cd backend/phcbackend
.\mvnw.cmd test
```
- **15 / 15 Tests Passing (`BUILD SUCCESS`)** covering check-in validation, accuracy thresholds, presence pings, anti-spoofing velocity checks, absentee alerts, configurable PHC radii, shift grace period rules, offline batch sync, and anomaly detection.
