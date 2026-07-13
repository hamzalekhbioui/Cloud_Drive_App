# Vault — Cloud Drive App

[![CI](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml/badge.svg)](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml)

A full-stack personal cloud storage application. Upload, organise, preview, and manage your files from any browser, backed by **Azure Blob Storage** and a **Spring Boot** REST API.

---


```

---

## Database Schema

Three tables are created automatically by Flyway on first run:

- **`users`** — `id`, `email`, `password`, `name`, `created_at`, `last_login`
- **`files`** — `id`, `original_file_name`, `blob_file_name`, `url`, `size`, `type`, `user_id`, `created_at`, `starred`, `deleted_at`
- **`user_settings`** — `id`, `user_id`, `dark_mode`, `density`, `notification flags`, `default_view`, `default_sort`, `api_token`, `updated_at`
