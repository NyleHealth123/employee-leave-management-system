# Authorization Contract

This contract complements `openapi.yaml`. UI route guards are convenience only; Spring Security and application services enforce every rule below.

## Role and scope matrix

| Capability | Employee | Manager | Administrator | Additional scope rule |
|---|---:|---:|---:|---|
| Sign in, sign out, view own principal | Yes | Yes | Yes | Authenticated account must be enabled |
| View own dashboard, balances, requests, history | Yes | Yes | Yes | Always resolved from authenticated employee; client-supplied employee IDs are ignored |
| Preview, submit, cancel own leave | Yes | Yes | Yes | Actor owns request; cancellation status/cutoff applies |
| View basic employee team calendar | Yes | Yes | Yes | Own entries may appear; coworker must be active and share viewer's direct manager; pending/approved only; exact field allowlist below |
| View manager queue/request details | No | Yes | No through manager API | Request employee's `manager_id` equals actor's employee ID |
| Approve/reject request | No | Yes | No through manager API | Direct report only; actor employee ID must differ from request employee ID |
| View manager team calendar | No | Yes | No through manager API | Direct reports only |
| Manage employees/reporting | No | No | Yes | Administrator role; creation provisions the employee and password-backed account together |
| Manage leave types/policies/holidays | No | No | Yes | Administrator role |
| Adjust balances | No | No | Yes | Nonblank reason required |
| View organization requests/reports/audit | No | No | Yes | Administrator role |
| Make exceptional correction | No | No | Yes | Nonblank reason and complete audit required |

A user may hold multiple roles. Each call is authorized for the selected endpoint and target resource; holding `MANAGER` never expands employee ownership, and holding `ADMINISTRATOR` does not make the manager API a bypass.

Every user account has at least one unique role, and the only role codes are `EMPLOYEE`, `MANAGER`, and `ADMINISTRATOR`. Employee create/update commands and employee/principal responses use this same closed, non-empty, duplicate-free role collection.

## Authentication behavior

- `GET /api/auth/csrf` is available before login and returns a CSRF token for unsafe requests.
- `POST /api/auth/login` establishes one server-side session and rotates the session ID.
- The browser sends the session cookie automatically and echoes the CSRF token through `X-XSRF-TOKEN` on unsafe requests.
- `POST /api/auth/logout` invalidates the server session and clears the session cookie.
- Missing/expired authentication returns `401 AUTHENTICATION_REQUIRED`; an authenticated but unauthorized call returns `403 ACCESS_DENIED` without revealing the target resource.
- `POST /api/admin/employees` requires `initialPassword` and creates the employee plus associated password-backed account atomically. The password is hashed before persistence and is input-only: it never appears in employee, principal, audit, or other response data. No reset, invitation, SSO, magic-link, or first-login-change flow is part of the MVP.

## Service authorization invariants

1. Employee request/history predicates require `request.employee_id = actor.employee_id`; employee balance predicates require `balance.employee_id = actor.employee_id`. Employee endpoints derive the actor identifier from the principal and never trust a client-supplied employee ID.
2. Employee team-calendar repositories require `request.status IN (PENDING, APPROVED)` and select rows satisfying `(request.employee_id = viewer.employee_id) OR (viewer.manager_id IS NOT NULL AND request.employee.active = true AND request.employee.manager_id = viewer.manager_id)`. The dedicated projection selects only `employee.display_name`, `request.start_date`, `request.end_date`, and `request.status`. It must not load or serialize leave reason/type/balance, duration mode, decision comments, identifiers, status/audit history, or a general request DTO.
3. Manager list, detail, and manager-calendar queries accept the actor employee ID and include `request.employee.manager_id = actor.employee_id` in the repository predicate. Manager detail and decision commands repeat this direct-report check after locking the request.
4. Every manager decision requires the `MANAGER` role and `request.employee_id != actor.employee_id`. Self-approval is never permitted; rejection is also limited to a direct report and cannot use a manager endpoint as a self-service path.
5. There is no delegation or exceptional manager grant entity in the MVP.
6. Every employee/reporting, policy/type, holiday, balance, organization-report, audit-query, or correction service entry requires `ADMINISTRATOR`. An administrator role does not make manager endpoints a bypass; administrator corrections use dedicated actions and never impersonate a manager or rewrite prior audit/history rows.
7. After locking the request, correction authorization validates exactly one action/current-state pair: `CANCEL_PENDING` with `PENDING`, `CANCEL_APPROVED` with `APPROVED`, or `REOPEN_REJECTED` with `REJECTED`. Same-status and every other pair is rejected.
8. Repository scope predicates prevent unscoped loads where practical, and application services repeat authorization before mutation. Authorization failures produce no request, balance, ledger, occupancy, history, or audit mutation.

## Version and stale-write behavior

- Manager approval/rejection, employee cancellation, administrator correction, employee/reporting updates, leave-type updates, policy-version creation against its parent leave type, holiday updates/deactivation, and balance adjustment require `expectedVersion` in the JSON command body.
- Missing `expectedVersion` is `400 VALIDATION_FAILED`. After authorization, a mismatch is `409 STALE_VERSION`; the response does not reveal the current version or protected resource state.
- A stale command performs no request, balance, ledger, occupancy, history, audit, or master-data mutation. A successful update increments the guarded aggregate version.
- Leave submission and creation of a new employee, leave type, holiday, or balance period have no pre-existing target version; they use current server validation, database constraints, locks where applicable, and idempotency.

## Business error codes

| HTTP status | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Request shape or field validation failed |
| 401 | `AUTHENTICATION_REQUIRED` | Session is absent or expired |
| 401 | `INVALID_CREDENTIALS` | Login failed without revealing which credential was wrong |
| 403 | `ACCESS_DENIED` | Role, ownership, or reporting-scope check failed |
| 404 | `RESOURCE_NOT_FOUND` | Resource is absent or deliberately hidden from this scope |
| 409 | `LEAVE_OVERLAP` | Active AM/PM occupancy conflicts |
| 409 | `INSUFFICIENT_BALANCE` | Required units cannot be reserved |
| 409 | `INVALID_STATUS_TRANSITION` | Current request state does not permit command |
| 409 | `STALE_VERSION` | A request or other mutable aggregate changed since the client loaded it; no mutation occurred |
| 409 | `POLICY_CHANGED` | Revalidation changes material eligibility/duration |
| 422 | `CANCELLATION_CUTOFF_PASSED` | Employee self-cancellation is no longer permitted |
| 422 | `NO_CHARGEABLE_DAYS` | Configured rules exclude every requested date |
| 422 | `REJECTION_COMMENT_REQUIRED` | Configured policy requires a comment |

Every problem response includes a correlation identifier. Messages are user-safe and field errors do not expose inaccessible employee/request data.

Each OpenAPI operation documents the problem responses that can arise from its actual validation and authorization path: malformed or validation-invalid input uses `400`, absent authentication uses `401`, authenticated role/scope denial uses `403`, absent or deliberately hidden addressable resources use `404`, and established stale/business conflicts use `409`. Required version commands retain `400 VALIDATION_FAILED` for a missing token and `409 STALE_VERSION` with zero mutation for a mismatch.
