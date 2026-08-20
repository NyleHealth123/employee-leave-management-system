# Specification-to-Design Traceability

This matrix keeps Phase 1 design tied to the approved specification. Task/test IDs are design-stage mappings from the existing `tasks.md`; `$speckit-tasks` reconciliation must retain or deliberately supersede them when it incorporates this updated design.

| Specification coverage | Design location | Contract, verification, and current task/test mapping |
|---|---|---|
| FR-001–FR-003: authentication, logout, roles, ownership | `plan.md` authorization; atomic administrator employee/account provisioning; input-only password hashing; non-empty duplicate-free closed roles; UserAccount/Role/EmployeeProfile; ownership predicates | Auth/employee contracts and explicit operation problem responses; `authorization.md`; quickstart 1 and 6; T027–T033, T038, T047, T049, T058, T073, T077, T079, T085–T087, T119–T120 (tests T027, T032, T038, T058, T073, T077, T119–T120) |
| FR-004–FR-005: dashboard, leave types, balances | Frontend design; LeaveType/LeavePolicyVersion/LeaveBalance | Dashboard/balance/type operations; T038, T042–T043, T048–T050, T054 (tests T038, T039) |
| FR-006–FR-008: request input, calculation, validation | LeaveRequest/Slot; calculation rules | Calculate/submit operations; quickstart 2; T035, T038–T045, T049, T052, T057 (tests T035, T038, T039, T057) |
| FR-009: active overlap prevention | Transaction strategy; active-slot partial unique index | `409 LEAVE_OVERLAP`; quickstart 3; T036–T038, T045–T046 (tests T036–T038) |
| FR-010–FR-011: mandatory tracked-balance reservation and history | LeaveBalance/BalanceLine/Movement; no separate validation flag | Submit/insufficient-balance contracts; quickstart 2–3; T037–T039, T043, T045, T047, T049, T052–T053 (tests T037–T039) |
| FR-012–FR-013: states, ownership, cancellation cutoff/version | Exact transition table; organization timezone | Required-version cancel contract; quickstart 5; T093–T105 (tests T093–T097, T105) |
| FR-014–FR-018: manager queue/detail/decisions/scope/calendar | Direct-report predicate; self-decision guard | Manager endpoints; quickstart 4; T058–T072 (tests T058–T061, T072) |
| FR-019: employees and manager assignments | People module; EmployeeProfile version | Required-version administrator employee contract; quickstart 6; T073, T077, T079–T080, T085, T087, T092 (tests T073, T077, T092) |
| FR-020–FR-022: policies, sole balance allocation, holiday soft deactivation | LeaveType/PolicyWeeklyOff/CompanyHoliday/LeaveBalance | Required-version type/policy/holiday contracts; quickstart 2, 6; T074, T076–T077, T081–T083, T085, T088–T089, T092 (tests T074, T076–T077, T092) |
| FR-023: auditable balance adjustment | LeaveBalanceMovement/AuditEvent; balance lock/version | Adjustment contract; quickstart 6; T075, T077, T084–T085, T090 (tests T075, T077) |
| FR-024–FR-026: conversion, release/restore, revalidation | Transaction/lock table; balance-line transitions | Approve/reject/cancel contracts; quickstart 4–5; T059, T064–T065, T072, T121 (tests T059, T072, T121) |
| FR-027–FR-028: immutable audit and status trace | AuditEvent/StatusHistory; append-only constraints | Audit API and rollback; quickstart 4–8; T037, T045, T053, T059, T064–T065, T075, T083–T084, T094–T095, T099–T100, T107, T112–T115, T121 (tests T037, T059, T075, T094–T095, T107, T121) |
| FR-029–FR-030: organization requests and summaries | Reporting module/request indexes | Administrator report contracts; quickstart 6; T106, T108, T110–T117 (tests T106, T108–T109, T117) |
| FR-031: employee/team calendars and holidays | Dedicated EmployeeTeamCalendarEntry; exact same-manager/active/status predicate | Privacy-safe employee schema and manager schema; quickstart 7; T038, T042, T048–T049, T055, T057 (tests T038, T057; cross-cutting T120, T127) |
| FR-032: safe errors and stale semantics | Shared Problem schema; operation-specific `400`/`401`/`403`/`404`/`409` responses; `STALE_VERSION` behavior | API/security/frontend errors; T025–T026, T030–T031, T046, T051, T063, T077, T096, T101, T119–T120, T130 (tests T025, T030, T077, T096, T119–T120, T130) |
| FR-033–FR-034: configurable/consistent rules | Effective policy/shared calculator | Preview/submit/decision/cancel/report checks; T035, T041, T074, T081, T088, T092 (tests T035, T074, T092) |
| FR-035: exact administrator corrections | Three-row transition table; correction transaction/version | Closed correction-action enum; quickstart 6; T095–T101, T104–T105, T121 (tests T095–T097, T105, T121) |

| FR-036: repeatable local-demo organization dataset | Profile-isolated local-demo Flyway location; deterministic existing-model seed data; environment-only credentials; fail-closed production boundaries | T118 creates `backend/src/main/resources/db/local-demo/R__local_demo_data.sql` with at least 50 (preferably approximately 50–60) synthetic employees, administrator/manager/employee accounts, multiple managers with direct reports, realistic reporting relationships, leave types/policies, supported weekly offs, holidays, balances, and representative `PENDING`/`APPROVED`/`REJECTED`/`CANCELLED` requests; T124 exercises employee/manager/administrator workflows and reporting/audit usability; T125 safely resets/recreates the dataset; T126 runs complete verification; T127 verifies a clean-volume quickstart; T131 audits final evidence, including no duplicates/corruption, production isolation, credential secrecy, and no hardcoded demo passwords. |

## Success criteria verification

| Success criterion | Planned evidence and current task/test mapping |
|---|---|
| SC-001 | Timed employee request flow: T039, T052, T128 |
| SC-002 | Calculation unit/integration/acceptance evidence: T035, T041, T044, T057, T127 |
| SC-003 | Full authorization/no-mutation matrix: T027, T038, T058, T060, T077, T096, T108, T120, T127 |
| SC-004 | Transaction/ledger reconciliation including rejection release: T037, T059, T075, T094, T095, T121, T124, T127 |
| SC-005 | Timed manager queue-to-decision flow: T061, T069, T128 |
| SC-006 | Administrator configuration downstream effects: T073–T092, T127 |
| SC-007 | Required audit and rollback evidence: T037, T059, T075, T094, T095, T107, T121, T127 |
| SC-008 | Safe/actionable validation and accessibility: T025, T030, T039, T061, T078, T097, T109, T122, T128 |
| SC-009 | Same-manager active-employee calendar scope and exact field allowlist: T038, T048, T055, T120, T127 |
| SC-010 | Exact correction whitelist/effects and forbidden no-mutation cases: T095–T101, T104–T105, T120, T121, T127 |

| SC-011 | Local-demo acceptance: T118 verifies at least 50 (preferably approximately 50–60) synthetic employees, an administrator, multiple managers with direct reports, employee accounts, valid reporting relationships, leave types/policies, supported weekly offs, holidays, balances, and representative `PENDING`/`APPROVED`/`REJECTED`/`CANCELLED` requests; T124 verifies employee, manager, administrator, reporting, audit, and E2E usability; T125 verifies deterministic reset/recreation without unexpected duplicates or corruption and refuses production; T126 runs the full verification set; T127 repeats clean-volume acceptance; T131 records production isolation, credential secrecy, and no hardcoded demo-password evidence. |

Implementation evidence (commits, executed test results, and acceptance-run outcomes) is intentionally appended later; known design-stage mappings are not deferred.
