# Vault — Cloud Drive App

[![CI](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml/badge.svg)](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml)

A full-stack personal cloud storage application. Upload, organise, preview, and manage your files from any browser, backed by **Azure Blob Storage** and a **Spring Boot** REST API.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Clone the repository](#1-clone-the-repository)
  - [2. Configure environment variables](#2-configure-environment-variables)
  - [3. Run the backend](#3-run-the-backend)
  - [4. Run the frontend](#4-run-the-frontend)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)

---

## Features

- **Authentication** — Email/password registration & login, plus **Google OAuth 2.0** sign-in
- **File Management** — Upload (up to 100 MB), list, stream/preview, rename via star
- **Starred Files** — Mark important files for quick access
- **Trash** — Soft-delete files and restore them, or permanently delete
- **Analytics Dashboard** — Storage overview, breakdown by file type, largest files, daily upload activity, and smart storage insights
- **Settings** — Update profile, change password, toggle dark mode, adjust display preferences, manage an API token
- **Swagger UI** — Interactive API documentation available out of the box

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 4, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL, Flyway migrations |
| **Cloud Storage** | Azure Blob Storage |
| **Auth** | JWT (jjwt 0.12), Google OAuth 2.0 |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger UI |
| **Monitoring** | Spring Boot Actuator, Logstash Logback encoder |
| **Frontend** | React 19, TypeScript, Vite |
| **Routing** | React Router v7 |
| **HTTP Client** | Axios |
| **Charts** | Recharts |
| **Animations** | Framer Motion |

---

## Architecture

```
┌─────────────────────────────────┐
│        React + Vite (port 5173) │  ← SPA frontend
└────────────────┬────────────────┘
                 │ REST / JSON (JWT)
┌────────────────▼────────────────┐
│  Spring Boot API (port 8081)    │  ← Backend
│  Spring Security · JPA · Flyway │
└──────┬────────────────┬─────────┘
       │                │
┌──────▼──────┐  ┌──────▼──────────────┐
│ PostgreSQL  │  │  Azure Blob Storage  │
└─────────────┘  └──────────────────────┘
```

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+** (or use the included `./mvnw` wrapper)
- **Node.js 18+** and **npm**
- **PostgreSQL** database
- **Azure Blob Storage** account and container

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/hamzalekhbioui/Cloud_Drive_App.git
cd Cloud_Drive_App
```

### 2. Configure environment variables

**Backend** — copy the example file and fill in your values:

```bash
cp .env.example .env
```

**Frontend** — copy the example file and fill in your Google OAuth Client ID:

```bash
cp frontend/.env.example frontend/.env
```

See the [Environment Variables](#environment-variables) section for details on each variable.

### 3. Run the backend

Source the `.env` file so the variables are available to Spring Boot, then start the server:

```bash
# Linux / macOS
set -a && source .env && set +a
./mvnw spring-boot:run

# Windows (PowerShell)
# Set each variable manually, then:
./mvnw.cmd spring-boot:run
```

The API will be available at `http://localhost:8081`.  
Swagger UI: `http://localhost:8081/swagger-ui.html`

> **Note:** Flyway runs automatically on startup and creates the required tables.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

---

## Environment Variables

### Backend (`.env`)

| Variable | Description | Default |
|---|---|---|
| `AZURE_STORAGE_CONNECTION_STRING` | Azure Blob Storage connection string | — (required) |
| `AZURE_STORAGE_CONTAINER_NAME` | Blob container name | `files` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/clouddrive` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | — (required) |
| `JWT_SECRET` | Secret key for signing JWTs (min. 32 chars) | — (required) |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds | `86400000` (24 h) |
| `SERVER_PORT` | Port the API listens on | `8081` |

### Frontend (`frontend/.env`)

| Variable | Description |
|---|---|
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth 2.0 Client ID (from Google Cloud Console) |

---

## API Reference

Interactive documentation is served by Swagger UI at `/swagger-ui.html` when the backend is running.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new account |
| `POST` | `/api/auth/login` | Log in with email & password |
| `POST` | `/api/auth/google` | Sign in with a Google access token |
| `POST` | `/api/files/upload` | Upload a file |
| `GET` | `/api/files/me` | List the authenticated user's files |
| `GET` | `/api/files/starred` | List starred files |
| `GET` | `/api/files/trash` | List trashed files |
| `GET` | `/api/files/{id}/stream` | Stream / preview a file |
| `PATCH` | `/api/files/{id}/star` | Toggle starred status |
| `DELETE` | `/api/files/{id}` | Soft-delete (move to trash) |
| `POST` | `/api/files/{id}/restore` | Restore from trash |
| `DELETE` | `/api/files/{id}/permanent` | Permanently delete a file |
| `GET` | `/api/analytics/overview` | Storage totals and usage percentage |
| `GET` | `/api/analytics/breakdown` | Bytes used per file-type category |
| `GET` | `/api/analytics/largest-files` | Top 10 files by size |
| `GET` | `/api/analytics/activity` | Daily upload activity (last 30 days) |
| `GET` | `/api/analytics/insights` | Smart storage insights |
| `GET` | `/api/settings` | Retrieve all settings |
| `PUT` | `/api/settings/profile` | Update display name |
| `PUT` | `/api/settings/password` | Change password |
| `PUT` | `/api/settings/preferences` | Update UI preferences |
| `POST` | `/api/settings/api-token` | Regenerate API token |
| `GET` | `/actuator/health` | Health probe |

All endpoints except `/api/auth/**` and `/actuator/health` require a `Bearer <JWT>` header.

---

## Project Structure

```
Cloud_Drive_App/
├── src/main/
│   ├── java/com/cloud/drive/
│   │   ├── CloudDriveApplication.java
│   │   ├── config/          # Security, Azure Blob configuration
│   │   ├── controller/      # REST controllers (Auth, File, Analytics, Settings)
│   │   ├── dto/             # Request/response data transfer objects
│   │   ├── exception/       # Global exception handler
│   │   ├── model/           # JPA entities (User, FileEntity, UserSettings)
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JWT filter, JwtUtil, UserDetailsService
│   │   └── service/         # Business logic (Auth, File, Analytics, Settings, BlobStorage)
│   └── resources/
│       ├── application.properties
│       ├── db/migration/    # Flyway SQL migrations
│       └── logback-spring.xml
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios API client modules
│   │   ├── components/      # Shared UI components (AppShell, FileRow, Modal…)
│   │   ├── context/         # React contexts (Auth, Theme)
│   │   ├── pages/           # Route-level page components
│   │   └── utils/
│   ├── .env.example
│   └── package.json
├── .env.example
└── pom.xml
```

---

## Database Schema

Three tables are created automatically by Flyway on first run:

- **`users`** — `id`, `email`, `password`, `name`, `created_at`, `last_login`
- **`files`** — `id`, `original_file_name`, `blob_file_name`, `url`, `size`, `type`, `user_id`, `created_at`, `starred`, `deleted_at`
- **`user_settings`** — `id`, `user_id`, `dark_mode`, `density`, `notification flags`, `default_view`, `default_sort`, `api_token`, `updated_at`
