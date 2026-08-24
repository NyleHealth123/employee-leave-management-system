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
| Backend | `http://localhost:8080` | Spring profile `local` for ordinary development; use `local-demo` to additionally activate profile-isolated demo data |
| Frontend | `http://localhost:5173` | Vite proxies `/api` to backend and uses strict fixed port |
| Production route model | One origin | SPA and `/api` share an origin; session cookies and CSRF remain enabled |

Create a local environment file from a committed example and set database credentials plus local-only demo account passwords. The real environment file must remain ignored. `local` is the ordinary development profile. `local-demo` is the development/demo profile that additionally activates `classpath:db/local-demo`; normal `local`, test, and production configurations activate only the standard migration location and must never load `classpath:db/local-demo`. Demo seeding must fail closed outside `local-demo`, and missing required demo credential configuration must fail local-demo startup/setup rather than use insecure defaults.

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

### Local-demo dataset acceptance (FR-036 / SC-011)

After a clean `local-demo` initialization or supported reset, verify and record the following before running the workflow scenarios:

1. **Employee population**: at least 50 synthetic employees exist, preferably approximately 50–60, and no real personal information is present.
2. **Role population**: at least one administrator, multiple managers, and employee accounts exist.
3. **Reporting structure**: managers have direct reports and every seeded manager relationship is valid for manager-scope authorization.
4. **Business configuration**: leave types, effective policies, supported weekly-off configuration, holidays, and employee leave balances exist.
5. **Representative leave data**: valid requests exist in `PENDING`, `APPROVED`, `REJECTED`, and `CANCELLED` states.
6. **Workflow usability**: employee workflows have usable demo accounts/data; manager scope contains direct-report requests; administrator organization views contain enough rows for reporting and pagination; audit and reporting views contain representative data.
7. **Repeatability**: clean reset/recreation restores the expected dataset, and repeating the supported setup does not create unexpected duplicate or corrupt rows.
8. **Credential safety**: credentials come from local environment configuration; no plaintext password is stored in SQL, source, logs, or API responses; missing required demo credentials cause local-demo startup/setup to fail rather than use insecure defaults.
9. **Production isolation**: production does not activate `classpath:db/local-demo`, production startup does not depend on demo data, reset/cleanup refuses production configuration, and no production migration introduces demo rows.

Record these checks as part of T127 acceptance evidence.

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
5. As an administrator, create an employee with `initialPassword` and one or more unique values from `EMPLOYEE`, `MANAGER`, and `ADMINISTRATOR`; verify an empty, duplicate, or unknown role set and a missing password return `400 VALIDATION_FAILED`.
6. Verify the created account can authenticate with the supplied password, the stored credential is a secure hash, and no employee, principal, audit, or other response contains `initialPassword`, a plaintext password, or a password hash.

Expected result: all protected access is authenticated, role-scoped, and CSRF-protected.

### 2. Duration calculation and submission

1. Configure a leave type that excludes selected weekly offs and company holidays and permits half-days.
2. Preview a full-day range containing both exclusions and verify chargeable dates/days.
3. Preview AM and PM half-day requests and verify each returns `0.5` day.
4. Submit the valid request and verify status `PENDING`, active occupancy slots, reserved balance, status history, and an audit event.
5. For every `tracksBalance=true` type, verify the API offers no switch that disables validation/reservation and that insufficient unreserved balance rejects the complete submission.

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
6. Before the successful rejection, record allocated, reserved, consumed, and available units plus the request version. Reject using that `expectedVersion`, then verify the reservation line changes `RESERVED -> RELEASED`, reserved units decrease by exactly the request units, available units increase by exactly those units, and allocated/consumed units do not change.
7. Verify exactly one `RELEASE_RESERVED` ledger movement, inactive calendar occupancy, a `PENDING -> REJECTED` status-history row, and an immutable rejection audit event while the original submission history remains intact.
8. Retry with the stale pre-rejection version and verify `409 STALE_VERSION` with no extra movement, occupancy, history, audit, or balance change.

Expected result: only the direct manager can decide, self-approval is always blocked, comment policy is enforced, and rejection atomically releases the pending reservation and restores availability with matching immutable ledger/audit evidence.

### 5. Cancellation and restoration

1. Cancel a pending request and verify reserved units are released once.
2. Cancel approved future leave before its configured cutoff and verify consumed units are restored once.
3. Attempt cancellation exactly at and after the organization-time-zone cutoff.
4. Omit `expectedVersion`, then use a stale version, and verify `400 VALIDATION_FAILED` and `409 STALE_VERSION` respectively with no mutation.

Expected result: permitted cancellation is fully atomic and late self-cancellation returns `422 CANCELLATION_CUTOFF_PASSED` without changing balance or audit state.

### 6. Administration and audit

1. Create an employee and associated password-backed account together, then update its manager relationship and verify it immediately controls manager scope. Verify create requires input-only `initialPassword`, all create/update/response role arrays use the same non-empty duplicate-free closed enum, and no response exposes password material.
2. Create an effective-dated leave policy and holiday and verify new calculations use them. Soft-deactivate the holiday with its required `expectedVersion`; verify new/recalculated requests ignore it while historical request snapshots, status history, and audit records remain unchanged.
3. Adjust a balance with a reason and verify summary, immutable movement, and audit event.
4. Perform each allowed correction with a reason and current `expectedVersion`: `PENDING -> CANCELLED` releases reserved balance and deactivates occupancy; `APPROVED -> CANCELLED` restores consumed balance and deactivates occupancy; `REJECTED -> PENDING` revalidates current policy/dates/duration/overlap/available balance, re-reserves tracked balance, and restores pending occupancy. Verify immutable ledger/status/audit additions rather than rewriting prior history.
5. Attempt same-status correction plus `CANCELLED -> APPROVED`, `REJECTED -> APPROVED`, `APPROVED -> PENDING`, and `CANCELLED -> PENDING`; verify `409 INVALID_STATUS_TRANSITION` and no request, balance, ledger, occupancy, history, or audit mutation.
6. Repeat employee/reporting, leave-type, policy-version, holiday, correction, and balance-adjustment updates with stale `expectedVersion` values and verify `409 STALE_VERSION` without mutation.
7. View organization requests and a leave summary filtered by period.

Expected result: configuration is data-driven, corrections are administrator-only, and every material action is traceable.

### 7. Employee team-calendar privacy

1. Create the viewer, an active coworker with the same direct manager, an inactive coworker with that manager, an active employee under a different manager, and a viewer with no manager. Give each pending/approved and terminal requests as applicable.
2. Query the employee team calendar as the first viewer and verify it includes only their own qualifying entries plus pending/approved entries for the active same-manager coworker.
3. Verify inactive employees, different-manager employees, and rejected/cancelled entries are absent. Verify a viewer without a manager receives only their own qualifying entries.
4. Inspect every response object and verify its exact fields are `employeeDisplayName`, `startDate`, `endDate`, and `status`; confirm reason, balance, decision comment, leave type, duration mode, identifiers, status history, and audit history are absent.

Expected result: employee calendar scope and field-level privacy satisfy FR-031 and SC-009 without reusing private request DTOs.

### 8. Transaction rollback

Use an integration-test failure injection to make required history/audit persistence fail during submission, approval, rejection, cancellation, and adjustment.

Expected result: request state, occupancy, balance summaries, movements, and history all roll back; no business update survives without required audit history.

## T127 acceptance evidence

Evidence baseline: GitHub Actions Verify run #23 at commit `2186fd4` passed the backend PostgreSQL/Testcontainers suite, frontend suite, and the existing Playwright smoke test against PostgreSQL plus a freshly reset `local-demo` stack. T127 adds the explicitly identified coverage below. Rows marked **hosted rerun required** are implemented but are not acceptance-complete until the updated Verify workflow passes them; therefore T127 remains incomplete.

### Local-demo dataset checklist

| ID | Actor and prerequisite state | Exact action | Expected result | Automated evidence | Status / additional verification |
|---|---|---|---|---|---|
| D1 | System; clean `local-demo` database | Run the supported seed/reset | At least 50 synthetic employees and no real PII | `LocalDemoDatasetTest.seedsOrganizationStructureConfigurationAndRepresentativeStatuses`; `e2e/global-setup.ts` | Verified in hosted run #23 |
| D2 | System; seeded database | Count assigned application roles | Administrator, multiple managers, and employee accounts exist | `LocalDemoDatasetTest.seedsOrganizationStructureConfigurationAndRepresentativeStatuses`; global setup response assertions | Verified in hosted run #23 |
| D3 | Manager/employee fixtures; seeded database | Validate every `manager_id` against a manager-role profile | Direct-report relationships are valid for manager scope | `LocalDemoDatasetTest.seedsOrganizationStructureConfigurationAndRepresentativeStatuses`; manager portion of `leave-management-smoke.spec.ts` | Verified in hosted run #23 |
| D4 | System; seeded database | Query types, policies, weekly offs, holidays, and balances | Required business configuration is present | `LocalDemoDatasetTest.seedsOrganizationStructureConfigurationAndRepresentativeStatuses` | Verified in hosted run #23 |
| D5 | System; seeded database | Count requests by status | `PENDING`, `APPROVED`, `REJECTED`, and `CANCELLED` all exist | `LocalDemoDatasetTest.seedsOrganizationStructureConfigurationAndRepresentativeStatuses`; global setup status assertions | Verified in hosted run #23 |
| D6 | Employee, manager, administrator; seeded database | Exercise employee submission, manager decisions, admin audit, and paged/report queries | Each role has usable representative data | `leave-management-smoke.spec.ts`; `LeaveReportingRepositoryTest`; `AuditHistoryQueryTest` | Verified in hosted run #23 |
| D7 | Administrator; safety-approved `local-demo` database | Reset twice | IDs/counts/statuses are deterministic and no duplicate logins or corrupt rows appear | `LocalDemoDatasetTest.resetTwiceRecreatesTheSameAuthoritativeDatasetWithoutDuplicates` | Verified in hosted run #23 |
| D8 | System; environment-supplied demo hashes | Start/reset with valid and missing credential configuration | Encoded placeholders are used; missing configuration fails closed; no plaintext fallback exists | `LocalDemoDatasetTest.usesEncodedCredentialPlaceholdersAndProductionConfigurationDoesNotActivateDemoLocation`; `LocalDemoConfigurationSafetyTest` | Verified in hosted run #23 |
| D9 | System; non-demo/production configuration | Inspect migration locations and request reset | Demo migration is absent and reset is refused outside safe `local-demo` PostgreSQL | `LocalDemoConfigurationSafetyTest.refusesProductionAndNonDemoProfiles`; `refusesMissingOrUnverifiableSafetyConfiguration`; `LocalDemoDatasetTest.usesEncodedCredentialPlaceholdersAndProductionConfigurationDoesNotActivateDemoLocation` | Verified in hosted run #23 |

### Workflow acceptance checklist

| ID | Actor and prerequisite state | Exact action | Expected result | Automated evidence | Status / additional verification |
|---|---|---|---|---|---|
| 1.1 | Each seeded employee, manager, and administrator account | Sign in and GET `/api/auth/me` | Only the assigned role and non-secret profile fields are returned | New `quickstart-authentication.spec.ts`; seed-role assertions | **Hosted rerun required** |
| 1.2 | Anonymous caller, then authenticated wrong-role caller | Call protected endpoints | Shared problem response is `401 AUTHENTICATION_REQUIRED` or `403 ACCESS_DENIED` | New `quickstart-authentication.spec.ts`; `AuthenticationSecurityTest.protectedEndpointsReturnSafe401And403Problems` | **Hosted rerun required** for real stack |
| 1.3 | Authenticated demo actor | Logout, then GET `/api/auth/me` | Session is invalid and the call returns `401` | New `quickstart-authentication.spec.ts`; `AuthenticationSecurityTest.logoutInvalidatesTheAuthenticatedSession` | **Hosted rerun required** for real stack |
| 1.4 | Employee principal | Render navigation and directly call a forbidden API | Manager/admin navigation is absent and backend still denies access | `AppShell.test.tsx`; `AuthorizationMatrixTest`; new role-boundary Playwright test | Existing layers verified; real-stack boundary **awaits hosted rerun** |
| 1.5 | Administrator; unique employee identity | Create with password/closed roles, then try missing, empty, duplicate, and unknown values | Valid create succeeds; invalid commands return `400 VALIDATION_FAILED` | `EmployeeAdministrationTest.rejectsEmptyUnknownAndDuplicateRoles`; `AdministrationConfigurationApiContractTest.creationRequiresPasswordAndRoles`; `OpenApiValidationTest` | Verified in hosted run #23 |
| 1.6 | Newly constructed password-backed account | Authenticate correct/wrong password and inspect persisted/response shapes | Secure hash authenticates; no password material is exposed | `EmployeeAdministrationTest.createsAtomicAccountAndProfileWithoutPlaintext`; `AccountAuthenticationTest`; `AdministrationConfigurationApiContractTest.administratorCreateDoesNotExposeCredentialFields`; `OpenApiValidationTest.successfulResponseSchemasNeverExposeCredentialOrSessionSecrets` | Verified in hosted run #23 |
| 2.1 | Administrator-configured type/policy/holiday | Configure exclusions and half-days | Rules are stored as policy data | `LeavePolicyAdministrationTest.createsEffectiveVersionWithConfiguredRules`; `HolidayAdministrationTest`; local-demo fixture | Verified in hosted run #23 |
| 2.2 | Employee; range containing holiday and weekly offs | Preview full-day leave | Exact chargeable and excluded dates/days are authoritative | `AdministrationWorkflowTest.configuredWeeklyOffAndHolidayChangeChargeableWorkflow`; `LeaveDurationCalculatorTest.excludesConfiguredWeeklyOffsAndHolidays`; smoke preview | Verified in hosted run #23 |
| 2.3 | Employee; half-days allowed | Preview AM and PM | Each is `0.5` day | `LeaveDurationCalculatorTest.supportsPermittedHalfDays`; `AdministrationWorkflowTest.configuredHalfDayRuleIsApplied` | Verified in hosted run #23 |
| 2.4 | Employee; valid calculated request | Submit | `PENDING`, active slots, reservation, history, and audit commit | `LeaveSubmissionTransactionTest`; `LedgerAuditReconciliationTest.submitApproveAndEmployeeCancellationReconcileEveryAuthoritativeStore`; smoke submission | Verified in hosted run #23 |
| 2.5 | Employee; balance-tracked type with insufficient unreserved units | Submit without any client bypass | Complete request is rejected and no partial state remains | `LeaveSubmissionTransactionTest.concurrentNonOverlappingSubmissionsHaveOneAtomicLoserWhenBalanceIsInsufficient`; `EmployeeLeaveApiContractTest.calculationAndSubmissionUseServerOwnedIdentityAndAuthoritativeResult` | Verified in hosted run #23 |
| 3.1 | Employee/date with active AM occupancy | Submit AM/full-day, then complementary PM | Overlaps return `409 LEAVE_OVERLAP`; PM remains possible | `LeaveRequestOverlapRepositoryTest.everyActivePartialAndFullDayOverlapShapeIsRejected`; `complementaryHalfDaysAndOtherEmployeesRemainAvailable` | Verified in hosted run #23 |
| 3.2 | Employee; one balance insufficient for two concurrent requests | Submit simultaneously | Exactly one succeeds; loser is stable and leaves no partial rows | `LeaveSubmissionTransactionTest.concurrentNonOverlappingSubmissionsHaveOneAtomicLoserWhenBalanceIsInsufficient`; `LeaveRequestOverlapRepositoryTest.concurrentActiveSlotInsertsAllowExactlyOneWinner` | Verified in hosted run #23 |
| 4.1 | Assigned manager; direct-report pending request | Open and approve | Request becomes `APPROVED` | Manager portion of `leave-management-smoke.spec.ts`; `LedgerAuditReconciliationTest.submitApproveAndEmployeeCancellationReconcileEveryAuthoritativeStore` | Verified in hosted run #23 |
| 4.2 | Same request with reserved balance | Inspect balance/history/audit after approval | Reserved moves to consumed exactly once with immutable history/audit | `LedgerAuditReconciliationTest.submitApproveAndEmployeeCancellationReconcileEveryAuthoritativeStore` | Verified in hosted run #23 |
| 4.3 | Unrelated manager | Open/decide another manager's request | Request is hidden/denied with zero mutation | Smoke out-of-scope request; `AuthorizationMatrixTest.managerScopeIsDirectReportOnlyAndUnrelatedResourcesRemainHidden`; `deniedManagerWriteHasZeroDomainMutationAcrossAllCollaborators` | Verified in hosted run #23 |
| 4.4 | Manager who also has another non-admin role; own request | Attempt approval | Self-approval is denied | `ManagerAuthorizationTest.selfApprovalIsForbiddenEvenWithManagerRole` | Verified in hosted run #23 |
| 4.5 | Assigned manager; policy requires rejection comment | Reject blank, then reject with comment | Blank returns `422 REJECTION_COMMENT_REQUIRED`; supplied comment succeeds | New `LedgerAuditReconciliationTest.requiredRejectionCommentIsEnforcedWithoutMutation`; successful rejection in the same PostgreSQL class and smoke | **Hosted rerun required** for missing-comment database case |
| 4.6 | Pending tracked request with recorded pre-state/version | Reject using current version | Reservation becomes released; only reserved/available change by request units | `LedgerAuditReconciliationTest.rejectReopenAndAdministratorCancellationReserveAndReleaseExactlyOnce` | Verified in hosted run #23 |
| 4.7 | Successfully rejected request | Inspect ledger, occupancy, status history, audit, and original submission history | One release movement; occupancy inactive; rejection history/audit appended | `LedgerAuditReconciliationTest.rejectReopenAndAdministratorCancellationReserveAndReleaseExactlyOnce`; database immutability test | Verified in hosted run #23 |
| 4.8 | Rejected request; stale pre-rejection version | Retry decision | `409 STALE_VERSION`; no extra authoritative rows or balance change | `LedgerAuditReconciliationTest.staleRetryAndForbiddenCorrectionLeaveDatabaseSnapshotUnchanged`; `ExpectedVersionContractTest` | Verified in hosted run #23 |
| 5.1 | Employee; owned pending request | Cancel once | Reserved units release once and request becomes `CANCELLED` | `LeaveCancellationTransactionTest.pendingCancellationReleasesReservationAndWritesAllHistory`; correction reconciliation test | Verified in hosted run #23 |
| 5.2 | Employee; approved future request before cutoff | Cancel | Consumed units restore once | Cancellation portion of smoke; `LeaveCancellationTransactionTest.approvedCancellationRestoresConsumedBalance`; reconciliation test | Verified in hosted run #23 |
| 5.3 | Employee; approved request at/beyond organization-zone cutoff | Attempt cancellation | Returns `422 CANCELLATION_CUTOFF_PASSED` without mutation | `CancellationPolicyTest.cutoffUsesOrganizationTimezoneAtExactInstant`; `LeaveCancellationApiContractTest.cutoffAndForbiddenTransitionProblemsAreStable` | Verified in hosted run #23 |
| 5.4 | Employee; owned request | Omit version, then send stale version | `400` then `409`, with no mutation | `ExpectedVersionContractTest.everyApprovedVersionSensitiveHttpCommandRejectsAMissingExpectedVersion`; `LeaveCancellationTransactionTest.staleVersionDoesNotReadBalanceOrWriteHistory` | Verified in hosted run #23 |
| 6.1 | Administrator; valid employee/account and manager | Create, update manager/roles, then query manager scope | Atomic password-backed account; closed roles/no secrets; relationship governs scope | `EmployeeAdministrationTest`; `AdministrationConfigurationApiContractTest`; `AuthorizationMatrixTest.managerScopeIsDirectReportOnlyAndUnrelatedResourcesRemainHidden` | Verified in hosted run #23 |
| 6.2 | Administrator; effective policy and active holiday | Create policy/holiday, calculate, deactivate holiday with version, recalculate | New calculations use current config; old snapshots/history/audit remain append-only | `LeavePolicyAdministrationTest`; `HolidayAdministrationTest.deactivationPreservesRecordAndAudits`; `AdministrationWorkflowTest`; `LedgerAuditReconciliationTest.ledgerStatusHistoryAndAuditRowsAreDatabaseImmutable` | Verified in hosted run #23 |
| 6.3 | Administrator; existing balance | Adjust with reason | Summary, immutable movement, and audit reconcile | `BalanceAdministrationTest.adjustmentRequiresReasonAndIsAudited`; `LedgerAuditReconciliationTest.allocationAndAdjustmentReconcileSummaryLedgerAndAudit` | Verified in hosted run #23 |
| 6.4 | Administrator; pending, approved, and rejected requests | Apply each allowed correction with reason/current version | Correct balance/occupancy restoration or revalidation; history remains append-only | `LedgerAuditReconciliationTest.rejectReopenAndAdministratorCancellationReserveAndReleaseExactlyOnce`; `administratorApprovedCancellationRestoresConsumptionExactlyOnce`; `ExceptionalCorrectionTransactionTest.rejectedToPendingRevalidatesThenRecreatesOccupancyAndReservation` | Verified in hosted run #23 |
| 6.5 | Administrator; requests in incompatible states | Attempt same-state and all forbidden transitions | `409 INVALID_STATUS_TRANSITION`; no authoritative mutation | Expanded `ExceptionalCorrectionTransactionTest.everyForbiddenTransitionLeavesAllCollaboratorsUntouched`; API contract and database snapshot tests | **Hosted rerun required** for the complete transition matrix |
| 6.6 | Authorized actor; stale versions for every version-sensitive resource | Repeat employee, report/request, type/policy, holiday, correction, and adjustment commands | `409 STALE_VERSION` and no mutation | `ExpectedVersionContractTest.staleVersionResponseMatrixCoversEveryApprovedVersionSensitiveContract`; resource transaction tests | Verified in hosted run #23 |
| 6.7 | Administrator; period and filters supplied | View organization requests and leave summary | Inclusive filtered pages and stored-unit summaries are returned | `LeaveReportingRepositoryTest`; `AdminReportingApiContractTest`; `AdminReports.test.tsx` | Verified in hosted run #23 |
| 7.1 | Viewer plus active/inactive same-team, different-team, and no-manager fixtures with active/terminal requests | Create fixture | All privacy boundary cases exist | `LeaveRequestOverlapRepositoryTest.teamCalendarRepositoryEnforcesSameManagerActiveStatusScopeAndFieldProjection` | Verified in hosted run #23 |
| 7.2 | First viewer | Query employee team calendar | Own qualifying plus active same-manager coworker pending/approved entries only | Same PostgreSQL repository test | Verified in hosted run #23 |
| 7.3 | First and no-manager viewers | Inspect exclusions and no-manager result | Inactive/different-team/terminal absent; no-manager sees own qualifying entries only | Same PostgreSQL repository test | Verified in hosted run #23 |
| 7.4 | Employee calendar caller | Inspect every response object | Exact four public fields; all private request data absent | `EmployeeLeaveApiContractTest.teamCalendarUsesExactPrivacyProjection`; `AuthorizationMatrixTest.employeeTeamCalendarUsesDedicatedPrivacyProjectionAndViewerPredicate` | Verified in hosted run #23 |
| 8.1 | PostgreSQL integration fixture; injected required-history/audit failure | Submit, approve, reject, cancel, and adjust while persistence fails | Entire transaction rolls back across request, slots, balance, movements, history, audit, and versions | `LeaveSubmissionTransactionTest.everyRequiredPersistenceFailureRollsBackTheWholeSubmission`; approval plus new rejection/cancellation/adjustment rollback tests in `LedgerAuditReconciliationTest` | **Hosted rerun required** for the three new PostgreSQL cases |

T127 completion gate: a hosted Verify run must pass the updated backend suite and both Playwright specifications from a fresh T125 reset. Until that evidence exists, keep T127 `[ ]`.

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
