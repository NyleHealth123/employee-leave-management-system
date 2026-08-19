# Implementation Plan: Employee Leave Management MVP

**Branch**: `001-employee-leave-management` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-employee-leave-management/spec.md`

## Summary

Build a responsive React and TypeScript single-page application backed by one Java 21 Spring Boot service and PostgreSQL. The modular monolith exposes role-oriented REST resources, uses Spring Security server-side session authentication, and keeps authorization and business invariants in transactional application services. PostgreSQL constraints, row locks, normalized half-day occupancy slots, and Flyway migrations protect overlap and balance integrity. All request transitions, balance movements, and required audit records commit atomically.

## Technical Context

**Language/Version**: Java 21; Spring Boot 4.1.x latest compatible patch; React 19.x; TypeScript 5.x

**Primary Dependencies**: Maven; Spring Web MVC, Spring Security, Spring Data JPA, Bean Validation, Flyway, PostgreSQL JDBC, Spring Boot Actuator; React Router, Vite, native `fetch`-based API client

**Storage**: PostgreSQL 17 or later; versioned SQL migrations are the schema source of truth

**Testing**: JUnit 5, Spring Boot Test, MockMvc, Testcontainers PostgreSQL; Vitest, React Testing Library, user-event, MSW; a small Playwright end-to-end smoke suite

**Target Platform**: Modern evergreen desktop and mobile browsers; Spring Boot service on a Java 21 runtime; local development on Windows, macOS, or Linux; production-compatible Linux deployment

**Project Type**: Web application with a separate React frontend and one Spring Boot backend service

**Performance Goals**: For normal indexed operations at the agreed MVP load, 95% of interactive API requests complete within 500 ms and calendar/report queries within 2 seconds; the authenticated application shell becomes usable within 2 seconds on a typical broadband connection

**Constraints**: Single organization; no microservices; session authentication only; CSRF protection enabled; administrators create each employee and associated password-backed account together; every account has at least one unique role from `EMPLOYEE`, `MANAGER`, and `ADMINISTRATOR`; plaintext passwords are input-only and never persisted or returned; manager scope, employee ownership, employee team-calendar privacy, administrator scope, and self-approval restrictions enforced in repository/application services; `tracks_balance=true` always validates and reserves balance; state, balance, reservation, ledger, occupancy, history, and audit changes are atomic; required `expectedVersion` values protect concurrency-sensitive updates; organization-specific rules remain data-driven

**Scale/Scope**: One-organization MVP with paginated organization and history queries. No business volume target is approved, so production capacity sizing is deferred; schema indexes, bounded page sizes, and database-backed concurrency controls avoid coupling correctness to a guessed user count.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

### Pre-design gate

**Re-run after latest clarification and focused contract reconciliation**: 2026-08-19

| Principle | Result | Planning evidence |
|---|---|---|
| Spec Kit artifacts are authoritative | PASS | The plan traces to the approved `spec.md`, its 2026-08-19 clarification session, and the focused employee-account/role contract decision; no business policy is replaced by a technical default. |
| Traceable, task-gated delivery | PASS | This command reconciles planning artifacts only. Existing task/test IDs remain stable, affected task wording is reconciled, and implementation remains gated on the final `$speckit-analyze`. |
| Domain integrity and role separation | PASS | Closed non-empty role assignment, employee ownership, normal reporting scope, administrator authority, self-approval denial, state transitions, reservations, and cancellation cutoffs are explicit design constraints. |
| Security, quality, and verified workflows | PASS | Atomic password-backed account provisioning, input-only credential hashing, session security, CSRF, layered authorization, explicit error contracts, transaction tests, PostgreSQL integration tests, frontend tests, and end-to-end acceptance coverage are planned. |
| Simplicity and deliberate change | PASS | One backend service and one frontend are used; Flyway versioned migrations govern every persistent schema change. |

No constitution violation or unjustified complexity is present.

## Architecture

### Runtime boundaries

1. The React SPA renders role-specific dashboards and calls only `/api/**` contracts. It may hide unauthorized navigation for usability, but it is never an authorization boundary.
2. Spring Security authenticates the request and applies coarse endpoint role checks. Controllers validate transport shape and map DTOs; they do not implement leave policy.
3. Application services enforce ownership, manager reporting scope, self-approval prevention, state transitions, cancellation cutoff, calculation, and transaction boundaries.
4. Domain services implement reusable leave-day and policy calculations without web or persistence concerns.
5. Repositories persist normalized domain state. PostgreSQL constraints and locks provide the final concurrency and integrity guard.

### Backend modules

- `auth`: login/logout/current-principal, account state, roles, security configuration, CSRF and JSON authentication errors.
- `people`: employee profiles and manager reporting relationships.
- `policy`: leave types, policy versions, weekly offs, holiday treatment, and rejection/cancellation rules.
- `balance`: balance periods, reservations, consumption, and administrator adjustments.
- `request`: calculation preview, submission, lifecycle transitions, history, overlap slots, and employee history.
- `calendar`: employee/team calendars and company holiday views.
- `reporting`: organization request search and aggregate summaries.
- `audit`: append-only audit events written inside the originating business transaction.

Each module may contain `api`, `application`, `domain`, and `persistence` packages. Cross-module calls go through application/domain interfaces rather than controller or repository shortcuts.

### Transaction and concurrency strategy

- Submission calculates occupied dates and half-day slots, locks all applicable `leave_balance` rows in deterministic period order, rechecks unreserved availability, inserts the request and active occupancy slots, creates reservation lines, updates reserved units, and writes status/audit events in one transaction.
- Active overlap is protected by a partial unique index on `(employee_id, leave_date, slot)` for active occupancy rows. A full day occupies both `AM` and `PM`; a half day occupies one slot. Constraint conflicts become a stable `409 LEAVE_OVERLAP` response.
- Approval locks the request and its balance rows, revalidates scope, self-approval, status and current policy, then moves each reservation from reserved to consumed exactly once with status/audit events.
- Rejection or pending cancellation locks the same rows and releases reserved units. Eligible approved cancellation decrements consumed units. The status update, balance movement, occupancy deactivation, history, and audit event commit or roll back together.
- Administrative correction permits only `PENDING -> CANCELLED`, `APPROVED -> CANCELLED`, and `REJECTED -> PENDING`. The first two release/restore balance and deactivate occupancy; `REJECTED -> PENDING` locks and revalidates current policy, dates, overlap, and balance, re-reserves tracked balance, and reactivates occupancy. Same-status and every other transition fail without mutation.
- Balance allocation is the sole MVP allowance mechanism. Every `tracks_balance=true` request validates unreserved availability and creates reservation lines; there is no separate allowance-basis or optional balance-validation mode.
- Administrator balance adjustments and permitted corrections use the same deterministic locking, immutable-ledger, history, and audit rules. No controller directly mutates request or balance entities.

The transaction boundary is the application-service command. The following writes never commit independently:

| Command | Locked/versioned state | Atomic work |
|---|---|---|
| Submit | Deterministically lock applicable balance rows; database guards active slots | Recalculate, validate, create `PENDING`, reserve tracked balance, insert occupancy, ledger, status history, and audit |
| Approve | Require request `expectedVersion`; lock request and balance rows | Revalidate scope/policy/dates/overlap/balance lines, convert reserved to consumed, retain occupancy, write ledger/history/audit |
| Reject | Require request `expectedVersion`; lock request and balance rows | Validate scope/comment, release reservation, deactivate occupancy, write ledger/history/audit |
| Employee cancel | Require request `expectedVersion`; lock request and balance rows | Validate ownership/status/cutoff, release reserved or restore consumed units, deactivate occupancy, write ledger/history/audit |
| Administrator correction | Require request `expectedVersion`; lock request and affected balance rows | Validate the exact permitted source/target pair; apply release/restore/re-reserve and occupancy effect; write ledger/history/audit |
| Balance allocation | Enforce nonoverlapping period constraint and idempotency | Create the allocation summary, immutable `ALLOCATE` movement, and audit event |
| Balance adjustment | Require balance `expectedVersion`; lock balance row | Apply signed adjustment and write immutable `ADMIN_ADJUST` movement and audit event |
| Employee/type/holiday update | Require aggregate `expectedVersion`; optimistic compare-and-increment | Apply the authorized master-data change and audit it; holiday removal sets `active=false` and never deletes history |
| Policy-version creation | Require parent leave-type `expectedVersion`; lock/compare parent and effective-date range | Validate ranges and create the immutable policy version and audit event |

An `expectedVersion` mismatch returns `409 STALE_VERSION`; the entire command has no observable mutation. Create-only commands without an existing mutable target (leave submission, new employee/type/holiday, and new balance-period allocation) use validation, database constraints, locking where applicable, and idempotency rather than a fabricated version token.

### Authentication and authorization

- Use one Spring Security server-side HTTP session established by `POST /api/auth/login`; do not implement JWT.
- The session cookie is `HttpOnly`, `Secure` outside local HTTP development, and `SameSite=Lax`. Successful login rotates the session identifier; logout invalidates it and clears the cookie.
- Keep CSRF enabled. The SPA obtains a CSRF token and sends it in a header for unsafe methods. Production uses one origin; the development server proxies `/api` to avoid broad credentialed CORS.
- Passwords use Spring Security's delegating password encoder. Authentication failures return generic messages; unauthenticated and forbidden API requests return consistent `401` and `403` problem responses.
- Administrator employee creation provisions the employee profile and its password-backed user account in one transaction. `initialPassword` is required only on that creation command, is hashed before persistence, and is never serialized in an employee, user, principal, or audit response. The MVP adds no reset, invitation, SSO, magic-link, or first-login password-change workflow.
- Every account has one or more unique roles drawn only from `EMPLOYEE`, `MANAGER`, and `ADMINISTRATOR`. Create/update DTOs and employee/principal response DTOs use the same closed role enum and non-empty, duplicate-free array constraints.
- Endpoint role checks are supplemented by repository/application-service predicates. Employee-private queries require `request.employee_id = actor.employee_id`; manager queries require `request.employee.manager_id = actor.employee_id`; decisions repeat that predicate after locking and require `request.employee_id != actor.employee_id`; administrator endpoints require `ADMINISTRATOR` and use no manager impersonation path.
- The employee team-calendar query returns only `PENDING`/`APPROVED` entries owned by active employees where `(entry.employee_id = actor.employee_id) OR (actor.manager_id IS NOT NULL AND entry.employee.manager_id = actor.manager_id)`. Its dedicated projection contains only employee display name, start date, end date, and status—never reason, balance, duration mode, leave type, comments, history, identifiers, or audit data.

### API and DTO strategy

- `contracts/openapi.yaml` is the external REST contract. Controllers accept request DTOs and return response DTOs; persistence entities are never serialized.
- Use `/api` as the base path, JSON payloads, ISO-8601 dates/timestamps, decimal day quantities externally, and integer half-day units internally.
- List endpoints use bounded page parameters and stable sorting. Validation, authorization, conflict, and business-rule failures use a shared problem response with a machine-readable code and field errors.
- Every concurrency-sensitive update DTO requires `expectedVersion`. Missing tokens fail request validation with `400 VALIDATION_FAILED`; mismatches return `409 STALE_VERSION` with the current value undisclosed. Correction DTOs expose only the closed actions `CANCEL_PENDING`, `CANCEL_APPROVED`, and `REOPEN_REJECTED`; the service validates each action's exact source status after locking.
- Each protected operation explicitly documents applicable `400`, `401`, `403`, `404`, and `409` problem responses. Response entries follow actual validation, authentication, role/scope, hidden-resource, and established conflict behavior rather than mechanically advertising impossible outcomes.
- State-changing operations support an `Idempotency-Key` header where duplicate browser submission is plausible. Database uniqueness on the actor and key prevents duplicate leave requests or balance movements.

### Migration and data lifecycle

- Flyway versioned SQL files live in `backend/src/main/resources/db/migration`. Applied migrations are never edited; changes use forward migrations.
- Initial migrations create identity/role/people tables, policy/holiday tables, balance/request tables, audit/history tables, constraints/indexes, and local-demo seed data in a profile-specific migration location.
- Tests start an empty PostgreSQL instance, run all migrations, and verify constraints. Schema auto-creation is disabled; ORM validation checks mapping drift.
- Audit and status history are append-only. Normal deletion uses employee/leave-type deactivation. Holiday removal is soft deactivation (`active=false`); requests, policy snapshots, balance movements, status history, and audit records are retained.

### Frontend design

- Use an authenticated application shell with nested Employee, Manager, and Administrator routes. Users with multiple roles can switch among permitted dashboards.
- Organize code by feature (`auth`, `dashboard`, `leave-requests`, `balances`, `team-approvals`, `calendar`, `admin`, `reports`) with shared accessible form, table, card, status, dialog, date, and API components.
- A central API client sends cookies and CSRF headers, maps problem responses, and handles expired sessions. Server responses remain authoritative for calculated duration, balance, scope, status, and cancellation eligibility.
- Provide loading, empty, validation, forbidden, conflict, and retry states. Mobile layouts use stacked cards and an agenda calendar view; all actions remain keyboard accessible and status is not conveyed by color alone.

## Test Strategy

- **Domain unit tests**: duration calculation, configured weekly offs and holidays, AM/PM rules, policy versions, cutoff boundaries, the exact ordinary/administrative transition matrix, mandatory tracked-balance reservation, and balance invariants.
- **Repository/migration tests**: migrations from empty database, foreign/check/unique constraints, active occupancy conflicts, immutable history expectations, query indexes, and pessimistic locks using PostgreSQL Testcontainers.
- **Transactional integration tests**: simultaneous submissions against one balance, exactly-once reservation conversion/release/restoration/re-reservation, manager rejection ledger and availability restoration, all three permitted corrections and all forbidden corrections, rollback when occupancy/ledger/audit/history insertion fails, required stale-version rejection, deterministic multi-period locking, and holiday soft-deactivation retention.
- **Security/API tests**: login/logout/me/CSRF; administrator employee/account provisioning with required input-only password hashing; closed non-empty duplicate-free role arrays; `401` and `403`; employee ownership; manager direct-report scope; manager self-approval denial; administrator-only configuration, adjustment, reports, and corrections; operation-specific `400`/`401`/`403`/`404`/`409` error contracts and pagination.
- **Frontend tests**: role routes and navigation, dashboard states, calculation preview, submission validation/conflicts, history, cancellation cutoff messaging, manager decision/comment behavior, administrator forms, API/session errors, keyboard behavior, and responsive variants.
- **End-to-end smoke**: employee login and submit, manager login and approve/reject, employee status/balance update, eligible cancellation restoration, and audit visibility for an administrator.

## Project Structure

### Documentation (this feature)

```text
specs/001-employee-leave-management/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- openapi.yaml
|   |-- authorization.md
|   `-- traceability.md
`-- tasks.md                 # created by $speckit-tasks, not this command
```

### Source Code (repository root)

```text
backend/
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
`-- src/
    |-- main/
    |   |-- java/com/example/leavemanagement/
    |   |   |-- auth/{api,application,domain,persistence}/
    |   |   |-- people/{api,application,domain,persistence}/
    |   |   |-- policy/{api,application,domain,persistence}/
    |   |   |-- balance/{api,application,domain,persistence}/
    |   |   |-- request/{api,application,domain,persistence}/
    |   |   |-- calendar/{api,application}/
    |   |   |-- reporting/{api,application,persistence}/
    |   |   |-- audit/{domain,persistence}/
    |   |   `-- shared/{api,config,security}/
    |   `-- resources/db/migration/
    `-- test/java/com/example/leavemanagement/
        |-- unit/
        |-- integration/
        |-- security/
        `-- contract/

frontend/
|-- package.json
|-- package-lock.json
|-- vite.config.ts
`-- src/
    |-- app/{router,providers,layout}/
    |-- features/{auth,dashboard,leave-requests,balances,team-approvals,calendar,admin,reports}/
    |-- shared/{api,components,forms,types,utils}/
    |-- test/
    `-- main.tsx

e2e/
|-- package.json
`-- tests/

compose.yaml
README.md
```

**Structure Decision**: Use a modular monolith in `backend/` and a separate feature-oriented SPA in `frontend/`, with only the minimal cross-application `e2e/` harness. This matches the requested deployment boundary and keeps business transactions inside one service.

## Post-design Constitution Check

**Re-run after reconciled Phase 0/Phase 1 artifacts and contracts**: 2026-08-19

| Principle | Result | Design evidence |
|---|---|---|
| Authoritative artifacts | PASS | Data model and contracts implement atomic password-backed employee/account provisioning, closed non-empty roles, credential-safe DTOs, explicit operation error responses, mandatory tracked-balance reservation, the sole allocation mechanism, privacy-safe same-manager calendars, exact correction transitions, holiday soft deactivation, required stale-write tokens, and audit rules without adding HRMS scope. |
| Traceable delivery | PASS | `contracts/traceability.md` maps requirements and outcomes to stable task/test IDs, and directly affected task wording is reconciled without renumbering. |
| Domain integrity and role separation | PASS | Explicit non-empty closed role assignments, ownership, direct-report, same-manager-calendar, administrator, and self-approval predicates combine with active occupancy constraints, balance movements, and immutable state history. |
| Security, quality, verified workflows | PASS | Password-backed account provisioning, input-only credential handling, authentication, CSRF, explicit API error contracts, transaction rollback, concurrency, migration, authorization, frontend, and end-to-end tests are explicitly defined. |
| Simplicity and deliberate change | PASS | One backend service, one database, one frontend, forward-only migrations, and no distributed workflow are introduced. |

No complexity exception is required.
