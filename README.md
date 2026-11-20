# Clinic Platform — Multi-Tenant SaaS Medical Queue & Clinic Management System

A full-stack, multi-tenant SaaS platform for managing **solo-doctor multi-branch practices** and **multi-specialty medical centers**. It combines a **Hexagonal Architecture** Spring Boot backend, a **React** operational web dashboard, a **real-time WebSocket TV waiting room display**, and a **Flutter** patient mobile app.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [1. Environment Variables (`.env`)](#1-environment-variables-env)
  - [2. Run with Docker Compose](#2-run-with-docker-compose)
  - [3. Run Locally (Development)](#3-run-locally-development)
- [CI Pipeline](#ci-pipeline)

---

## Architecture Overview

The backend follows **Hexagonal Architecture (Ports & Adapters)** across a multi-module Maven build, keeping core business rules strictly isolated from Spring, JPA, and transport frameworks:

```text
┌──────────────────────────────────────────────────────────┐
│                     boot (Spring Boot)                   │
│       Application Entrypoint, Configuration & Wiring     │
└───────────────────────────┬──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│                     infrastructure                       │
│  Web Controllers (adapter/in/web)                        │
│  JPA Entities, Repositories & Adapters (adapter/out)     │
│  STOMP WebSocket Publisher, JWT Security, DataSeeder     │
└───────────────────────────┬──────────────────────────────┘
                            │ implements / calls ports
┌───────────────────────────▼──────────────────────────────┐
│                      application                         │
│  Use Cases (port/in), Outbound Ports (port/out)          │
│  Commands, Results, Application Services                 │
└───────────────────────────┬──────────────────────────────┘
                            │ depends on
┌───────────────────────────▼──────────────────────────────┐
│                        domain                            │
│  Pure Java Models, Enums & Domain Exceptions             │
│  (Zero external framework dependencies)                  │
└──────────────────────────────────────────────────────────┘
```

### Multi-Tenant SaaS Design
- **Tenant Isolation:** Organizations (`SOLO_DOCTOR` or `MEDICAL_CENTER`) own isolated branches (`clinics`), specialties, doctor profiles, clinic-doctor schedules, staff accounts, patients, appointments, and prescriptions.
- **Subscription Plans & Quotas:** Enforced at the domain level (`Organization`) for `TRIAL`, `BASIC`, `PRO`, and `ENTERPRISE` plans (max branches, max doctors, and monthly appointment quotas).
- **Platform Governance (`SUPER_ADMIN`):** Can activate/suspend organizations, manage subscription statuses, and reset organization owner passwords.

---

## Project Structure

```text
clinic-platform/
├── domain/                  # Pure Java domain models, enums & business transitions
├── application/             # Use-case ports (in/out), commands, results & services
├── infrastructure/          # REST controllers, JPA persistence, STOMP WS, JWT security
├── boot/                    # Spring Boot runner, application.yml & repackaged JAR
├── frontend/                # React 19 + Vite + Tailwind CSS v4 dashboard & TV screen
├── patient_app/             # Flutter mobile application for patients
├── .github/workflows/       # GitHub Actions CI pipeline (Maven + Frontend build)
├── Dockerfile               # Multi-stage backend build (Maven -> Temurin 17 JRE)
├── docker-compose.yml       # MySQL 8.0 + Backend + Nginx Frontend orchestration
└── pom.xml                  # Parent Maven POM
```

---

## Key Features

### 1. Multi-Branch & Multi-Specialty Clinic Management
- Supports **Solo Doctors** operating across multiple branches and **Medical Centers** with multiple specialties and doctors.
- Per-branch working days, booking open/close windows, doctor arrival time, clinic timezone handling (default `Africa/Cairo`), and manual emergency branch closure with custom reason.
- Configurable default pricing per doctor and optional price overrides per clinic branch.

### 2. Real-Time Queue & TV Waiting Room (`/tv`)
- Live queue updates pushed instantly via **WebSocket (STOMP over SockJS)** to the Receptionist dashboard, Doctor dashboard, Patient mobile app, and TV waiting room screens.
- **Single-Doctor TV Mode (`/tv?clinicId=X&doctorId=Y`):** Displays organization/branch name, doctor name, specialty, current queue number, waiting count, chime sound, and Arabic voice announcement.
- **Multi-Doctor TV Mode (`/tv?clinicId=X`):** Responsive multi-card grid displaying all doctors assigned to the branch side-by-side with live updates, visual turn highlights, and bell chime alerts.

### 3. Smart Queue Operations & Emergency Handling
- **Walk-in & Priority Emergency Queue:** Receptionists and doctors can add walk-in patients directly. Marking a case as **Emergency (`priority = true`)** places the patient at the front of the waiting queue without renumbering or disrupting existing tickets.
- **Skip & Requeue Flow (`SKIPPED`):** Absent patients can be skipped to keep the queue moving and easily requeued upon arrival from either the Receptionist or Doctor dashboard, while the patient app displays a clear orange alert directing them to reception.
- **Patient Self-Cancellation:** Patients can cancel their active `WAITING` or `SKIPPED` appointment from the mobile app with instant WebSocket queue synchronization.
- **Emergency Queue Cancellation:** Doctors/admins can cancel all remaining active appointments for the day in one action, displaying the closure reason to patients in the mobile app.

### 4. Visit Types & Revenue Analytics
- Supports **`EXAMINATION`** (new visit) and **`CONSULTATION`** (follow-up) at booking (mobile & walk-in) with separate per-doctor and per-branch pricing.
- Interactive **Analytics Dashboard** powered by Recharts showing expected vs. collected daily revenue, examination vs. consultation breakdown, booking sources (`ONLINE` vs. `WALK_IN`), and 7-day appointment trends.

### 5. Booking Protection, Security & Staff Lifecycle
- **Duplicate Booking Prevention:** Prevents a patient from holding multiple active tickets in the same clinic on the same day, returning their current queue number.
- **Per-Clinic Patient Blocking:** Doctors and receptionists can block abusive phone numbers from online booking per branch (and unblock anytime).
- **Expired History Cleanup (`NO_SHOW`):** Past-date `WAITING` or `SKIPPED` appointments automatically appear as `NO_SHOW` in patient booking history based on the clinic's local timezone.
- **Immediate Session Revocation:** Deactivating a staff member (`active = false`) or suspending an organization immediately rejects both logins and active JWT tokens in `JwtAuthenticationFilter` while preserving historical audit records.
- **Hierarchical Password Management:**
  - Any authenticated user can change their own password (`/api/auth/change-password`).
  - Clinic admins/doctors/receptionists can reset passwords for staff and patients with auto-generated temporary passwords (`PasswordGenerator`).
  - Walk-in patients can be upgraded to real portal accounts with randomly generated credentials.
  - `SUPER_ADMIN` can generate temporary passwords for organization owners.

### 6. Doctor EHR & Patient Mobile App
- **Doctor Dashboard:** Arabic-normalized patient search, full medical history sidebar, structured prescription builder, and direct visits without queue.
- **Flutter Patient App:** Organization & branch discovery (automatically hiding suspended tenants), real-time queue tracking with **QR e-ticket**, appointment booking with visit-type pricing, self-cancellation, appointment history (`NO_SHOW` aware), digital prescriptions, and profile/password management.

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.3.5, Spring Security, JWT (`jjwt` 0.12.5), Spring Data JPA / Hibernate, Spring WebSocket (STOMP + SockJS), Bean Validation, MySQL 8.0 |
| **Testing** | JUnit 5, AssertJ, Mockito |
| **Web Frontend** | React 19, Vite 8, Tailwind CSS v4, React Router v7, Axios, Recharts 3, Lucide React, `@stomp/stompjs`, `sockjs-client` |
| **Mobile App** | Flutter (Dart `^3.13.0`), `http`, `stomp_dart_client`, `shared_preferences`, `qr_flutter`, `intl` |
| **DevOps & Hosting** | Docker, Docker Compose, Nginx Alpine (SPA routing via `nginx.conf`), GitHub Actions CI |

---

## Getting Started

### 1. Environment Variables (`.env`)

Create a `.env` file in the root directory based on `.env.example` (never commit `.env` to Git):

| Variable | Required | Default | Description |
| :--- | :---: | :--- | :--- |
| `MYSQL_ROOT_PASSWORD` | Yes (Docker) | `root` | Root password for MySQL container and backend datasource |
| `SPRING_DATASOURCE_URL` | Optional | `jdbc:mysql://localhost:3306/clinic_db?...` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | Optional | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes (Local) | — | Database password |
| `JWT_SECRET` | **Yes** | — | Base64-encoded HMAC-SHA secret key (at least 256-bit) |
| `JWT_EXPIRATION` | Optional | `86400000` | JWT expiration in milliseconds (default: 24 hours) |
| `APP_SUPERADMIN_EMAIL` | Recommended | — | Initial `SUPER_ADMIN` username created if `users` table is empty |
| `APP_SUPERADMIN_PASSWORD` | Recommended | — | Initial `SUPER_ADMIN` password |
| `APP_SEED_ENABLED` | Optional | `false` | Set to `true` in dev to seed sample clinics, doctors, and patient |
| `SPRING_PROFILES_ACTIVE` | Optional | `prod` | Spring active profile (`dev` or `prod`) |

> **Security Note:** Before pushing or deploying to production, ensure `boot/src/main/resources/application.yml` reads `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, and `APP_SUPERADMIN_*` from environment placeholders rather than hardcoded values.

---

### 2. Run with Docker Compose

1. Create `.env` in the project root with the variables above (especially `JWT_SECRET`, `MYSQL_ROOT_PASSWORD`, `APP_SUPERADMIN_EMAIL`, `APP_SUPERADMIN_PASSWORD`).
2. Build and start all services (`mysql-db`, `backend`, `frontend`):
   ```bash
   docker compose up --build -d
   ```
3. Access the services:
   - **Web Dashboard & TV Screen:** `http://localhost` (served via Nginx with SPA fallback)
   - **Backend API:** `http://localhost:8080/api`
   - **MySQL:** `localhost:3306`

---

### 3. Run Locally (Development)

#### Prerequisites
- **JDK 17+** & **Maven 3.9+**
- **MySQL 8.0** running locally
- **Node.js 20+** & **npm**
- **Flutter SDK** (for the mobile app)

#### A. Backend (Spring Boot)
```bash
# Build all modules and run unit tests
mvn clean install

# Export required environment variables (Windows PowerShell example)
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
$env:JWT_SECRET="your_base64_256bit_secret"
$env:APP_SEED_ENABLED="true"
$env:APP_SUPERADMIN_EMAIL="superadmin"
$env:APP_SUPERADMIN_PASSWORD="ChangeMeStrong123!"

# Run the boot module
mvn -pl boot spring-boot:run
```

#### B. Web Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
- Runs on `http://localhost:5173` (uses `VITE_API_URL` if set, defaulting to `http://localhost:8080`).

#### C. Patient Mobile App (Flutter)
```bash
cd patient_app
flutter pub get

# Android Emulator (defaults to 10.0.2.2:8080)
flutter run

# Physical Device (pass your machine's LAN IP)
flutter run --dart-define=API_HOST=192.168.1.100:8080
```

---

## CI Pipeline

GitHub Actions (`.github/workflows/ci.yml`) automatically runs on every push/PR to `main` or `master`:
- **Backend:** Sets up JDK 17 (Temurin) and runs `mvn clean install` (including unit tests across `domain` and `application`).
- **Frontend:** Sets up Node.js 20, installs dependencies, and runs `npm run build`.