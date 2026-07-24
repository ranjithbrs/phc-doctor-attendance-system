# 🚀 PHC Doctor Attendance System - Deployment Guide

This document provides step-by-step instructions on how to deploy both the **Frontend** (GitHub Pages) and the **Backend** (Spring Boot + MySQL).

---

## 📁 1. Frontend Deployment (GitHub Pages)

### Option A: Automatic Deployment via GitHub Actions (Recommended)
This repository includes a GitHub Actions workflow (`.github/workflows/deploy.yml`) that automatically deploys the frontend whenever you push to `main` or `master`.

1. **Push your code to GitHub**:
   ```bash
   git add .
   git commit -m "Configure frontend for Git deployment"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
   git push -u origin main
   ```
2. **Enable GitHub Pages**:
   - Go to your repository on GitHub.
   - Click **Settings** -> **Pages**.
   - Under **Build and deployment -> Source**, select **GitHub Actions**.
   - Your site will automatically build and publish at `https://YOUR_USERNAME.github.io/YOUR_REPOSITORY/`.

### Option B: Deploying directly from Main Branch
If you prefer not to use GitHub Actions:
- Go to **Settings** -> **Pages**.
- Under **Source**, select `Deploy from a branch`.
- Select `main` branch and folder `/ (root)`.
- Click **Save**. The included root `index.html` will automatically forward visitors to the frontend application.

---

## ⚙️ 2. Connecting Frontend to Live Backend (`config.js`)

By default, the application runs on `http://localhost:8080` when tested locally. When deployed to GitHub Pages, it connects to your live backend.

To configure your live backend API URL:
Open `frontend/config.js` and edit:
```javascript
const DEFAULT_PROD_API_URL = "https://your-backend-api.onrender.com";
```
*Tip: You can also temporarily override the API URL directly in your browser console:*
```javascript
localStorage.setItem("API_BASE_URL", "https://your-backend-api.onrender.com");
```

---

## ☕ 3. Backend Deployment (Spring Boot + MySQL)

### Free/Cheap Hosting Options:
- **Render.com** (Web Service + Managed PostgreSQL / MySQL)
- **Railway.app** (Spring Boot Service + MySQL Database)
- **Koyeb.com** (Dockerized Spring Boot container)

### Step 1: Database Setup
1. Create a MySQL database on your cloud provider (e.g. Railway or Aiven).
2. Note the Connection URL, Username, and Password.

### Step 2: Environment Variables for Backend
Configure the following environment variables in your deployment platform settings:

| Variable | Example Value |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://your-db-host:3306/phc_db?useSSL=false` |
| `SPRING_DATASOURCE_USERNAME` | `your_db_user` |
| `SPRING_DATASOURCE_PASSWORD` | `your_db_password` |
| `SERVER_PORT` | `8080` |

### Step 3: Build & Deploy
- For Maven build: `mvn clean package -DskipTests`
- Run Jar command: `java -jar target/phcbackend-0.0.1-SNAPSHOT.jar`

---

## 🧪 4. Testing Default Login Credentials

Initial test accounts (once seeded into DB):

| Role | Email | Password | Access |
| :--- | :--- | :--- | :--- |
| **Doctor** | `doctor@phc.gov.in` | `doc123` | Doctor Check-In / History |
| **Admin (DDHS)** | `admin@phc.gov.in` | `admin123` | Central DDHS Monitoring Dashboard |
