# Employee Leave Management System

This repository implements the approved Employee Leave Management MVP as a Java 21 Spring Boot modular monolith, PostgreSQL database, and React/TypeScript SPA.

## Local development

1. Copy `.env.example` to `.env` and replace the local database password.
2. Run `docker compose up -d postgres`.
3. Run `cd backend` and `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (`mvnw.cmd` on Windows).
4. Run `cd frontend`, `npm ci`, and `npm run dev`.
5. Open `http://localhost:5173`.

Vite proxies `/api` to port 8080. Production serves the SPA and API from one origin so the server-side session and CSRF cookie/header flow remain same-origin.

## Verification

- Backend: `backend/mvnw.cmd verify`
- Frontend: `npm run lint && npm run typecheck && npm test -- --run && npm run build`
- E2E scaffold: `cd e2e && npm test`

Database schema is managed exclusively by forward-only Flyway migrations. Hibernate uses validation and never creates production schema.

