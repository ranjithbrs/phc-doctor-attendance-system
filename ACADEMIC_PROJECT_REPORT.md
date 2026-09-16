# ACADEMIC PROJECT REPORT & SYSTEM ARCHITECTURE
## Enterprise Primary Health Centre (PHC) Doctor Attendance & Health Surveillance System

---

### 📋 Project Abstract
The **PHC Doctor Attendance & Health Surveillance System** is an enterprise-grade, multi-tenant digital health governance platform designed to ensure medical officer presence across rural Primary Health Centres (PHCs). The platform combines real-time **Leaflet.js GPS Geofencing (500m radius)**, **Facial Liveness Selfie Verification**, **Velocity Spoof Scanners**, **PWA Offline IndexedDB Queueing**, **Chart.js Executive Dashboard Analytics**, **jsPDF Official Report Exports**, and **Emergency Epidemic SOS Duty Broadcasting**.

---

### 1. 📐 System Architecture Diagram

```mermaid
flowchart TD
    subgraph Client["Client Presentation Layer (PWA HTML5 / JS / Leaflet)"]
        A1[Doctor Mobile Portal - docDashboard.html]
        A2[District Admin Portal - ddhcDashboard.html]
        A3[IndexedDB Offline Queue & ServiceWorker]
    end

    subgraph Security["Security & Authentication Layer"]
        B1[BCrypt Password Hashing]
        B2[Dynamic Device ID Binding Lock-in]
        B3[Session Token Security]
    end

    subgraph Core["Spring Boot Business Logic Layer"]
        C1[Attendance & Geofence Engine]
        C2[Facial Liveness & Selfie Validator]
        C3[Teleportation & Velocity Scanner]
        C4[Leave Management & Exemption Engine]
        C5[Surveillance & Heartbeat Monitor]
        C6[Analytics & PDF/CSV Export Engine]
    end

    subgraph Database["Persistence Layer (H2 / MySQL)"]
        D1[(PHCs & Divisions)]
        D2[(Doctors & Roles)]
        D3[(Attendance Logs & Audits)]
        D4[(Leave Requests)]
    end

    Client <--> Security
    Security <--> Core
    Core <--> Database
```

---

### 2. 🗄️ Entity-Relationship Diagram (ERD)

```mermaid
erDiagram
    DIVISION ||--|{ PHC : contains
    PHC ||--|{ DOCTOR : employs
    DOCTOR ||--|{ ATTENDANCE : marks
    DOCTOR ||--|{ LEAVE_REQUEST : submits
    ATTENDANCE ||--|{ AUDIT_LOG : generates

    DIVISION {
        Long id PK
        String name
        String districtName
    }

    PHC {
        Long id PK
        String name
        Double latitude
        Double longitude
        Double geofenceRadiusMeters
        Long division_id FK
    }

    DOCTOR {
        Long id PK
        String name
        String email UK
        String password
        String role
        String registeredDeviceId
        Long phc_id FK
    }

    ATTENDANCE {
        Long id PK
        LocalDate date
        LocalTime checkInTime
        LocalTime checkOutTime
        String status
        Double latitude
        Double longitude
        Double livenessScore
        String photoProof
        Long doctor_id FK
    }

    LEAVE_REQUEST {
        Long id PK
        LocalDate startDate
        LocalDate endDate
        String leaveType
        String reason
        String status
        Long doctor_id FK
    }

    AUDIT_LOG {
        Long id PK
        LocalDateTime timestamp
        String action
        String verificationResult
        Double calculatedDistanceMeters
        Double accuracyMeters
    }
```

---

### 3. 🔄 Data Flow Diagram (DFD Level 1)

```mermaid
flowchart TD
    Doctor((Doctor User))
    Admin((District Admin))

    P1[Process 1.0: BCrypt Auth & Device Binding]
    P2[Process 2.0: Geofence & Liveness Mark-In]
    P3[Process 3.0: Velocity & Anomaly Scanner]
    P4[Process 4.0: Leave Request Workflow]
    P5[Process 5.0: District Analytics & PDF Export]

    D1[(Doctor DB)]
    D2[(Attendance DB)]
    D3[(Audit & Anomaly DB)]
    D4[(Leave DB)]

    Doctor -->|Credentials & Device ID| P1
    P1 <--> D1
    P1 -->|Session Token| Doctor

    Doctor -->|GPS Coords + Camera Selfie| P2
    P2 <--> D1
    P2 -->|Save Mark-In & Liveness| D2
    P2 -->|Log Audit Determination| D3

    P2 -->|Check Previous Mark-In Time & Distance| P3
    P3 -->|Flag Impossible Velocity >150km/h| D3

    Doctor -->|Submit Dates & Reason| P4
    P4 <--> D4
    Admin -->|Approve / Reject Leave| P4

    Admin -->|Query Analytics & Reports| P5
    P5 <--> D2
    P5 <--> D3
    P5 -->|Generate Formatted PDF / CSV| Admin
```

---

### 4. 🎭 UML Use Case Diagram

```mermaid
usecaseDiagram
    actor Doctor
    actor "District Admin (DDHS)" as Admin

    package "PHC Doctor Attendance & Health Surveillance System" {
        usecase "Authenticate & Bind Device" as UC1
        usecase "Mark Geofenced Attendance (Leaflet GPS)" as UC2
        usecase "Capture Facial Liveness Selfie" as UC3
        usecase "Continuous Presence Heartbeat Ping" as UC4
        usecase "Apply for Leave Exemption" as UC5
        usecase "View Real-Time Surveillance Feed" as UC6
        usecase "Inspect AI Anomaly & Velocity Audit Log" as UC7
        usecase "Approve / Reject Leave Requests" as UC8
        usecase "Export Official PDF & CSV Reports" as UC9
        usecase "Broadcast Emergency Outbreak SOS" as UC10
    }

    Doctor --> UC1
    Doctor --> UC2
    Doctor --> UC3
    Doctor --> UC4
    Doctor --> UC5

    Admin --> UC1
    Admin --> UC6
    Admin --> UC7
    Admin --> UC8
    Admin --> UC9
    Admin --> UC10
```

---

### 5. 🌟 Summary of Enterprise Features for Presentation

1. **Geofencing Engine**: Enforces strict physical presence within a configurable radius (default 500m) around designated PHCs using Leaflet.js and Haversine formula.
2. **Facial Liveness Verification**: Webcam selfie capture evaluating liveness score ($\ge 0.95$) to prevent photo-of-photo spoofing.
3. **Teleportation / Velocity Scanner**: Flags impossible physical movements (>150 km/h) between consecutive mark-ins across PHCs.
4. **BCrypt Security & Dynamic Device Binding**: Hashes passwords with BCrypt and locks logins to registered device IDs.
5. **PWA Offline ServiceWorker & IndexedDB Queue**: Permits attendance mark-in without internet connection, auto-syncing upon reconnection.
6. **Executive Chart.js Analytics**: Visual 7-day trend line charts and PHC compliance bar charts on DDHS Admin Dashboard.
7. **jsPDF Official Report Engine**: 1-click generation of formatted PDF and CSV audit reports complete with Government Directorate headers.
8. **AI Compliance Trust Scorecard**: Dynamic 0–100% security trust score computation displayed on doctor profiles.
9. **Emergency SOS Duty Broadcast**: One-click epidemic outbreak alert broadcast mode for district health officials.
