---

description: "Dependency-ordered implementation tasks for the Employee Leave Management MVP"
---

# Tasks: Employee Leave Management MVP

**Input**: Approved design documents from `/specs/001-employee-leave-management/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`, `contracts/authorization.md`, `contracts/traceability.md`, `quickstart.md`, `.specify/memory/constitution.md`

**Tests**: Automated tests are required for critical workflows by the constitution, specification, plan, and user request. Test tasks precede the corresponding implementation tasks and must initially fail for the intended reason.

**Organization**: Tasks are grouped by user story after shared setup and foundational work. Requirement identifiers in task descriptions preserve traceability to the approved specification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it targets different files and does not depend on unfinished work in the same phase
- **[Story]**: Maps the task to a user story from `spec.md`
- Every task names the primary file or module area it changes

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the approved modular-monolith backend, React SPA, test harnesses, and local development skeleton without adding application behavior.

- [X] T001 Create the Java 21 Spring Boot Maven Wrapper project and approved web, security, JPA, validation, Flyway, PostgreSQL, Actuator, JUnit, Spring Boot Test, Testcontainers, and architecture-test dependencies in `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, and `backend/.mvn/wrapper/maven-wrapper.properties`
- [X] T002 [P] Create the React 19 and TypeScript 5 Vite project with React Router and production dependencies in `frontend/package.json`, `frontend/package-lock.json`, `frontend/tsconfig.json`, `frontend/tsconfig.app.json`, and `frontend/vite.config.ts`
- [X] T003 [P] Configure frontend linting, Vitest, React Testing Library, user-event, MSW, and coverage scripts in `frontend/eslint.config.js`, `frontend/vitest.config.ts`, and `frontend/src/test/setup.ts`
- [X] T004 [P] Create the Playwright end-to-end package and configuration in `e2e/package.json`, `e2e/package-lock.json`, and `e2e/playwright.config.ts`
- [X] T005 Create the backend application entry point and approved module package skeleton in `backend/src/main/java/com/example/leavemanagement/LeaveManagementApplication.java` and `backend/src/main/java/com/example/leavemanagement/{auth,people,policy,balance,request,calendar,reporting,audit,shared}`
- [X] T006 [P] Create the frontend entry point and feature-oriented directory skeleton in `frontend/src/main.tsx`, `frontend/src/app/`, `frontend/src/features/`, and `frontend/src/shared/`
- [X] T007 [P] Add repository-wide Java, TypeScript, SQL, and Markdown formatting/editor settings in `.editorconfig`, `backend/.gitattributes`, and `frontend/.prettierignore`
- [X] T008 Configure the local PostgreSQL 17 service, health check, persistent volume, and environment variables in `compose.yaml` and `.env.example`, and ignore real local secrets in `.gitignore`
- [X] T009 [P] Configure the Vite `/api` development proxy and strict port 5173 in `frontend/vite.config.ts`
- [X] T010 [P] Add backend, frontend, database, and test commands plus the one-origin production model to `README.md`
- [X] T011 Verify the empty backend, frontend, and e2e scaffolds compile and record the baseline commands in `specs/001-employee-leave-management/tasks.md`

**Baseline commands (2026-08-19)**: `backend/mvnw.cmd test-compile`, `frontend/npm run typecheck`, `frontend/npm run build`, and `e2e/npm exec playwright -- test --list` (configuration loaded; zero tests are expected in the T004 scaffold).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the database schema, persistence model, session security, shared API behavior, immutable audit foundation, and authenticated frontend shell required by every user story.

**⚠️ CRITICAL**: No user story implementation starts until this phase passes its migration, security, and shared contract tests.

- [X] T012 Configure PostgreSQL, Flyway-only schema management, Hibernate `validate`, organization time zone, bounded pagination defaults, and `local`/`test` profiles in `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-local.yml`, and `backend/src/test/resources/application-test.yml`
- [X] T013 Write a PostgreSQL Testcontainers migration test covering clean-database migration, required constraints, and ORM mapping validation in `backend/src/test/java/com/example/leavemanagement/integration/MigrationValidationTest.java`
- [X] T014 Implement identity, role, employee, manager self-reference, account-state, and supporting indexes in `backend/src/main/resources/db/migration/V001__identity_and_people.sql`
- [X] T015 Implement leave type, effective-dated policy, weekly-off, organization settings, holiday, policy-range constraints, and indexes in `backend/src/main/resources/db/migration/V002__leave_policy_and_calendar.sql`
- [X] T016 Implement balance periods, nonoverlap protection, summary invariants, append-only balance movements, idempotency keys, and indexes in `backend/src/main/resources/db/migration/V003__leave_balances.sql`
- [X] T017 Implement leave requests, half-day occupancy slots, balance allocation lines, state constraints, active-slot partial uniqueness, idempotency keys, and query indexes in `backend/src/main/resources/db/migration/V004__leave_requests.sql`
- [X] T018 Implement append-only request status history and audit-event tables, indexes, and mutation-prevention rules in `backend/src/main/resources/db/migration/V005__history_and_audit.sql`
- [X] T019 [P] Map organization, account, roles, employee profiles, and manager relationships with optimistic versions in `backend/src/main/java/com/example/leavemanagement/auth/persistence/UserAccountEntity.java` and `backend/src/main/java/com/example/leavemanagement/people/persistence/EmployeeProfileEntity.java`
- [X] T020 [P] Map leave types, policy versions, weekly offs, and holidays in `backend/src/main/java/com/example/leavemanagement/policy/persistence/LeaveTypeEntity.java`, `LeavePolicyVersionEntity.java`, `PolicyWeeklyOffEntity.java`, and `CompanyHolidayEntity.java`
- [X] T021 [P] Map balance summaries and immutable ledger movements in `backend/src/main/java/com/example/leavemanagement/balance/persistence/LeaveBalanceEntity.java` and `LeaveBalanceMovementEntity.java`
- [X] T022 [P] Map requests, occupancy slots, allocation lines, and status history in `backend/src/main/java/com/example/leavemanagement/request/persistence/LeaveRequestEntity.java`, `LeaveRequestSlotEntity.java`, `LeaveRequestBalanceLineEntity.java`, and `LeaveRequestStatusHistoryEntity.java`
- [X] T023 [P] Map immutable generic audit events and expose insert/read-only persistence operations in `backend/src/main/java/com/example/leavemanagement/audit/persistence/AuditEventEntity.java` and `AuditEventRepository.java`
- [X] T024 Define scoped repositories, deterministic balance locking queries, stable sorting, and bounded page queries in `backend/src/main/java/com/example/leavemanagement/{auth,people,policy,balance,request}/persistence/*Repository.java`
- [X] T025 [P] Write shared problem-response tests for validation, safe 401/403/404 handling, business codes, field errors, and correlation IDs in `backend/src/test/java/com/example/leavemanagement/contract/ProblemResponseContractTest.java`
- [X] T026 Implement the OpenAPI-aligned problem DTO, exception hierarchy, correlation filter, validation mapping, and global exception handler in `backend/src/main/java/com/example/leavemanagement/shared/api/ProblemResponse.java`, `GlobalExceptionHandler.java`, and `CorrelationIdFilter.java`
- [X] T027 [P] Write MockMvc security tests for CSRF bootstrap, generic login failure, session fixation protection, logout invalidation, disabled accounts, non-empty duplicate-free closed-role principals, and protected endpoint 401/403 behavior in `backend/src/test/java/com/example/leavemanagement/security/AuthenticationSecurityTest.java`
- [X] T028 Implement Spring Security delegating password encoding, database-backed principal loading, JSON session login/logout/me/CSRF endpoints, cookie attributes, CSRF enforcement, and coarse role rules in `backend/src/main/java/com/example/leavemanagement/auth/application/AccountUserDetailsService.java`, `backend/src/main/java/com/example/leavemanagement/auth/api/AuthController.java`, and `backend/src/main/java/com/example/leavemanagement/shared/security/SecurityConfiguration.java` (FR-001, FR-002)
- [X] T029 Implement the authenticated user/employee identity abstraction used by service-layer ownership and scope checks in `backend/src/main/java/com/example/leavemanagement/shared/security/CurrentActor.java` and `CurrentActorProvider.java` (FR-002, FR-003)
- [X] T030 [P] Write frontend API-client tests for cookie credentials, CSRF acquisition/header injection, problem mapping, correlation IDs, and expired-session handling in `frontend/src/shared/api/apiClient.test.ts`
- [X] T031 Implement the typed native-fetch API client, CSRF token lifecycle, common problem types, and session-expiry callback in `frontend/src/shared/api/apiClient.ts` and `frontend/src/shared/api/problem.ts`
- [X] T032 [P] Write authenticated route and multi-role navigation tests in `frontend/src/app/router/AppRouter.test.tsx` and `frontend/src/app/layout/AppShell.test.tsx`
- [X] T033 Implement the authentication provider, protected role routes, multi-role dashboard switcher, logout action, and accessible responsive shell in `frontend/src/app/providers/AuthProvider.tsx`, `frontend/src/app/router/AppRouter.tsx`, and `frontend/src/app/layout/AppShell.tsx`
- [X] T034 Add architecture tests that prevent controllers from calling repositories and enforce approved module dependencies in `backend/src/test/java/com/example/leavemanagement/architecture/ModuleArchitectureTest.java`

**Checkpoint**: All migrations apply to empty PostgreSQL, authentication and CSRF work through the REST contract, shared failures are stable and safe, and both applications compile.

---

## Phase 3: User Story 1 - Submit and Track a Leave Request (Priority: P1) 🎯 MVP

**Goal**: An authenticated employee can view configured options and balances, preview authoritative duration, submit a non-overlapping request with an atomic pending reservation, inspect history, and use the employee dashboard/calendar.

**Independent Test**: With configured policy, holidays, weekly offs, and balance, sign in as an employee, preview a valid range, submit it, and verify `PENDING`, occupied slots, reserved units, immutable movement/status/audit entries, own history, and dashboard visibility; overlaps and insufficient concurrent reservations must leave no partial data.

### Tests for User Story 1

- [X] T035 [P] [US1] Write parameterized duration-calculation unit tests for inverted/empty ranges, configurable weekly offs and holidays, full-day slots, permitted AM/PM half-days, disallowed half-days, policy boundaries, and zero chargeable days in `backend/src/test/java/com/example/leavemanagement/unit/LeaveDurationCalculatorTest.java` (FR-006–FR-008, FR-033, FR-034; SC-002)
- [X] T036 [P] [US1] Write PostgreSQL repository tests for every partial/full active overlap shape and concurrent active-slot uniqueness in `backend/src/test/java/com/example/leavemanagement/integration/LeaveRequestOverlapRepositoryTest.java` (FR-009)
- [X] T037 [P] [US1] Write transactional submission tests for deterministic multi-period locks, concurrent non-overlapping submissions competing for the same sufficient/insufficient unreserved balance, idempotent duplicate submission, and rollback when request/slot/balance-line/ledger/history/audit persistence fails in `backend/src/test/java/com/example/leavemanagement/integration/LeaveSubmissionTransactionTest.java` (FR-010, FR-011, FR-027)
- [X] T038 [P] [US1] Write MockMvc contract/security tests for leave types, holidays, own balances, calculation, submit, own list/detail, dashboard, and employee team-calendar results limited to the viewer plus pending/approved entries of active employees sharing the viewer's direct manager, with the exact display-name/date/status field allowlist and no private fields; verify missing or invalid CSRF tokens on calculation and submission return `403 ACCESS_DENIED` and cause no protected state mutation in `backend/src/test/java/com/example/leavemanagement/contract/EmployeeLeaveApiContractTest.java` (FR-003–FR-011, FR-031, FR-032; SC-003, SC-009)
- [X] T039 [P] [US1] Write frontend behavior tests for request validation, preview exclusions, half-day controls, conflict/insufficient-balance problems, duplicate-submit prevention, request history/detail, and dashboard loading/empty/error states in `frontend/src/features/leave-requests/LeaveRequestFlow.test.tsx` and `frontend/src/features/dashboard/EmployeeDashboard.test.tsx`

### Implementation for User Story 1

- [X] T040 [P] [US1] Define integer half-day units, duration modes, calculated slots, exclusion reasons, and calculation result domain types in `backend/src/main/java/com/example/leavemanagement/request/domain/LeaveUnits.java`, `DurationMode.java`, and `LeaveCalculation.java`
- [X] T041 [US1] Implement the single authoritative duration calculator using effective policies, organization time zone, configured weekly offs, and active company holidays in `backend/src/main/java/com/example/leavemanagement/request/domain/LeaveDurationCalculator.java` (FR-007, FR-008, FR-033, FR-034)
- [X] T042 [P] [US1] Implement active leave-type/option and company-holiday reference queries in `backend/src/main/java/com/example/leavemanagement/policy/application/LeavePolicyQueryService.java` and `backend/src/main/java/com/example/leavemanagement/calendar/application/HolidayQueryService.java` (FR-005, FR-031)
- [X] T043 [P] [US1] Implement own balance projection and multi-period deterministic locking/allocation operations in `backend/src/main/java/com/example/leavemanagement/balance/application/LeaveBalanceService.java` (FR-005, FR-010)
- [X] T044 [US1] Implement calculation preview with server-owned employee identity and shared calculation rules in `backend/src/main/java/com/example/leavemanagement/request/application/LeaveCalculationService.java` (FR-006–FR-008)
- [X] T045 [US1] Implement atomic leave submission that recalculates, checks active overlap, and for every `tracks_balance=true` policy locks and validates sufficient unreserved balance before inserting the `PENDING` request/slots, exact reservation lines, immutable `RESERVE` ledger/status/audit records, and idempotency result in one transaction; untracked policies alone omit balance lines in `backend/src/main/java/com/example/leavemanagement/request/application/LeaveSubmissionService.java` (FR-009–FR-012, FR-027, FR-028)
- [X] T046 [US1] Translate active-slot and insufficient-balance database conflicts to stable `409 LEAVE_OVERLAP` and `409 INSUFFICIENT_BALANCE` problems in `backend/src/main/java/com/example/leavemanagement/request/application/LeaveSubmissionExceptionTranslator.java` (FR-009, FR-010, FR-032)
- [X] T047 [P] [US1] Implement employee-owned paginated history/detail queries that expose decision comments and status history without accepting an employee ID in `backend/src/main/java/com/example/leavemanagement/request/application/EmployeeLeaveRequestQueryService.java` (FR-003, FR-011)
- [X] T048 [P] [US1] Implement the employee dashboard and a dedicated team-calendar projection whose predicate permits the viewer's own pending/approved entries plus pending/approved entries of active employees sharing the viewer's direct manager and whose DTO contains only employee display name, start/end dates, and status in `backend/src/main/java/com/example/leavemanagement/calendar/application/EmployeeDashboardService.java` and `EmployeeTeamCalendarService.java` (FR-003, FR-004, FR-031; SC-009)
- [X] T049 [US1] Implement OpenAPI DTOs and controllers for `/leave-types`, `/holidays`, `/employee/leave-balances`, calculation, submission, own history/detail, dashboard, and employee team calendar in `backend/src/main/java/com/example/leavemanagement/{policy,calendar,balance,request}/api/` (FR-003–FR-011, FR-031)
- [X] T050 [P] [US1] Define TypeScript request, calculation, balance, holiday, request-history, page, and dashboard contract types in `frontend/src/shared/types/leave.ts` and API functions in `frontend/src/features/leave-requests/api.ts` and `frontend/src/features/dashboard/api.ts`
- [X] T051 [P] [US1] Build accessible reusable field, error summary, loading, empty, retry, card, table, status badge, and date-display components in `frontend/src/shared/components/` and `frontend/src/shared/forms/` (FR-032)
- [X] T052 [US1] Build the employee leave request form with leave-type options, full/half-day controls, authoritative preview, exclusions, available balance, validation, and idempotent submission in `frontend/src/features/leave-requests/LeaveRequestFormPage.tsx` (FR-005–FR-010)
- [X] T053 [P] [US1] Build responsive employee request history and detail/status-timeline pages in `frontend/src/features/leave-requests/LeaveRequestHistoryPage.tsx` and `LeaveRequestDetailPage.tsx` (FR-011, FR-028)
- [X] T054 [P] [US1] Build the employee dashboard with balance cards, pending requests, approved upcoming leave, and holidays in `frontend/src/features/dashboard/EmployeeDashboardPage.tsx` (FR-004)
- [X] T055 [P] [US1] Build the privacy-safe responsive employee team agenda/calendar view in `frontend/src/features/calendar/EmployeeTeamCalendarPage.tsx` (FR-031)
- [X] T056 [US1] Register employee dashboard, request, history/detail, balance, holiday, and team-calendar routes and navigation in `frontend/src/app/router/AppRouter.tsx` and `frontend/src/app/layout/AppShell.tsx`
- [X] T057 [US1] Run US1 backend and frontend suites, verify calculation acceptance evidence, and reconcile every US1 operation/schema with `specs/001-employee-leave-management/contracts/openapi.yaml` in `backend/src/test/java/com/example/leavemanagement/contract/EmployeeLeaveApiContractTest.java` (SC-002)

**Checkpoint**: User Story 1 is independently usable and testable as the employee-request MVP.

---

## Phase 4: User Story 2 - Decide a Direct Report's Leave Request (Priority: P1)

**Goal**: A manager can see and decide only direct-report requests, never self-approve, apply rejection-comment policy, atomically convert/release reservations, and view the scoped team calendar/dashboard.

**Independent Test**: Sign in as a direct manager, inspect and approve/reject a pending direct-report request, then verify exact balance conversion/release and history/audit; repeat as an unrelated manager and as the request owner and verify denial without mutation.

### Tests for User Story 2

- [X] T058 [P] [US2] Write manager authorization matrix tests for role gating, repository query scoping, direct-report detail, unrelated-manager denial, multi-role behavior, and unconditional self-approval prevention in `backend/src/test/java/com/example/leavemanagement/security/ManagerAuthorizationTest.java` (FR-014–FR-016)
- [X] T059 [P] [US2] Write transactional approval/rejection tests for mandatory `expectedVersion` mismatch handling, current-policy/date/overlap/balance revalidation, material policy changes, required rejection comments, approval's exactly-once `RESERVED` to `CONSUMED` conversion, rejection's exactly-once reservation release and occupancy deactivation, and complete rollback of request/balance/ledger/occupancy/history/audit state in `backend/src/test/java/com/example/leavemanagement/integration/ManagerDecisionTransactionTest.java` (FR-016, FR-017, FR-024–FR-028; SC-004)
- [X] T060 [P] [US2] Write MockMvc contract tests for manager queue/detail, approve/reject, and scoped team-calendar operations in `backend/src/test/java/com/example/leavemanagement/contract/ManagerLeaveApiContractTest.java` (FR-014–FR-018)
- [X] T061 [P] [US2] Write frontend tests for manager dashboard/queue states, detail balance, approve/reject dialogs, required comments, stale/policy conflicts, forbidden scope, and responsive calendar in `frontend/src/features/team-approvals/ManagerWorkflow.test.tsx` and `frontend/src/features/dashboard/ManagerDashboard.test.tsx`

### Implementation for User Story 2

- [X] T062 [P] [US2] Implement direct-report-scoped queue/detail repository projections and relevant-balance lookup in `backend/src/main/java/com/example/leavemanagement/request/persistence/ManagerLeaveRequestQueryRepository.java` (FR-014, FR-015)
- [X] T063 [US2] Implement manager request query service that derives manager identity from the principal and repeats scope checks without leaking out-of-scope resources in `backend/src/main/java/com/example/leavemanagement/request/application/ManagerLeaveRequestQueryService.java` (FR-014, FR-015, FR-032)
- [X] T064 [US2] Implement atomic approval with request/balance locking, mandatory `expectedVersion`, direct-report and unconditional self-approval checks, status/current-policy/date/duration/overlap/balance revalidation, exactly-once reservation-to-consumption ledger conversion, retained occupancy, and immutable status-history/audit writes in `backend/src/main/java/com/example/leavemanagement/request/application/ApproveLeaveRequestService.java` (FR-015, FR-016, FR-024, FR-026–FR-028)
- [X] T065 [US2] Implement atomic rejection with request/balance locking, mandatory `expectedVersion`, direct-report and self checks, configured comment validation, exactly-once reservation-release ledger movement, slot deactivation, and immutable status-history/audit writes in `backend/src/main/java/com/example/leavemanagement/request/application/RejectLeaveRequestService.java` (FR-015–FR-017, FR-025, FR-027, FR-028)
- [X] T066 [P] [US2] Implement direct-report-only pending/approved calendar and manager dashboard projections in `backend/src/main/java/com/example/leavemanagement/calendar/application/ManagerTeamCalendarService.java` and `backend/src/main/java/com/example/leavemanagement/request/application/ManagerDashboardService.java` (FR-018)
- [X] T067 [US2] Implement OpenAPI manager DTOs and queue/detail/approve/reject/team-calendar controllers with mandatory decision `expectedVersion`, stable `400 VALIDATION_FAILED`/`409 STALE_VERSION` responses, coarse `MANAGER` checks, and service-enforced direct-report scope/self-approval prevention in `backend/src/main/java/com/example/leavemanagement/request/api/ManagerLeaveRequestController.java` and `backend/src/main/java/com/example/leavemanagement/calendar/api/ManagerCalendarController.java` (FR-014–FR-018, FR-032)
- [X] T068 [P] [US2] Implement typed manager API functions and contracts that carry the loaded request `version` as mandatory `expectedVersion` for approve/reject and map stale-version problems in `frontend/src/features/team-approvals/api.ts` and `frontend/src/shared/types/manager.ts`
- [X] T069 [US2] Build the manager dashboard, paginated pending queue, request detail with relevant balance, and accessible approve/reject dialogs that submit the loaded request version and refresh on `STALE_VERSION` in `frontend/src/features/dashboard/ManagerDashboardPage.tsx`, `frontend/src/features/team-approvals/ManagerQueuePage.tsx`, and `ManagerRequestDetailPage.tsx` (FR-014, FR-016, FR-017, FR-032)
- [X] T070 [P] [US2] Build the responsive pending/approved direct-report agenda/calendar in `frontend/src/features/calendar/ManagerTeamCalendarPage.tsx` (FR-018)
- [X] T071 [US2] Register manager-only dashboard, queue/detail, and calendar routes/navigation in `frontend/src/app/router/AppRouter.tsx` and `frontend/src/app/layout/AppShell.tsx`
- [X] T072 [US2] Run the US2 suites and verify unauthorized, stale, failed-audit, and duplicate-decision attempts leave request, slot, balance, movement, and history state unchanged in `backend/src/test/java/com/example/leavemanagement/integration/ManagerDecisionTransactionTest.java`

**Checkpoint**: User Story 2 works for direct reports only and preserves every authorization and balance invariant.

---

## Phase 5: User Story 3 - Administer Leave Policy and Employee Records (Priority: P1)

**Goal**: An administrator can manage employees/reporting, leave types and effective policies, weekly offs, holidays, starting balances, and audited adjustments through administrator-only APIs and UI.

**Independent Test**: As an administrator, create/update an employee and manager relationship, configure a leave type/policy and holiday, allocate and adjust a balance with reasons, then verify scope, calculation, immutable ledger, and audit effects; verify every operation is denied to non-administrators.

### Tests for User Story 3

- [X] T073 [P] [US3] Write people-domain/integration tests for atomic administrator creation of an employee and associated password-backed account, mandatory input-only `initialPassword`, secure hashing with no plaintext persistence/response/audit exposure, non-empty duplicate-free roles restricted to `EMPLOYEE`/`MANAGER`/`ADMINISTRATOR`, unique employee identity, active account/profile behavior, manager-role requirement, manager self-reference rejection, administrator authorization, employee/reporting updates with mandatory `expectedVersion`, stale no-mutation behavior, immediate manager-scope changes, and deactivation retention in `backend/src/test/java/com/example/leavemanagement/integration/EmployeeAdministrationTest.java` (FR-001, FR-002, FR-019, FR-032; SC-003)
- [X] T074 [P] [US3] Write policy-domain/repository tests for leave-type updates and parent-guarded policy creation with mandatory `expectedVersion` and stale no-mutation behavior, version sequencing, nonoverlapping effective dates, data-driven weekly offs/holiday treatment, unconditional validation/reservation for `tracks_balance=true`, absence of any duplicate allowance-basis setting, half-day/rejection/cutoff rules, and leave-type deactivation in `backend/src/test/java/com/example/leavemanagement/integration/LeavePolicyAdministrationTest.java` (FR-020, FR-021, FR-033, FR-034)
- [X] T075 [P] [US3] Write transactional balance administration tests for the sole MVP allowance path of nonoverlapping employee balance-period allocation, signed adjustment with mandatory `expectedVersion` and stale no-mutation handling, mandatory reasons, deterministic locks, idempotency, immutable `ALLOCATE`/`ADMIN_ADJUST` movements, and full summary/ledger/audit rollback in `backend/src/test/java/com/example/leavemanagement/integration/BalanceAdministrationTest.java` (FR-021, FR-023, FR-027, FR-032)
- [X] T076 [P] [US3] Write holiday repository/application tests for create/update/soft-deactivate with mandatory `expectedVersion`, stale no-mutation behavior, active-date uniqueness, downstream calculation effects, and preservation of historical requests, policy snapshots, status history, and audit records in `backend/src/test/java/com/example/leavemanagement/integration/HolidayAdministrationTest.java` (FR-022, FR-027, FR-034)
- [X] T077 [P] [US3] Write administrator-only MockMvc contract tests for employee/account provisioning and reporting, leave-type, policy-version, holiday, balance allocation, and balance-adjustment operations, including required input-only `initialPassword`, consistent closed non-empty duplicate-free role arrays, no credential fields in responses, required `expectedVersion` fields, and each operation's applicable safe `400`/`401`/`403`/`404`/`409 STALE_VERSION` responses in `backend/src/test/java/com/example/leavemanagement/contract/AdministrationConfigurationApiContractTest.java` (FR-001, FR-002, FR-019–FR-023, FR-032)
- [X] T078 [P] [US3] Write frontend tests for administrator dashboard and employee/account provisioning/reporting, leave-type/policy, weekly-off, holiday soft-deactivation, allocation, and adjustment forms, proving employee creation requires `initialPassword`, role selection is non-empty and duplicate-free, response models contain no credential fields, each contract-designated update sends the loaded `expectedVersion`, and applicable validation, 403, and `STALE_VERSION` refresh/retry states are handled in `frontend/src/features/admin/AdminConfiguration.test.tsx` and `frontend/src/features/dashboard/AdminDashboard.test.tsx`

### Implementation for User Story 3

- [X] T079 [US3] Implement administrator-only transactional employee/profile and password-backed account creation requiring input-only `initialPassword`, hash it before persistence without plaintext/audit/response exposure, enforce at least one unique role from `EMPLOYEE`/`MANAGER`/`ADMINISTRATOR`, and implement `expectedVersion`-guarded employee/reporting update, role assignment, manager activity/role/self validation, deactivation, and audit so stale relationship commands make no mutation in `backend/src/main/java/com/example/leavemanagement/people/application/EmployeeAdministrationService.java` (FR-001, FR-002, FR-019, FR-027, FR-032)
- [X] T080 [P] [US3] Implement paginated employee administration queries and manager-choice projections in `backend/src/main/java/com/example/leavemanagement/people/application/EmployeeAdministrationQueryService.java` (FR-019)
- [X] T081 [US3] Implement administrator-only leave-type creation plus `expectedVersion`-guarded metadata update/deactivation and parent-version-guarded immutable effective-dated policy creation, enforcing weekly-off/range rules, mandatory tracked-balance semantics, no duplicate allowance-basis concept, and atomic audit writes in `backend/src/main/java/com/example/leavemanagement/policy/application/LeavePolicyAdministrationService.java` (FR-020, FR-021, FR-027, FR-033)
- [X] T082 [US3] Implement administrator-only holiday creation and `expectedVersion`-guarded update/soft deactivation (`active=false`) with atomic audit, never deleting or rewriting historical leave/audit data, in `backend/src/main/java/com/example/leavemanagement/policy/application/HolidayAdministrationService.java` (FR-022, FR-027, FR-032)
- [X] T083 [US3] Implement atomic balance-period allocation with nonoverlap enforcement and `ALLOCATE` movement/audit records in `backend/src/main/java/com/example/leavemanagement/balance/application/BalanceAllocationService.java` (FR-021, FR-027)
- [X] T084 [US3] Implement atomic administrator balance adjustment with row locking, mandatory balance `expectedVersion`, nonblank reason, idempotency, summary update, immutable `ADMIN_ADJUST` movement, audit record, and all-or-nothing stale/failure behavior in `backend/src/main/java/com/example/leavemanagement/balance/application/BalanceAdjustmentService.java` (FR-023, FR-027, FR-028, FR-032)
- [X] T085 [US3] Implement OpenAPI administrator DTOs/controllers for atomic employee/account provisioning and reporting, leave types, policies, holidays, employee balance allocations, and adjustments with required input-only `initialPassword` only on employee creation, consistent closed non-empty duplicate-free role arrays, no credential response fields, `ADMINISTRATOR` checks, mandatory `expectedVersion` on every contract-designated update, and the documented applicable `400`/`401`/`403`/`404`/`409` problems in `backend/src/main/java/com/example/leavemanagement/{people,policy,balance}/api/` (FR-001, FR-002, FR-019–FR-023, FR-032)
- [X] T086 [P] [US3] Implement typed administrator configuration API functions and contract types with a creation-only required `initialPassword`, shared closed non-empty duplicate-free role types, credential-free response types, required `expectedVersion` update fields, and documented validation/authorization/not-found/stale-version problem mapping in `frontend/src/features/admin/api.ts` and `frontend/src/shared/types/admin.ts`
- [X] T087 [P] [US3] Build the administrator employee list/editor with required creation-only password input, non-empty duplicate-free closed account roles, active state, manager assignment, optimistic version handling, no password material in returned/displayed employee data, and pagination in `frontend/src/features/admin/employees/AdminEmployeesPage.tsx` (FR-001, FR-002, FR-019)
- [X] T088 [P] [US3] Build leave-type and effective-policy pages with configurable balance tracking (without an allowance-basis control), half-days, weekly offs, holiday treatment, rejection comments, cancellation cutoff, effective dates, and loaded leave-type version submission for type updates/policy creation in `frontend/src/features/admin/policies/AdminLeavePoliciesPage.tsx` (FR-020, FR-021, FR-033)
- [X] T089 [P] [US3] Build company holiday list/create/edit/soft-deactivate flows that send the loaded holiday version and refresh safely on stale conflicts in `frontend/src/features/admin/holidays/AdminHolidaysPage.tsx` (FR-022, FR-032)
- [X] T090 [P] [US3] Build the sole-MVP employee balance allocation and adjustment views with mandatory reason, loaded balance version on adjustments, ledger-oriented confirmation, and stale/conflict refresh handling in `frontend/src/features/admin/balances/AdminEmployeeBalancesPage.tsx` (FR-021, FR-023, FR-032)
- [X] T091 [US3] Build the administrator dashboard with links and configuration health summaries in `frontend/src/features/dashboard/AdminDashboardPage.tsx` and register administrator-only configuration routes in `frontend/src/app/router/AppRouter.tsx`
- [X] T092 [US3] Run the US3 suites and verify a new manager assignment, policy, weekly-off rule, holiday, and sole-mechanism employee balance allocation immediately affect the downstream scoped calculation workflow in `backend/src/test/java/com/example/leavemanagement/integration/AdministrationWorkflowTest.java` (SC-006)

**Checkpoint**: User Story 3 supplies all configurable people, policy, holiday, and balance prerequisites without hard-coded organization rules.

---

## Phase 6: User Story 4 - Cancel an Eligible Leave Request (Priority: P2)

**Goal**: An employee can cancel only their own pending request or eligible approved future leave, with exact reservation release or consumption restoration; administrators can perform separate, reasoned exceptional corrections.

**Independent Test**: Cancel pending and approved-before-cutoff requests and verify `CANCELLED`, inactive slots, exactly-once ledger restoration, history/audit, and updated balance; test exactly at/after cutoff, foreign ownership, duplicate calls, and terminal states with no mutation, then verify a reasoned administrator correction uses compensating records.

### Tests for User Story 4

- [X] T093 [P] [US4] Write cancellation eligibility unit tests for pending requests, approved future leave, organization-time-zone cutoff boundaries, terminal states, ownership, and allowed transitions in `backend/src/test/java/com/example/leavemanagement/unit/CancellationPolicyTest.java` (FR-012, FR-013)
- [X] T094 [P] [US4] Write transactional employee-cancellation tests for ownership, mandatory `expectedVersion` and stale no-mutation behavior, exact pending reservation release/approved consumption restoration, slot deactivation, idempotency, concurrent duplicate commands, immutable ledger/status/audit writes, and full request/balance/ledger/occupancy/history/audit rollback in `backend/src/test/java/com/example/leavemanagement/integration/LeaveCancellationTransactionTest.java` (FR-013, FR-025, FR-027, FR-028, FR-032)
- [X] T095 [P] [US4] Write administrator correction tests for authorization, mandatory reason/`expectedVersion`, stale no-mutation, the exact `PENDING -> CANCELLED`, `APPROVED -> CANCELLED`, and fully revalidated `REJECTED -> PENDING` transitions with their release/restore/re-reserve and occupancy effects, every same-status/other transition forbidden, immutable ledger/history/audit additions, idempotency, and complete rollback in `backend/src/test/java/com/example/leavemanagement/integration/ExceptionalCorrectionTransactionTest.java` (FR-027, FR-028, FR-032, FR-035; SC-010)
- [X] T096 [P] [US4] Write MockMvc contract/security tests for employee cancellation and the closed administrator correction actions `CANCEL_PENDING`, `CANCEL_APPROVED`, and `REOPEN_REJECTED`, including ownership/administrator permissions, mandatory `expectedVersion`, and stable 400/403/409/422 problems in `backend/src/test/java/com/example/leavemanagement/contract/LeaveCancellationApiContractTest.java` (FR-013, FR-032, FR-035)
- [X] T097 [P] [US4] Write frontend tests for cancellation visibility, ownership/cutoff messaging, mandatory loaded `expectedVersion`, stale refresh, balance refresh, and duplicate clicks plus administrator correction's exact three actions, reason/version fields, forbidden-state hiding, and errors in `frontend/src/features/leave-requests/LeaveCancellation.test.tsx` and `frontend/src/features/admin/corrections/AdminCorrection.test.tsx`

### Implementation for User Story 4

- [X] T098 [US4] Implement cancellation eligibility evaluation using request ownership, status, policy snapshot/current rules, start date, organization time zone, and exact cutoff instant in `backend/src/main/java/com/example/leavemanagement/request/domain/CancellationPolicy.java` (FR-012, FR-013)
- [X] T099 [US4] Implement atomic own cancellation with ownership enforcement, request/balance locks, mandatory `expectedVersion` and idempotency checks, exact balance-line release or restoration plus immutable ledger movement, slot deactivation, `CANCELLED` history, and audit event in `backend/src/main/java/com/example/leavemanagement/request/application/CancelLeaveRequestService.java` (FR-013, FR-025, FR-027, FR-028)
- [X] T100 [US4] Implement administrator-only, `expectedVersion`-guarded correction actions for exactly `PENDING -> CANCELLED` (release reservation/deactivate occupancy), `APPROVED -> CANCELLED` (restore consumption/deactivate occupancy), and `REJECTED -> PENDING` (revalidate policy/dates/duration/overlap/balance, re-reserve tracked balance, reactivate occupancy), rejecting same-status/every other transition and atomically appending immutable ledger/history/audit without rewriting prior records in `backend/src/main/java/com/example/leavemanagement/request/application/ExceptionalCorrectionService.java` (FR-027, FR-028, FR-035)
- [X] T101 [US4] Implement OpenAPI cancel and closed-action correction DTO/controller operations with mandatory `expectedVersion` and stable missing-version, stale-version, cutoff, forbidden-transition, and access errors in `backend/src/main/java/com/example/leavemanagement/request/api/LeaveCancellationController.java` and `backend/src/main/java/com/example/leavemanagement/request/api/AdminCorrectionController.java` (FR-013, FR-032, FR-035)
- [X] T102 [P] [US4] Add typed cancellation and closed-action correction API calls with mandatory `expectedVersion` plus missing/stale/transition problem-code mapping in `frontend/src/features/leave-requests/api.ts` and `frontend/src/features/admin/api.ts`
- [X] T103 [P] [US4] Add accessible own-request cancellation controls and confirmation that submit the loaded request version, explain eligibility/cutoff, and refresh after success or stale conflict in `frontend/src/features/leave-requests/LeaveRequestHistoryPage.tsx` (FR-013, FR-032)
- [X] T104 [P] [US4] Build the administrator correction dialog exposing only the source-valid `CANCEL_PENDING`, `CANCEL_APPROVED`, or `REOPEN_REJECTED` action, mandatory reason and loaded request version, explicit exceptional-action warning, stale refresh, and audit confirmation in `frontend/src/features/admin/corrections/AdminCorrectionDialog.tsx` (FR-032, FR-035)
- [X] T105 [US4] Run US4 suites and reconcile balance summaries against immutable movement lines after each cancellation/correction path in `backend/src/test/java/com/example/leavemanagement/integration/LeaveCancellationTransactionTest.java`

**Checkpoint**: User Story 4 preserves balances and immutable history across normal cancellation and administrator-only correction.

---

## Phase 7: User Story 5 - Review Organization Leave Activity (Priority: P2)

**Goal**: An administrator can inspect organization-wide requests, immutable audit history, and paginated leave summaries by status and leave type for a selected reporting period.

**Independent Test**: Seed requests across employees, statuses, types, and dates; as an administrator verify filtered paginated organization requests, exact report buckets, and audit-event fields, while non-administrators receive 403 without data disclosure.

### Tests for User Story 5

- [ ] T106 [P] [US5] Write PostgreSQL reporting tests for inclusive period boundaries, status/type aggregation, decimal-day conversion, stable sorting, pagination bounds, and relevant indexes in `backend/src/test/java/com/example/leavemanagement/integration/LeaveReportingRepositoryTest.java` (FR-029, FR-030)
- [X] T107 [P] [US5] Write audit query tests for append-only behavior, required actor/action/time/status/reason fields, entity filters, stable ordering, pagination, and administrator-only access in `backend/src/test/java/com/example/leavemanagement/integration/AuditHistoryQueryTest.java` (FR-027, FR-028)
- [X] T108 [P] [US5] Write MockMvc contract/security tests for organization request search, leave summary, and audit-event endpoints in `backend/src/test/java/com/example/leavemanagement/contract/AdminReportingApiContractTest.java` (FR-029, FR-030)
- [ ] T109 [P] [US5] Write frontend tests for administrator report period validation, status/type summary states, organization request pagination, audit filtering, safe errors, keyboard access, and responsive layouts in `frontend/src/features/reports/AdminReports.test.tsx`

### Implementation for User Story 5

- [ ] T110 [P] [US5] Implement paginated organization request projections with date/status filters and stable sorting in `backend/src/main/java/com/example/leavemanagement/reporting/persistence/OrganizationLeaveRequestRepository.java` (FR-029)
- [ ] T111 [P] [US5] Implement status/type aggregate report queries with inclusive selected-period semantics and integer-unit-to-day conversion in `backend/src/main/java/com/example/leavemanagement/reporting/persistence/LeaveSummaryRepository.java` (FR-030)
- [ ] T112 [P] [US5] Implement immutable audit event filtering/pagination projections in `backend/src/main/java/com/example/leavemanagement/audit/application/AuditQueryService.java` (FR-027, FR-028)
- [X] T113 [US5] Implement administrator reporting services and OpenAPI DTO/controllers for organization requests, leave summaries, and audit events in `backend/src/main/java/com/example/leavemanagement/reporting/application/LeaveReportingService.java`, `backend/src/main/java/com/example/leavemanagement/reporting/api/AdminReportingController.java`, and `backend/src/main/java/com/example/leavemanagement/audit/api/AdminAuditController.java` (FR-027–FR-030)
- [X] T114 [P] [US5] Implement typed report, organization-request, and audit API functions/types in `frontend/src/features/reports/api.ts` and `frontend/src/shared/types/reporting.ts`
- [X] T115 [US5] Build administrator organization request table, selected-period status/type summaries, and audit-event browser with loading/empty/error states in `frontend/src/features/reports/AdminLeaveReportsPage.tsx` and `frontend/src/features/reports/AdminAuditHistoryPage.tsx` (FR-027–FR-030)
- [X] T116 [US5] Add report and audit summaries/links to the administrator dashboard and register administrator-only routes in `frontend/src/features/dashboard/AdminDashboardPage.tsx` and `frontend/src/app/router/AppRouter.tsx`
- [ ] T117 [US5] Run US5 suites and compare report totals with source request units across all four statuses and multiple leave types in `backend/src/test/java/com/example/leavemanagement/integration/LeaveReportingRepositoryTest.java`

**Checkpoint**: User Story 5 provides accurate, scoped organization oversight and complete immutable audit visibility.

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: Validate the whole approved system, finish local-demo support, and address security, accessibility, performance, and documentation requirements that span stories.

- [ ] T118 [P] Add profile-isolated local-demo Flyway seed data for administrator, manager, employee, reporting relationships, policies, holidays, balances, and requests in `backend/src/main/resources/db/local-demo/R__local_demo_data.sql`, with credentials supplied only from local environment configuration
- [ ] T119 [P] Add OpenAPI drift and CSRF contract validation to Maven verification against `specs/001-employee-leave-management/contracts/openapi.yaml`, including local reference resolution, required input-only `initialPassword`, shared closed/non-empty/unique role schemas, absence of credential response fields, login missing/malformed/validation-invalid body coverage as `400 VALIDATION_FAILED`, operation-specific applicable `400`/`401`/`403`/`404`/`409` responses, and a matrix proving every operation requiring `X-XSRF-TOKEN` documents and returns `403 ACCESS_DENIED` for missing/invalid tokens without protected state mutation, in `backend/pom.xml`, `backend/src/test/java/com/example/leavemanagement/contract/OpenApiValidationTest.java`, and `backend/src/test/java/com/example/leavemanagement/security/CsrfContractTest.java` (FR-001, FR-002, FR-032; SC-003)
- [ ] T120 [P] Add full backend authorization coverage for every operation in `contracts/authorization.md`, including employee ownership, the employee same-manager/active coworker calendar predicate and privacy projection, manager direct-report scope, unconditional self-approval prevention, administrator-only operations, multi-role endpoint semantics, and forbidden no-mutation assertions in `backend/src/test/java/com/example/leavemanagement/security/AuthorizationMatrixTest.java` (FR-002, FR-003, FR-015, FR-016, FR-031, FR-035; SC-003, SC-009, SC-010)
- [ ] T121 [P] Add cross-workflow reconciliation for atomic request state, balance reservation/consumption/restoration, occupancy, status history, and audit across submit/approve/reject/cancel/allocate/adjust/all three corrections, including direct update/delete attempts proving ledger and audit rows immutable in `backend/src/test/java/com/example/leavemanagement/integration/LedgerAuditReconciliationTest.java` (FR-024–FR-028, FR-035; SC-004, SC-007, SC-010)
- [ ] T122 [P] Add responsive and keyboard-accessibility tests for role navigation, forms, dialogs, tables/cards, status text, focus management, and agenda views in `frontend/src/test/AccessibilityResponsive.test.tsx`
- [ ] T123 Implement query timing assertions or explain-plan checks for indexed interactive, calendar, and report queries in `backend/src/test/java/com/example/leavemanagement/integration/QueryPerformanceTest.java` using the plan's 500 ms/2 s targets at representative MVP fixture size
- [ ] T124 Implement focused Playwright smoke tests for employee login/preview/submit, manager approve/reject, employee balance/status refresh, eligible cancellation restoration, and administrator audit visibility in `e2e/tests/leave-management-smoke.spec.ts`
- [ ] T125 [P] Add local-demo test-data reset/cleanup support that never targets production profiles in `e2e/global-setup.ts` and `backend/src/main/java/com/example/leavemanagement/shared/config/LocalDemoConfiguration.java`
- [ ] T126 Run `backend/mvnw.cmd verify`, `frontend` lint/typecheck/test/build, and `e2e` smoke tests; record commands and any platform-specific notes in `README.md`
- [ ] T127 Execute every acceptance scenario in `specs/001-employee-leave-management/quickstart.md` against a clean local PostgreSQL volume and document verified results in `specs/001-employee-leave-management/quickstart.md`
- [ ] T128 Conduct the timed employee and manager usability checks and validation-message evaluation defined by SC-001, SC-005, and SC-008, recording evidence without changing requirements in `specs/001-employee-leave-management/quickstart.md`
- [ ] T129 Review dependency versions, session/cookie/CSRF settings, secret handling, safe logging, and production profile defaults in `backend/pom.xml`, `backend/src/main/resources/application.yml`, `frontend/package-lock.json`, and `.env.example`
- [ ] T130 [P] Add a cross-contract optimistic-concurrency matrix test proving mandatory `expectedVersion`, `400 VALIDATION_FAILED` when absent, `409 STALE_VERSION` without current-version disclosure, version increment on success, and zero mutation on stale approval, rejection, employee cancellation, administrator correction, employee/reporting update, leave-type update, policy creation, holiday update/soft deactivation, and balance adjustment in `backend/src/test/java/com/example/leavemanagement/contract/ExpectedVersionContractTest.java` (FR-019, FR-020, FR-022–FR-028, FR-032, FR-035; SC-003, SC-004, SC-010)
- [ ] T131 Re-run the traceability audit and reconcile implemented task/test identifiers for FR-001–FR-035 and SC-001–SC-010 in `specs/001-employee-leave-management/contracts/traceability.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: Starts immediately; T001 and T002 establish the backend/frontend manifests needed by later setup tasks.
- **Phase 2 — Foundational**: Depends on Phase 1 and blocks every story. Migrations T014–T018 run in order; entity mappings T019–T023 can proceed in parallel after their migrations are drafted; repositories and services follow their mapped entities.
- **Phase 3 — US1**: Depends on Phase 2 and supplies the shared request, calculation, balance reservation, history, and employee UI capabilities consumed by later workflow stories.
- **Phase 4 — US2**: Depends on US1 because manager decisions act on submitted pending requests and reservation lines.
- **Phase 5 — US3**: Depends on Phase 2 for its own administration increment, but completing it before end-to-end US1/US2 validation provides the configurable people, policy, holiday, and balance data those stories require.
- **Phase 6 — US4**: Depends on US1 request/reservation behavior; approved cancellation tests also depend on US2 approval behavior. Exceptional correction depends on US3 administrator security.
- **Phase 7 — US5**: Depends on persisted requests/history from US1–US4 and administrator security from US3.
- **Phase 8 — Polish**: Depends on all stories selected for release.

### User Story Completion Order

```text
Setup -> Foundational -> US1 Submit/Track -> US2 Decide
                         |                  |
                         +-> US3 Admin -----+-> US4 Cancel/Correct -> US5 Report/Audit -> Polish
```

- **US1 (P1)** is the suggested first functional MVP after setup/foundation.
- **US2 (P1)** requires US1's pending requests and reservations.
- **US3 (P1)** can be developed alongside US1 after foundation in separate files, but its configuration workflow must be complete before the full quickstart demonstration.
- **US4 (P2)** requires US1 and the approval path from US2; administrator correction also uses US3 authorization/configuration foundations.
- **US5 (P2)** is independently testable with fixtures but is delivered after earlier stories so it can report their real events.

### Within Each User Story

- Write the listed tests first and confirm they fail for the intended missing behavior.
- Implement domain rules before application transactions, application transactions before controllers, and controllers before frontend integration.
- Keep controller DTOs separate from persistence entities and keep cross-module access behind application/domain interfaces.
- Run the story checkpoint suites before starting dependent story work.

---

## Parallel Execution Examples

### User Story 1

```text
T035 duration unit tests || T036 overlap repository tests || T037 reservation transaction tests || T038 API contract tests || T039 frontend tests
After T041: T042 reference queries || T043 balance operations
After backend API completion: T053 history UI || T054 employee dashboard || T055 employee calendar
```

### User Story 2

```text
T058 authorization tests || T059 decision transaction tests || T060 API contract tests || T061 frontend tests
After decision services: T066 manager calendar/dashboard projections || T068 typed frontend API
```

### User Story 3

```text
T073 people tests || T074 policy tests || T075 balance tests || T076 holiday tests || T077 API tests || T078 frontend tests
After backend APIs: T087 employee UI || T088 policy UI || T089 holiday UI || T090 balance UI
```

### User Story 4

```text
T093 policy unit tests || T094 cancellation transactions || T095 correction transactions || T096 API tests || T097 frontend tests
After APIs: T103 employee cancellation UI || T104 administrator correction UI
```

### User Story 5

```text
T106 reporting repository tests || T107 audit query tests || T108 API contract tests || T109 frontend tests
After foundation: T110 organization query || T111 summary query || T112 audit query
```

---

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational phases.
2. Complete US1 in test-first order.
3. Stop at the US1 checkpoint and demonstrate login, calculation, atomic reservation, overlap prevention, history, dashboard, and employee calendar.
4. Add the remaining P1 stories US2 and US3 to complete the operational leave workflow.

### Incremental Delivery

1. **Foundation**: runnable secured applications and migrated PostgreSQL schema.
2. **US1**: employee submission/tracking MVP.
3. **US2**: direct-manager decisions and team view.
4. **US3**: administrator-managed organization configuration.
5. **US4**: balance-safe cancellation and exceptional correction.
6. **US5**: organization reports and audit review.
7. **Polish**: full contract, security, accessibility, performance, quickstart, and end-to-end validation.

## Notes

- `[P]` marks only tasks that operate in different files with no unfinished same-phase dependency.
- State-changing services must keep request, slots, balance summaries, immutable movements, status history, and audit writes in one transaction.
- Managers have direct-report scope only and can never self-approve; administrator correction is a separate, reasoned path.
- Applied Flyway migrations are immutable; later schema changes require new forward migrations.
- Organization rules remain configured data—never hard-code leave counts, weekly offs, holidays, rejection comments, or cancellation cutoffs.
