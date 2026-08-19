# Phase 0 Research: Employee Leave Management MVP

## Decision 1: Modular monolith and separate SPA

**Decision**: Use one Spring Boot backend service organized into domain modules and one separately developed React SPA. Deploy them under one origin for the MVP.

**Rationale**: Leave approval, balance reservation, status history, and audit recording need local ACID transactions. A modular monolith preserves those guarantees and matches the constitution's simplicity rule while still separating controllers, services, domain logic, repositories, and security.

**Alternatives considered**:

- Microservices were rejected because they add distributed transactions, deployment coordination, and failure modes without an MVP requirement.
- A single server-rendered application was rejected because the approved stack explicitly requires React and TypeScript.

## Decision 2: Spring Boot and toolchain baseline

**Decision**: Use Java 21, Maven Wrapper, and Spring Boot 4.1.x at the latest compatible patch when implementation begins. Use Spring Boot-managed Spring Security, Spring Data JPA, validation, and test dependency versions. Use React 19.x, TypeScript 5.x, a current stable Vite version, and a checked-in npm lockfile.

**Rationale**: Spring Boot 4.1 is a current stable line compatible with Java 21, and managed dependencies reduce incompatible version combinations. Patch versions remain locked by the implementation manifests and lockfiles, not by prose that becomes stale.

**Alternatives considered**:

- Pinning an older Spring Boot 3.x line was rejected because there is no existing compatibility baseline in the empty repository and the current stable line supports Java 21.
- Unmanaged dependency versions were rejected because they increase security and compatibility drift.

## Decision 3: Stateful session authentication

**Decision**: Use Spring Security server-side session authentication only. Expose JSON login, logout, current-principal, and CSRF bootstrap endpoints. Keep the session identifier in an `HttpOnly` cookie and keep CSRF protection enabled.

**Rationale**: One browser SPA and one backend do not need portable bearer tokens. A server session gives immediate logout/revocation, session-fixation protection, and less custom credential lifecycle code. Same-origin production hosting and a development proxy keep cookie and CSRF behavior straightforward.

**Alternatives considered**:

- JWT access/refresh tokens were rejected because they require secure browser storage, rotation, revocation, and refresh handling without an integration requirement.
- Supporting both sessions and JWT was rejected as duplicate authentication surface.

## Decision 4: Layered authorization

**Decision**: Apply request-level role checks in Spring Security and mandatory repository/application-service ownership and scope predicates using the authenticated employee identity. Employee-private queries use `resource.employee_id = actor.employee_id`. Manager queries and decisions use `resource.employee.manager_id = actor.employee_id`; decisions repeat the predicate after locking and require the actor not to own the request. Employee team-calendar queries union the viewer's own pending/approved entries with entries for active employees sharing the viewer's non-null direct manager and return only display name, start/end dates, and status. Administrator operations require `ADMINISTRATOR` at service entry. Administrators alone perform the three specified exceptional corrections.

**Rationale**: Route roles cannot express row-level reporting scope. Service checks keep authorization adjacent to state transitions and cover every controller or future caller. Repository queries are also scoped to avoid loading unrelated records unnecessarily.

**Alternatives considered**:

- Frontend-only role guards were rejected because they are not security controls.
- Delegated manager authority was rejected by the clarification decision.

## Decision 5: PostgreSQL persistence and Flyway

**Decision**: Use normalized PostgreSQL tables managed only by forward, versioned Flyway SQL migrations. Disable schema auto-creation and validate ORM mappings against the migrated schema.

**Rationale**: Versioned migrations make schema intent, rollout order, checksums, and environment consistency auditable. PostgreSQL provides transactions, row locking, partial unique indexes, JSONB for audit snapshots, and strong relational constraints needed by the domain.

**Alternatives considered**:

- ORM-generated production schema changes were rejected because they are not reviewable migrations.
- A document database was rejected because reporting relationships, balances, requests, and history are relational and transaction-heavy.

## Decision 6: Half-day representation and overlap protection

**Decision**: Represent stored quantities as integer half-day units. Materialize occupied request slots per chargeable date: `AM`, `PM`, or both for a full day. An active-slot partial unique index on employee, date, and slot protects against concurrent overlap.

**Rationale**: Integer units avoid decimal drift. Date ranges alone cannot distinguish two compatible half-day requests from conflicting full/half requests. Occupancy slots make all combinations explicit and allow PostgreSQL to reject races that pass an earlier application check.

**Alternatives considered**:

- A date-range exclusion constraint alone was rejected because it treats all portions of a day alike.
- An application-only overlap query was rejected because concurrent transactions could both pass the check.

## Decision 7: Balance periods, reservation ledger, and locking

**Decision**: Store one balance row per employee, leave type, and policy period, plus request reservation/movement lines. The FR-021 employee balance allocation is the sole MVP allowance mechanism. `tracks_balance=true` unconditionally requires sufficient-unreserved-balance validation and atomic reservation; no independent validation flag, allowance basis, or negative-balance mode exists. Lock affected balance rows with pessimistic write locks in deterministic order for every reserve, approve, reject, cancel, adjustment, or correction transaction.

**Rationale**: A request may cross balance periods, so line items preserve exact allocation. Row locks serialize competing reservations and allow a deterministic insufficient-balance response. Approval moves reserved units to consumed units; rejection/cancellation releases or restores exactly the recorded units.

**Alternatives considered**:

- Optimistic locking alone was rejected as the primary approach because ordinary competing requests would surface retries instead of a direct domain result.
- A single unpartitioned lifetime balance was rejected because employee balance allocations and policy applicability operate over periods.

## Decision 8: Atomic workflow and audit model

**Decision**: Place each business command in one Spring transaction that persists every applicable request/status change, balance summary and reservation line, immutable ledger movement, occupancy change, status history, and generic audit event together. Submission, approval, rejection, cancellation, each of the three permitted corrections, balance allocation, and balance adjustment use this boundary. Request/balance locks are acquired in deterministic order. Audit, ledger, occupancy, or history failure rolls back the entire command. Holiday removal is a version-checked soft deactivation and never deletes historical request or audit data.

**Rationale**: The specification forbids status and balance drift and requires traceability. A separate audit transaction could allow the business change to commit without its required history.

**Alternatives considered**:

- Asynchronous required-audit publication was rejected because it permits gaps.
- Mutable history rows were rejected because corrections must remain traceable.

## Decision 9: REST and error contracts

**Decision**: Use resource-oriented JSON REST endpoints under `/api`, ISO-8601 dates, decimal day values externally, bounded pagination, request/response DTOs, and a shared problem-details error envelope with stable business codes. Concurrency-sensitive update commands require an `expectedVersion` in the body. After authorization and locking where applicable, the service compares it to the aggregate version; a mismatch returns `409 STALE_VERSION` with no mutation and no current-version disclosure. Missing tokens return `400 VALIDATION_FAILED`. New-resource commands use constraints, server revalidation, locking where applicable, and idempotency instead of invented versions.

**Rationale**: DTOs decouple the SPA from persistence. Stable error codes let the frontend present useful messages for overlap, insufficient balance, cutoff, stale state, and forbidden scope while preserving HTTP semantics.

**Alternatives considered**:

- Exposing persistence entities was rejected due to coupling and accidental data disclosure.
- GraphQL was rejected because the requested REST boundaries are direct and sufficient.
- Optional version tokens and silent last-write-wins updates were rejected because they cannot reliably distinguish a deliberate update from a stale browser write.

## Decision 10: Closed administrative correction commands

**Decision**: Model only `PENDING -> CANCELLED`, `APPROVED -> CANCELLED`, and `REJECTED -> PENDING`. The cancellation corrections release reserved or restore consumed balance and deactivate occupancy. Reopening a rejected request revalidates current policy, dates, duration, overlap, and balance, then reserves tracked balance and restores active pending occupancy. Every successful correction writes immutable ledger/status/audit records in the same transaction; same-status and all other pairs fail without mutation.

**Rationale**: A closed command/transition model prevents transport DTOs and services from accepting arbitrary target states and makes compensating balance and occupancy effects testable.

**Alternatives considered**:

- A generic administrator-selected target status was rejected because it permits states and transitions explicitly forbidden by FR-035.
- Rewriting the old ledger or history was rejected because it destroys auditability.

## Decision 11: Frontend structure and tests

**Decision**: Use feature-oriented React modules, nested role routes, a shared authenticated shell and accessible components. Use Vitest with React Testing Library/user-event and MSW for component/integration behavior, plus a small Playwright smoke suite.

**Rationale**: Feature ownership keeps role workflows cohesive while shared controls avoid duplication. Testing through user behavior and the network boundary covers dashboard states, validation, role navigation, conflicts, and session expiry without binding tests to component internals.

**Alternatives considered**:

- A large global state framework was deferred because server-owned leave state and a small auth context do not establish the need.
- Browser tests for every permutation were rejected in favor of fast domain/API tests plus a focused end-to-end path.

## Decision 12: Local demonstration

**Decision**: Provide a root `compose.yaml` for PostgreSQL, Maven and npm wrapper/lockfile workflows, fixed development ports, a Vite `/api` proxy, and profile-scoped demo data configured only for local use.

**Rationale**: A new contributor can start the database, backend, and frontend independently while production keeps one origin. Profile-scoped seed data avoids placing demo credentials in production migrations.

**Alternatives considered**:

- Requiring a manually installed and preconfigured database was rejected as unnecessary setup friction.

## Resolved unknowns

All technical choices needed for Phase 1 are resolved. Organization-specific volume and deployment availability targets were not supplied; they do not block the design and remain explicit production-sizing inputs rather than guessed requirements.

## Primary references

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Security session management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security CSRF protection](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)
- [Spring Framework transaction management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Flyway migration concepts](https://documentation.red-gate.com/flyway/flyway-concepts/migrations)
- [React TypeScript guide](https://react.dev/learn/typescript)
- [Vite development server options](https://vite.dev/config/server-options.html)
- [Testing Library guiding principles](https://testing-library.com/docs/guiding-principles/)
- [W3C accessible forms guidance](https://www.w3.org/WAI/tutorials/forms/)
