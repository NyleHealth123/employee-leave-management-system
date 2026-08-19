# Authorization Contract

This contract complements `openapi.yaml`. UI route guards are convenience only; Spring Security and application services enforce every rule below.

## Role and scope matrix

| Capability | Employee | Manager | Administrator | Additional scope rule |
|---|---:|---:|---:|---|
| Sign in, sign out, view own principal | Yes | Yes | Yes | Authenticated account must be enabled |
| View own dashboard, balances, requests, history | Yes | Yes | Yes | Always resolved from authenticated employee; client-supplied employee IDs are ignored |
| Preview, submit, cancel own leave | Yes | Yes | Yes | Actor owns request; cancellation status/cutoff applies |
| View basic employee team calendar | Yes | Yes | Yes | Only privacy-safe calendar fields exposed |
| View manager queue/request details | No | Yes | No through manager API | Request employee's `manager_id` equals actor's employee ID |
| Approve/reject request | No | Yes | No through manager API | Direct report only; actor employee ID must differ from request employee ID |
| View manager team calendar | No | Yes | No through manager API | Direct reports only |
| Manage employees/reporting | No | No | Yes | Administrator role |
| Manage leave types/policies/holidays | No | No | Yes | Administrator role |
| Adjust balances | No | No | Yes | Nonblank reason required |
| View organization requests/reports/audit | No | No | Yes | Administrator role |
| Make exceptional correction | No | No | Yes | Nonblank reason and complete audit required |

A user may hold multiple roles. Each call is authorized for the selected endpoint and target resource; holding `MANAGER` never expands employee ownership, and holding `ADMINISTRATOR` does not make the manager API a bypass.

## Authentication behavior

- `GET /api/auth/csrf` is available before login and returns a CSRF token for unsafe requests.
- `POST /api/auth/login` establishes one server-side session and rotates the session ID.
- The browser sends the session cookie automatically and echoes the CSRF token through `X-XSRF-TOKEN` on unsafe requests.
- `POST /api/auth/logout` invalidates the server session and clears the session cookie.
- Missing/expired authentication returns `401 AUTHENTICATION_REQUIRED`; an authenticated but unauthorized call returns `403 ACCESS_DENIED` without revealing the target resource.

## Service authorization invariants

1. Employee resources derive owner identity from the authenticated principal rather than a request parameter.
2. Manager list queries include `employee.manager_id = actor.employee_id` in the repository predicate.
3. Manager detail and decision commands repeat the direct-report check after locking the request.
4. A manager decision also requires `request.employee_id != actor.employee_id`; no role combination or endpoint permits manager self-approval.
5. There is no delegation or exceptional manager grant entity in the MVP.
6. Administrator exceptional corrections use dedicated endpoints and action codes; they do not impersonate a manager or rewrite prior audit/history rows.
7. Authorization failures produce no request, balance, history, or audit mutation.

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
| 409 | `STALE_REQUEST` | Request changed since the client loaded it |
| 409 | `POLICY_CHANGED` | Revalidation changes material eligibility/duration |
| 422 | `CANCELLATION_CUTOFF_PASSED` | Employee self-cancellation is no longer permitted |
| 422 | `NO_CHARGEABLE_DAYS` | Configured rules exclude every requested date |
| 422 | `REJECTION_COMMENT_REQUIRED` | Configured policy requires a comment |

Every problem response includes a correlation identifier. Messages are user-safe and field errors do not expose inaccessible employee/request data.
