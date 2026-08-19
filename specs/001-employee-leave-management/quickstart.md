# Quickstart and Validation Guide

This is the target local run and acceptance workflow for the planned implementation. The commands become runnable after `$speckit-tasks` and `$speckit-implement` create the application; this planning step does not scaffold or implement them.

## Prerequisites

- Java 21
- Docker with Compose support for the local PostgreSQL service
- Node.js 24 LTS and npm
- Git

Maven is invoked through the checked-in Maven Wrapper, so a separate Maven installation is optional.

## Expected local configuration

| Component | Default | Notes |
|---|---|---|
| PostgreSQL | `localhost:5432` | Started by root `compose.yaml`; credentials supplied by local environment file |
| Backend | `http://localhost:8080` | Spring profile `local`; runs Flyway migrations on startup |
| Frontend | `http://localhost:5173` | Vite proxies `/api` to backend and uses strict fixed port |
| Production route model | One origin | SPA and `/api` share an origin; session cookies and CSRF remain enabled |

Create a local environment file from a committed example and set database credentials plus local-only demo account passwords. The real environment file must remain ignored. Demo seeding is enabled only by the `local-demo` profile/location and must fail closed outside that profile.

## Start the application

From the repository root:

```powershell
docker compose up -d postgres
```

Start the backend in one terminal:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local-demo
```

Start the frontend in another terminal:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. The frontend should obtain a CSRF token, establish a session through `/api/auth/login`, and route the signed-in user to a dashboard permitted by their roles.

On macOS/Linux, use `./mvnw` in place of `.\mvnw.cmd`.

## Automated verification

### Backend

```powershell
Set-Location backend
.\mvnw.cmd verify
```

The verification phase must run:

- Java/domain unit tests;
- Spring Security and MockMvc API tests;
- PostgreSQL Testcontainers migration/repository/transaction tests;
- API contract validation against `../specs/001-employee-leave-management/contracts/openapi.yaml`;
- architecture checks preventing controller-to-repository shortcuts and cross-module dependency violations.

### Frontend

```powershell
Set-Location frontend
npm ci
npm run lint
npm run typecheck
npm test -- --run
npm run build
```

Component/integration tests use MSW at the REST boundary and cover authenticated routing, roles, forms, dashboard states, error problems, cancellation availability, and manager/admin actions.

### End-to-end smoke tests

With PostgreSQL, backend, and frontend running:

```powershell
Set-Location e2e
npm ci
npm test
```

The smoke suite uses local-demo accounts and cleans or recreates its test data. It must not depend on production credentials.

## Required acceptance scenarios

### 1. Login and role boundaries

1. Sign in as each demo role and verify `/api/auth/me` returns only assigned roles and profile data.
2. Verify an unauthenticated API call returns `401` and a wrong-role call returns `403` using the shared problem format.
3. Verify logout invalidates the session and the next protected call returns `401`.
4. Verify frontend role guards hide unavailable navigation while direct API calls are still denied by the backend.

Expected result: all protected access is authenticated, role-scoped, and CSRF-protected.

### 2. Duration calculation and submission

1. Configure a leave type that excludes selected weekly offs and company holidays and permits half-days.
2. Preview a full-day range containing both exclusions and verify chargeable dates/days.
3. Preview AM and PM half-day requests and verify each returns `0.5` day.
4. Submit the valid request and verify status `PENDING`, active occupancy slots, reserved balance, status history, and an audit event.

Expected result: preview and submit use the same server calculation, and submission never trusts a client-supplied total.

### 3. Overlap and concurrent reservation

1. Submit an active AM request and verify another AM or full-day request for that employee/date returns `409 LEAVE_OVERLAP`; a PM request remains possible when otherwise valid.
2. Run two simultaneous submissions whose combined amount exceeds one available balance.

Expected result: only requests that can atomically acquire unique occupancy slots and sufficient unreserved units succeed; the loser receives a stable conflict, with no partial request, movement, or audit data.

### 4. Manager decision security

1. As the assigned manager, open a direct-report request and approve it.
2. Verify reserved units move to consumed units exactly once and status/audit history is committed.
3. Try the same operation as an unrelated manager and verify denial without mutation.
4. Create a manager's own request and verify that manager cannot approve it even if they hold other non-administrator roles.
5. Reject another pending request with and without the policy-required comment.

Expected result: only the direct manager can decide, self-approval is always blocked, and comment policy is enforced.

### 5. Cancellation and restoration

1. Cancel a pending request and verify reserved units are released once.
2. Cancel approved future leave before its configured cutoff and verify consumed units are restored once.
3. Attempt cancellation exactly at and after the organization-time-zone cutoff.

Expected result: permitted cancellation is fully atomic and late self-cancellation returns `422 CANCELLATION_CUTOFF_PASSED` without changing balance or audit state.

### 6. Administration and audit

1. Create/update an employee and manager relationship and verify it immediately controls manager scope.
2. Create an effective-dated leave policy and holiday and verify new calculations use them.
3. Adjust a balance with a reason and verify summary, immutable movement, and audit event.
4. Perform an exceptional correction with a reason and verify compensating movement/status history rather than rewriting prior history.
5. View organization requests and a leave summary filtered by period.

Expected result: configuration is data-driven, corrections are administrator-only, and every material action is traceable.

### 7. Transaction rollback

Use an integration-test failure injection to make required history/audit persistence fail during submission, approval, rejection, cancellation, and adjustment.

Expected result: request state, occupancy, balance summaries, movements, and history all roll back; no business update survives without required audit history.

## Contract and model references

- REST contract: [contracts/openapi.yaml](./contracts/openapi.yaml)
- Authorization behavior: [contracts/authorization.md](./contracts/authorization.md)
- Requirement mapping: [contracts/traceability.md](./contracts/traceability.md)
- Database entities and state transitions: [data-model.md](./data-model.md)
- Technical decisions: [research.md](./research.md)

## Stop and cleanup

```powershell
docker compose down
```

Use `docker compose down -v` only when intentionally discarding the local demonstration database; it must not be part of routine test or application startup.
