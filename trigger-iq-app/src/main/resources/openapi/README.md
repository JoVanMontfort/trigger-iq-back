# TriggerIQ OpenAPI Specification

This directory contains the OpenAPI 3.0 definitions for the TriggerIQ Sentiment API, which allows user profiles to be linked to sentiment analysis results across multiple channels and sources.

---

## 📂 Directory Structure

```
openapi/
├── v1/
│   ├── index.yaml                    # Main OpenAPI entry point
│   ├── paths/
│   │   └── users/
│   │       └── user-sentiment.yaml  # All operations for /users/{userId}/sentiment
│   ├── components/
│   │   ├── schemas/                 # Data models
│   │   ├── parameters/              # Reusable path/query/header params
│   │   ├── responses/               # Standardized API responses
│   │   └── requestBodies/           # Reusable request body definitions
```

---

## 🧩 Main Entry File

The main OpenAPI document is `index.yaml`. It references modular files via `$ref` to keep things clean and maintainable.

## 🧪 How to Validate

Use:

- Swagger Editor
- IntelliJ with OpenAPI plugin
- `swagger-cli validate v1/index.yaml`

## 🚀 How to Use

Serve with Swagger UI, Redoc, or import in Postman.

## 👥 Contribution Guidelines

1. Keep changes modular.
2. Use `$ref` for reuse.
3. Validate before commit.
4. Update `index.yaml` for new endpoints.

## 📌 Versioning

Specs are under `v1/`. Future versions go in separate folders.

## 📧 Contact

TriggerIQ API team or GitHub Issues.
