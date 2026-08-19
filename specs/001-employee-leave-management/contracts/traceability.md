# Specification-to-Design Traceability

This matrix keeps Phase 1 design tied to the approved specification. Task generation should retain these groupings and add task/test identifiers.

| Specification coverage | Design location | Contract or verification evidence |
|---|---|---|
| FR-001, FR-002, FR-003: authentication, logout, roles, ownership | `plan.md` Authentication and authorization; `data-model.md` UserAccount/Role/EmployeeProfile | `openapi.yaml` Authentication and Employee endpoints; `authorization.md`; quickstart scenario 1 |
| FR-004, FR-005: dashboard, leave types, balances | Frontend design; LeaveType/LeavePolicyVersion/LeaveBalance | Employee dashboard, leave balance, and leave type operations; frontend/API tests |
| FR-006, FR-007, FR-008: request input, calculation, validation | LeaveRequest, LeaveRequestSlot; calculation rules | Calculate/submit operations; quickstart scenario 2 |
| FR-009: active overlap prevention | Transaction strategy; active occupancy slots and partial unique index | `409 LEAVE_OVERLAP`; repository/concurrency tests; quickstart scenario 3 |
| FR-010, FR-011: pending reservation and history | LeaveBalance, LeaveRequestBalanceLine, LeaveBalanceMovement | Submit response and insufficient-balance conflict; scenarios 2–3 |
| FR-012, FR-013: states and cancellation cutoff | State transition table; organization timezone and policy cutoff | Cancel operation and `CANCELLATION_CUTOFF_PASSED`; scenario 5 |
| FR-014, FR-015, FR-016, FR-017, FR-018: manager queue, detail, decisions, scope, calendar | Layered authorization; EmployeeProfile manager relation | Manager endpoints; `authorization.md`; scenario 4 |
| FR-019: employees and manager assignments | People module; EmployeeProfile | Administrator employee operations; scenario 6 |
| FR-020, FR-021, FR-022: configurable leave policies, weekly offs, holidays, allowances | LeaveType, LeavePolicyVersion, PolicyWeeklyOff, CompanyHoliday, LeaveBalance | Administrator leave type/policy/holiday/balance-allocation APIs; scenarios 2 and 6 |
| FR-023: auditable balance adjustment | LeaveBalanceMovement and AuditEvent | Balance adjustment API requiring reason; scenario 6 |
| FR-024, FR-025, FR-026: approval conversion, release/restore, revalidation | Transaction strategy; balance-line state transitions | Approve/reject/cancel contracts; transactional tests; scenarios 4–5 |
| FR-027, FR-028: audit and status trace | AuditEvent, LeaveRequestStatusHistory, append-only constraints | Audit listing API; rollback verification; scenarios 6–7 |
| FR-029, FR-030: organization requests and summaries | Reporting module and request indexes | Administrator request/report operations; scenario 6 |
| FR-031: employee/team calendars and holidays | Calendar module; request slots and holidays | Employee and manager calendar/reference endpoints |
| FR-032: clear safe errors | Shared problem schema and business error code table | API/security/frontend error tests |
| FR-033, FR-034: configurable, consistent policy application | Effective policy model and shared domain calculation service | Preview, submit, approval, cancellation, calendar and report tests |
| FR-035: exceptional administrator correction | Dedicated transactional correction path and compensating movements | Correction endpoint requiring reason; scenario 6 |

## Success criteria verification

| Success criterion | Planned evidence |
|---|---|
| SC-001 | Timed employee usability test using the responsive request flow |
| SC-002 | Parameterized domain and PostgreSQL integration tests for full/half days, holidays, and weekly offs |
| SC-003 | Security/API matrix for every role, ownership case, manager scope, and self-approval |
| SC-004 | Transactional invariants and ledger reconciliation for approved, rejected, and cancelled requests |
| SC-005 | Timed manager usability test for queue-to-decision flow |
| SC-006 | Administrator configuration acceptance scenario showing downstream calculation/scope effects |
| SC-007 | Audit assertion for every required action plus rollback injection tests |
| SC-008 | User evaluation of validation/problem messages and accessible field-error presentation |
