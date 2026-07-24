# 🏥 Primary Health Centre (PHC) Doctor Attendance & Monitoring System

A modern, full-stack, geo-fenced web application designed for central medical administration and doctor attendance monitoring across Primary Health Centres (PHCs).

---

## 🌐 Live Demo & Deployment Links

| Layer | Hosting Provider | Live URL |
| :--- | :--- | :--- |
| **Frontend Web App** | GitHub Pages | [https://ranjithbrs.github.io/phc-doctor-attendance-system/](https://ranjithbrs.github.io/phc-doctor-attendance-system/) |
| **Backend REST API** | Render (Docker) | [https://phc-doctor-attendance-system.onrender.com](https://phc-doctor-attendance-system.onrender.com) |
| **Database** | Railway MySQL | Cloud Managed MySQL Instance |

---

## 🔑 Demo Test Credentials

You can test both role-based workflows using the following default seed accounts:

### 1. 👨‍⚕️ Doctor Login
* **Email:** `doctor@phc.gov.in`
* **Password:** `doc123`
* **Features:** Live GPS Location & 500m Geo-Fence Map, Geo-fenced Check-In / Check-Out, Attendance History filtering.

### 2. 🏛️ Admin / DDHS Dashboard
* **Email:** `admin@phc.gov.in`
* **Password:** `admin123`
* **Features:** District Health Surveillance Overview, Total PHCs & Doctors summary cards, Real-time Attendance % tables.

*Alternatively, click **"Create Account"** on the login page to register a new Doctor or Admin account!*

---

## ✨ Key Features & Capabilities

- 📍 **Geo-Fencing Attendance Check-In**: Uses Haversine mathematical distance calculation to verify doctor proximity (within 500 meters radius of assigned PHC).
- 🗺️ **Interactive Leaflet.js Map**: Real-time map displaying current doctor location and 500m radius geo-fence circle.
- 🔐 **Role-Based Authentication & Registration**: Secure access for both Doctors and Health Administrators (DDHS).
- 📊 **Central DDHS Administration Dashboard**: High-level statistical summaries (Total PHCs, Doctors, Present/Absent count, Attendance %).
- 📅 **Attendance History**: Date-filtered personal attendance logs with check-in/check-out timestamps and status badges.
- 🎨 **Modern Responsive UI**: Built with custom design tokens, smooth micro-animations, glassmorphism elements, and mobile-responsive layouts.

---

## 🛠️ Technology Stack

### Frontend
- **HTML5 & Vanilla JavaScript (ES6+)**
- **Vanilla CSS3 Design System** (Variables, Glassmorphism, Responsive Grid)
- **Leaflet.js** (Interactive OpenStreetMap Integration)
- **GitHub Actions & Pages** (CI/CD Automated Deployment)

### Backend
- **Java 21**
- **Spring Boot 3** (REST Web Services, Spring Data JPA, Hibernate)
- **Maven** (Dependency & Project Management)
- **Docker** (Multi-stage build runtime deployment on Render)

### Database
- **MySQL Database** (Hosted on Railway Cloud)

---

## 📁 Repository Project Structure

```
phc-doctor-attendance-system/
├── index.html                      # Root redirect for GitHub Pages
├── .github/workflows/
│   └── deploy.yml                  # GitHub Actions deployment workflow
├── frontend/
│   ├── index.html                  # Sign-In page
│   ├── register.html               # Registration / Sign-Up page
│   ├── docDashboard.html           # Doctor dashboard & Geo-fence map
│   ├── ddhcDashboard.html          # DDHS Admin monitoring dashboard
│   ├── attendanceHistory.html      # Attendance log & date filter
│   ├── style.css                   # Global design stylesheet
│   └── config.js                   # Dynamic environment API URL resolver
└── backend/
    └── phcbackend/
        ├── Dockerfile              # Multi-stage Docker deployment config
        ├── pom.xml                 # Maven configuration & Java 21 dependencies
        └── src/main/java/com/ranjith/phcbackend/
            ├── controller/         # AuthController, AttendanceController, DashboardController
            ├── model/              # Doctor, PHC, Attendance, Division entities
            ├── repository/         # JPA Repositories
            └── service/            # AttendanceService (Haversine logic), AuthService
```

---

## 🚀 Local Development Setup

### Prerequisites
- JDK 21
- Maven 3.x
- MySQL Server

### 1. Database Setup
Create a local database named `phc_db`:
```sql
CREATE DATABASE phc_db;
```

### 2. Backend Setup
Navigate to the backend directory:
```bash
cd backend/phcbackend
```

Configure `src/main/resources/application.properties` with your database credentials, then run:
```bash
./mvnw spring-boot:run
```
Backend will start on `http://localhost:8080`.

### 3. Frontend Setup
Open `frontend/index.html` in your web browser directly or serve it using any HTTP server (e.g. Live Server). `frontend/config.js` will automatically detect `localhost` and switch to `http://localhost:8080`.

---

## 📝 License
Distributed under the MIT License.
