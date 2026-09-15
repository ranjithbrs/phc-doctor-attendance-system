# 📚 Project Documentation & Resources

This directory contains reference documentation, workflow assets, and architectural guidelines for the **Primary Health Centre (PHC) Doctor Attendance & Geo-Fencing System**.

---

## 📄 Included Documentation Files

- **[Root Project README](../README.md)**: Main architecture summary, setup guide, system workflow diagram, API route reference, and ER database schema.
- **[Backend Service Guide](../backend/README.md)**: Spring Boot REST API setup, package layout, Haversine formula calculation details, and Docker deployment guide.
- **[Frontend App Guide](../frontend/README.md)**: HTML5/JS structure, CSS design system tokens, Leaflet.js mapping, and `config.js` API resolution mechanism.
- **[Deployment Manual](../DEPLOYMENT.md)**: Step-by-step instructions for deploying to GitHub Pages, Render, and Railway Cloud MySQL.

---

## 🎨 System Flow Summary

1. **Doctor Geo-Fence Check-In**:
   - Geolocation acquired via `navigator.geolocation.getCurrentPosition()`.
   - Leaflet map updates marker location and 500m radius circle.
   - `POST /attendance/checkin` payload sent to Spring Boot backend.
   - Backend calculates Haversine distance to assigned PHC.
   - Attendance marked as `PRESENT` (if \(\le 500\text{m}\)) or `ABSENT` (if \(> 500\text{m}\)).

2. **Check-Out Validation**:
   - Doctor submits check-out request via `PUT /attendance/checkout`.
   - Backend verifies active status is `PRESENT`.
   - Status updated to `COMPLETED` and check-out timestamp recorded.

3. **District Health Administration Surveillance**:
   - Administrators sign in with `ADMIN` role.
   - Access real-time monitoring cards and district PHC overview table (`/dashboard/summary/1` and `/dashboard/phc-overview/1`).
