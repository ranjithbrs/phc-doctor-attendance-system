/**
 * PHC Doctor Attendance System - API Configuration
 * 
 * Automatically detects whether the application is running locally or deployed on GitHub Pages / remote server.
 * You can also manually override the API URL by executing in browser console:
 * localStorage.setItem("API_BASE_URL", "https://your-backend-domain.com");
 */
const DEFAULT_PROD_API_URL = "https://phc-doctor-attendance-system.onrender.com"; // Live Render backend

const isLocalEnvironment = 
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1" ||
    window.location.protocol === "file:";

const API_BASE_URL = window.API_BASE_URL || (
    isLocalEnvironment
        ? "http://localhost:8080"
        : (localStorage.getItem("API_BASE_URL") || DEFAULT_PROD_API_URL)
);

console.log("[PHC Attendance System] Connected API Base URL:", API_BASE_URL);
