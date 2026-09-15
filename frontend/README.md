# 📱 PHC Doctor Attendance System - Frontend Application

The frontend module is a modern, responsive web application built with **HTML5**, **Vanilla JavaScript (ES6+)**, and a custom **CSS3 Design System**, integrated with **Leaflet.js** for real-time OpenStreetMap geo-fencing.

---

## 📁 File Structure & Responsibilities

```
frontend/
├── index.html            # User Sign-In page (Doctor & Admin authentication)
├── register.html         # User Registration page (Doctor & Admin role setup)
├── docDashboard.html     # Doctor dashboard with Leaflet.js live map & check-in buttons
├── ddhcDashboard.html    # DDHS Central Monitoring dashboard with district overview
├── attendanceHistory.html# Date-filterable personal attendance log history table
├── style.css             # Comprehensive design system (variables, animations, glassmorphism)
├── config.js             # Environment-aware API base URL auto-resolver
└── README.md             # Frontend documentation
```

---

## 🎨 Design System & Visual Features

- **Design Tokens:** Predefined CSS custom properties (`:root`) for colors, shadows, border radii, and transitions (`style.css`).
- **Glassmorphism Elements:** Dynamic semi-transparent card overlays with backdrop blur.
- **Interactive Mapping:** Embedded Leaflet.js map displaying doctor GPS coordinates and a 500-meter radius geo-fence circle (`docDashboard.html`).
- **Micro-Animations:** Fluid button hover states, card elevation effects, and alert fade-ins.
- **Responsive Layout:** Adaptive CSS Grid and Flexbox structures supporting desktops, tablets, and mobile devices.

---

## 🌐 Dynamic API URL Resolver (`config.js`)

`config.js` automatically detects whether the application is running locally or deployed on GitHub Pages / a remote environment:

```javascript
const DEFAULT_PROD_API_URL = "https://phc-doctor-attendance-system.onrender.com";

const isLocalEnvironment = 
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1" ||
    window.location.protocol === "file:";

const API_BASE_URL = window.API_BASE_URL || (
    isLocalEnvironment
        ? "http://localhost:8080"
        : (localStorage.getItem("API_BASE_URL") || DEFAULT_PROD_API_URL)
);
```

### Manual Console Override
You can test custom backends directly from your browser console:
```javascript
localStorage.setItem("API_BASE_URL", "https://your-custom-backend.com");
location.reload();
```

---

## 🚀 Deployment (GitHub Pages)

This repository utilizes GitHub Actions (`.github/workflows/deploy.yml`) to automatically deploy the `./frontend` directory to GitHub Pages on every push to `main` or `master`.
