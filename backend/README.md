# ⚙️ PHC Doctor Attendance System - Backend Service

The backend module is a production-ready **Spring Boot 3** REST API written in **Java 21**, managing authentication, geo-fenced attendance checks, and district health surveillance analytics.

---

## 🛠️ Architecture & Core Components

- **Framework:** Spring Boot 3.5.x
- **Language:** Java 21 (Eclipse Temurin JDK)
- **Data Layer:** Spring Data JPA + Hibernate ORM
- **Database:** MySQL 8.0+
- **Build Tool:** Apache Maven (`pom.xml`)
- **Containerization:** Multi-stage `Dockerfile`

---

## 📁 Package Structure

```
backend/phcbackend/src/main/java/com/ranjith/phcbackend/
├── PhcbackendApplication.java     # Main Spring Boot application entrypoint
├── controller/
│   ├── AuthController.java        # Handles /auth/login, /auth/register, /auth/phcs
│   ├── AttendanceController.java  # Handles /attendance/checkin, checkout, status, history
│   └── DashboardController.java   # Handles /dashboard/summary, phc-overview
├── model/
│   ├── Doctor.java                # JPA Entity for doctors and administrators
│   ├── PHC.java                   # JPA Entity for Primary Health Centres & GPS coords
│   ├── Attendance.java            # JPA Entity for daily attendance logs
│   └── Division.java              # JPA Entity for district medical divisions
├── repository/
│   ├── DoctorRepository.java      # JPA Repository interface for Doctor entity
│   ├── PHCRepository.java         # JPA Repository interface for PHC entity
│   ├── AttendanceRepository.java  # JPA Repository interface for Attendance entity
│   └── DivisionRepository.java    # JPA Repository interface for Division entity
└── service/
    ├── AttendanceService.java     # Core Haversine geo-fencing & status transition logic
    ├── AuthService.java           # Authentication, user creation & PHC listing
    └── DashboardService.java      # Attendance aggregation & percentage calculation
```

---

## 📐 Geo-Fencing Logic (Haversine Formula)

When a doctor submits a check-in request via `POST /attendance/checkin`:
1. The backend retrieves the assigned PHC building's latitude and longitude.
2. The `AttendanceService` calculates the great-circle distance between the live doctor GPS coordinates and the assigned PHC using the Haversine formula:
   \[
   a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1) \cos(\phi_2) \sin^2\left(\frac{\Delta \lambda}{2}\right)
   \]
   \[
   c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right), \quad d = R \cdot c \quad (\text{where } R = 6,371,000 \text{ m})
   \]
3. If distance \(d \le 500\text{ meters}\): Status is set to **`PRESENT`** and check-in time recorded.
4. If distance \(d > 500\text{ meters}\): Status is set to **`ABSENT`** with an explanatory distance warning.

---

## ⚙️ Environment Variables

The application can be configured using environment variables in production (e.g., Docker / Render / Railway):

| Environment Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC connection string | `jdbc:mysql://localhost:3306/phc_db` |
| `SPRING_DATASOURCE_USERNAME` | MySQL database user | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL database password | `secret` |
| `SERVER_PORT` | HTTP Server Port | `8080` |

---

## 🚀 Running & Building Locally

### Run via Maven Wrapper
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### Build Executable JAR
```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```
The compiled JAR will be located at `target/phcbackend-0.0.1-SNAPSHOT.jar`.

### Run via Docker
```bash
# Build image
docker build -t phc-backend .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/phc_db" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="password" \
  phc-backend
```
