# Vault — Cloud Drive App

[![CI](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml/badge.svg)](https://github.com/hamzalekhbioui/Cloud_Drive_App/actions/workflows/ci.yml)

A full-stack personal cloud storage application. Upload, organise, preview, and manage your files from any browser, backed by **Azure Blob Storage** and a **Spring Boot** REST API.

## AI file chat

PDF and DOCX files are processed asynchronously after upload. The backend extracts text, generates a summary and Azure OpenAI embeddings, stores chunks in PostgreSQL with `pgvector`, and exposes authenticated semantic-search-backed chat from the file preview.

Configure `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_API_VERSION`, `AZURE_OPENAI_CHAT_DEPLOYMENT`, and `AZURE_OPENAI_EMBEDDING_DEPLOYMENT` in `.env`. Use the `pgvector/pgvector:pg16` database image (as configured in `docker-compose.yml`). AI processing is disabled safely until all provider settings are present; unsupported formats and provider failures are reported in the file’s AI status.
