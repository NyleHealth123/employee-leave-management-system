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

- Backend (Java 21): `cd backend && ./mvnw verify` (`mvnw.cmd` on Windows). This runs unit, contract, security, PostgreSQL/Testcontainers integration, and ArchUnit six-layer boundary tests.
- Frontend: from `frontend`, run `npm ci`, `npm run lint`, `npm run typecheck`, `npm test -- --run`, and `npm run build`.
- E2E: with Docker/PostgreSQL, the `local-demo` backend, and the frontend running, install the Playwright browser with `cd e2e && npm ci && npx playwright install chromium`, then run `npm test -- tests/leave-management-smoke.spec.ts`.

The local-demo/E2E stack requires these environment variable names: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DEMO_ADMIN_PASSWORD_HASH`, `DEMO_MANAGER_PASSWORD_HASH`, `DEMO_EMPLOYEE_PASSWORD_HASH`, `LOCAL_DEMO_RESET_ENABLED`, `LOCAL_DEMO_EXPECTED_DATABASE`, `E2E_API_BASE_URL`, `E2E_WEB_BASE_URL`, `E2E_ADMIN_LOGIN`, `E2E_ADMIN_PASSWORD`, `E2E_MANAGER_LOGIN`, `E2E_MANAGER_PASSWORD`, `E2E_EMPLOYEE_LOGIN`, `E2E_EMPLOYEE_PASSWORD`, `E2E_OUT_OF_SCOPE_REQUEST_ID`, `E2E_APPROVAL_DATE`, and `E2E_REJECTION_DATE`. Supply values through the local environment only; never commit credentials or hashes.

`.github/workflows/verify.yml` is the hosted verification path. Its existing backend and frontend jobs run the complete checks, then its E2E job uses Docker, generates masked ephemeral demo credentials and compatible bcrypt hashes, starts the profile-isolated stack, waits for both services, and executes the same Playwright smoke test. Production configuration is not used or changed.

Database schema is managed exclusively by forward-only Flyway migrations. Hibernate uses validation and never creates production schema.
