/**
 * PHC Doctor Attendance System - API Configuration
 * 
 * Automatically detects whether the application is running locally or deployed on GitHub Pages / remote server.
 * You can also manually override the API URL by executing in browser console:
 * localStorage.setItem("API_BASE_URL", "https://your-backend-domain.com");
 */
const DEFAULT_PROD_API_URL = "https://phc-doctor-attendance-system.onrender.com"; // Live Render backend

const isHttpsPage = window.location.protocol === "https:";
const isLocalHostname = 
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1";

// Safely clear HTTP overrides on HTTPS pages (prevents Mixed Content blocking)
let savedApiUrl = null;
try {
    savedApiUrl = localStorage.getItem("API_BASE_URL");
    if (savedApiUrl && isHttpsPage && savedApiUrl.startsWith("http://")) {
        console.warn("[PHC Attendance System] Cleared HTTP API override on HTTPS page to prevent browser Mixed Content block.");
        localStorage.removeItem("API_BASE_URL");
        savedApiUrl = null;
    }
} catch (e) {}

const API_BASE_URL = window.API_BASE_URL || (
    savedApiUrl || (isLocalHostname ? "http://localhost:8080" : DEFAULT_PROD_API_URL)
);

console.log("[PHC Attendance System] Connected API Base URL:", API_BASE_URL);

// ===== SAFE STORAGE HELPERS (Supports LocalStorage + SessionStorage Fallback) =====
function setSafeStorage(key, val) {
  try { localStorage.setItem(key, val); } catch (e) {}
  try { sessionStorage.setItem(key, val); } catch (e) {}
}

function getSafeStorage(key) {
  try {
    const val = localStorage.getItem(key);
    if (val !== null && val !== undefined && val !== "") return val;
  } catch (e) {}
  try {
    const val = sessionStorage.getItem(key);
    if (val !== null && val !== undefined && val !== "") return val;
  } catch (e) {}
  return null;
}

function clearSafeStorage() {
  try { localStorage.clear(); } catch (e) {}
  try { sessionStorage.clear(); } catch (e) {}
}
