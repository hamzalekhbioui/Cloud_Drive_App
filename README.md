# Vault — Cloud Drive App

[![CI](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml/badge.svg)](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml)

A full-stack personal cloud storage application. Upload, organise, preview, and manage your files from any browser, backed by **Azure Blob Storage** and a **Spring Boot** REST API.

## Development checks

The repository CI workflow runs backend tests on Temurin Java 17 and frontend tests/builds on Node.js 22. Run the same checks locally:

```bash
./mvnw --batch-mode test
cd frontend && npm ci && npm test -- --run && npm run build
```
