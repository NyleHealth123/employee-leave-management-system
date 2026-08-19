# Feature Specification: Employee Leave Management MVP

**Feature Branch**: `001-employee-leave-management`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Build an Employee Leave Management System MVP with Employee, Manager, and Administrator roles."

## Clarifications

### Session 2026-08-19

- Q: When a balance-validated request is pending, should it reserve leave balance immediately, or should balance be checked only at approval? → A: Reserve the required balance on submission; release it when the request is rejected or cancelled, and convert it to final consumption when approved.
- Q: What cancellation policy must the MVP support for approved leave? → A: An employee may cancel eligible approved future leave until the configured cutoff before its start date; restore any reserved or consumed balance, and let administrators make auditable exceptional corrections.
- Q: What qualifies as explicit authorization allowing a manager to view or decide requests outside their normal reporting scope, including self-approval? → A: The MVP supports no delegated or exceptional manager authority. Managers may act only for normal direct reports and may never approve their own leave; administrators handle exceptions.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Submit and track a leave request (Priority: P1)

An employee signs in, reviews available leave balances, submits a valid leave request, and follows its status through the decision process. This lets an employee request time away without manual coordination.

**Why this priority**: Requesting leave and knowing its outcome is the central employee value of the MVP.

**Independent Test**: An employee with an available balance can submit a non-overlapping request and see its calculated duration and `Pending` status in request history.

**Acceptance Scenarios**:

1. **Given** an authenticated employee with an eligible leave type and sufficient balance, **When** they enter valid start and end dates, a permitted duration option, and a reason, **Then** the system shows the chargeable leave days before submission.
2. **Given** an employee reviews the calculated duration for a balance-validated leave type, **When** they submit a request with sufficient unreserved balance, **Then** the request is recorded as `Pending`, its required balance is reserved, it appears in history, and its creation is auditable.
3. **Given** an employee has a request that conflicts with an active request for the same dates, **When** they attempt submission, **Then** the system blocks it and explains the conflicting request or dates.
4. **Given** an employee opens their dashboard, **When** current leave data is available, **Then** they can see leave balances, pending requests, approved upcoming leave, and upcoming company holidays.

---

### User Story 2 - Decide a direct report's leave request (Priority: P1)

A manager reviews requests from employees who report to them, examines the relevant details, and approves or rejects a pending request with the required comment policy applied.

**Why this priority**: Timely manager decisions make submitted leave requests actionable and protect team coverage.

**Independent Test**: A manager can open a pending direct report request, approve or reject it, and the employee can see the resulting status and decision comment.

**Acceptance Scenarios**:

1. **Given** a manager has a pending request from a direct report, **When** they open it, **Then** they can see the employee, leave type, dates, duration, reason, and relevant leave balance.
2. **Given** a manager opens a pending request from a direct report, **When** they approve it, **Then** its status becomes `Approved`, the request's reserved balance becomes final consumption, and the decision is auditable.
3. **Given** a manager rejects a pending request, **When** a rejection comment is required by policy and none is supplied, **Then** the system prevents rejection and explains what is required.
4. **Given** a manager tries to decide a request from an employee outside their normal reporting scope or their own request, **When** they attempt the action, **Then** the system denies the action and does not change the request.
5. **Given** a manager views the team leave calendar, **When** approved or pending team requests exist, **Then** the calendar displays those requests with their status.

---

### User Story 3 - Administer leave policy and employee records (Priority: P1)

An administrator manages employee reporting relationships, leave types, leave balances, and company holidays so leave workflows operate under organization-configured rules.

**Why this priority**: The request workflow depends on accurate people, rules, balances, and holiday data.

**Independent Test**: An administrator can configure a leave type, assign a manager and balance to an employee, add a holiday, and verify those settings affect a subsequent leave request calculation or eligibility check.

**Acceptance Scenarios**:

1. **Given** an administrator manages an employee, **When** they assign or change the employee's manager, **Then** the reporting relationship is saved and determines that manager's request scope.
2. **Given** an administrator creates or changes a leave type, **When** they configure its allowance, balance-validation rule, half-day eligibility, and chargeable-day rules, **Then** new requests use those configured rules.
3. **Given** an administrator adjusts an employee's leave balance, **When** they provide an adjustment amount and reason, **Then** the balance changes, the reason is retained, and the adjustment is auditable.
4. **Given** an administrator adds or changes a company holiday, **When** an employee calculates a request spanning that date, **Then** the calculation treats the date according to the configured leave rules.

---

### User Story 4 - Cancel an eligible leave request (Priority: P2)

An employee cancels a request that remains eligible under the configured cancellation policy, and the system keeps balances and history consistent.

**Why this priority**: Employees need to correct changed plans while preserving reliable balances and decision history.

**Independent Test**: An employee cancels an eligible request and can see its `Cancelled` status, cancellation history, and correct resulting balance.

**Acceptance Scenarios**:

1. **Given** an employee owns a `Pending` request or approved future leave before its leave type's configured cancellation cutoff, **When** they cancel it, **Then** its status becomes `Cancelled`, its reservation or consumed balance is released or restored, and the change is auditable.
2. **Given** an employee owns approved future leave at or after the configured cancellation cutoff, **When** they try to cancel it, **Then** the system blocks self-cancellation and clearly explains the cutoff restriction.

---

### User Story 5 - Review organization leave activity (Priority: P2)

An administrator views leave requests throughout the organization and basic summaries to monitor workload, outstanding approvals, and leave use.

**Why this priority**: Organization-wide visibility supports administrative oversight without expanding the MVP into a broader HRMS.

**Independent Test**: An administrator can view organization-wide requests and a summary segmented by leave status and leave type for a selected reporting period.

**Acceptance Scenarios**:

1. **Given** leave requests exist across the organization, **When** an administrator views all leave requests, **Then** they can identify each request's employee, leave type, dates, duration, and status.
2. **Given** an administrator selects a reporting period, **When** they view basic leave reports, **Then** they can see totals by request status and leave type for that period.

### Edge Cases

- A request with an end date before its start date, a missing required field, or a date that cannot support the selected duration option is rejected with a clear error.
- A request that includes weekends or company holidays calculates only the days chargeable under the leave type's configured rules; it is blocked if no chargeable leave days remain.
- A new or changed request is checked against active requests for the same employee, including requests that overlap partially, fully contain another request, or are contained by another request.
- Simultaneous submissions for the same balance-validated leave type can succeed only where each successful request has sufficient unreserved balance; later submissions that cannot reserve the required amount are rejected with a clear insufficient-balance message.
- A leave type that does not permit half-days cannot be submitted as a half-day; a half-day request must identify the applicable date and permitted half-day period when the configured rules require one.
- A request that becomes invalid because its balance, leave type, holiday, or reporting relationship changed before a decision is revalidated before approval and receives a clear outcome.
- An employee cannot view or change another employee's private balance or request details except where their authorized manager or administrator role permits it.
- A manager who is also an employee follows the employee request rules for their own leave and can never approve their own request; an administrator handles the exceptional case.
- Status changes from terminal states are blocked except for policy-defined administrative correction, and every permitted correction remains traceable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST authenticate users before granting access to protected leave-management information or actions and MUST provide a logout action that ends the active session.
- **FR-002**: The system MUST assign each user an Employee, Manager, Administrator, or applicable combination of roles, and authorize every protected action by role and scope.
- **FR-003**: The system MUST allow an employee to view only their own leave balances, requests, dashboard information, and personal leave history unless an additional authorized role applies.
- **FR-004**: The employee dashboard MUST show current leave balances, pending requests, approved future leave, and upcoming company holidays.
- **FR-005**: The system MUST show employees each available leave type, its remaining balance, and its applicable request options.
- **FR-006**: The system MUST allow employees to submit a leave request with leave type, start date, end date, applicable full-day or half-day option, and reason.
- **FR-007**: Before submission, the system MUST calculate and display chargeable leave days using the configured leave rules, company holidays, and configured weekly offs.
- **FR-008**: The system MUST reject a leave request whose start date is after its end date, whose required fields are absent, or whose resulting duration is invalid under the selected leave type's rules.
- **FR-009**: The system MUST prevent an employee from creating or changing a leave request that overlaps an active request for the same employee and must identify the conflict clearly.
- **FR-010**: For a balance-validated leave request, the system MUST validate sufficient unreserved balance and reserve the required chargeable days when the request is submitted; it MUST reject a submission that cannot create the reservation and clearly state that insufficient unreserved balance prevented it.
- **FR-011**: The system MUST record a successfully submitted request as `Pending`, retain any associated balance reservation, and allow the employee to view its status, dates, leave type, duration, reason, and decision comment where present.
- **FR-012**: The system MUST support the request statuses `Pending`, `Approved`, `Rejected`, and `Cancelled`, and allow only policy-authorized transitions between them.
- **FR-013**: The system MUST let an employee cancel only their own `Pending` request before a decision or their own approved future request before the leave type's configured cancellation cutoff; it MUST block self-cancellation at or after that cutoff with a clear message, release or restore the request's reserved or consumed balance when cancellation succeeds, and record the cancellation in request history.
- **FR-014**: The system MUST let a manager view pending requests from employees within their reporting scope and access their request details.
- **FR-015**: The system MUST restrict managers to viewing and deciding requests only for employees in their normal reporting scope; the MVP MUST NOT provide delegated or exceptional manager authority.
- **FR-016**: The system MUST let a manager approve or reject only a pending request for a direct report and MUST never allow a manager to approve their own request; it MUST record the acting user, timestamp, resulting status, and any decision comment.
- **FR-017**: The system MUST enforce the configured rule for rejection comments, including whether a comment is mandatory or optional.
- **FR-018**: The system MUST provide managers a team leave calendar displaying approved and pending requests for employees in their authorized scope.
- **FR-019**: The system MUST let an administrator create, update, and deactivate employee records and assign or change each employee's manager.
- **FR-020**: The system MUST let an administrator manage leave types and configure, for each type, allowance or balance basis, whether balance validation is required, half-day eligibility, weekly-off treatment, company-holiday treatment, and the cancellation cutoff before an approved leave start date.
- **FR-021**: The system MUST let an administrator configure employee leave allowances or starting balances by leave type.
- **FR-022**: The system MUST let an administrator create, update, and remove company holidays, with holiday dates available to leave-day calculations.
- **FR-023**: The system MUST let an administrator view and adjust an employee's leave balance, requiring an adjustment reason and retaining the adjustment amount, reason, actor, and timestamp.
- **FR-024**: When a balance-validated request is approved, the system MUST convert its existing balance reservation into final leave consumption without deducting the balance a second time.
- **FR-025**: When a balance-validated pending request is rejected or cancelled, the system MUST release its reservation; when an approved request is cancelled before the configured cutoff, the system MUST restore its consumed balance exactly once.
- **FR-026**: The system MUST revalidate request eligibility, date conflicts, duration, and required balance before an approval changes a request to `Approved`.
- **FR-027**: The system MUST retain an audit history for applying, approving, rejecting, cancelling, balance adjustments, policy-defined administrative corrections, and all leave-request status changes.
- **FR-028**: The audit history for a leave request MUST identify the action, acting user, timestamp, prior and resulting status where applicable, and associated reason or comment where supplied.
- **FR-029**: The system MUST let an administrator view leave requests across the organization, including employee, leave type, dates, duration, and status.
- **FR-030**: The system MUST provide administrators basic leave summaries for a selected reporting period, including totals by leave status and leave type.
- **FR-031**: The system MUST display company holidays to employees and provide employees a basic team leave calendar containing only information they are authorized to see.
- **FR-032**: The system MUST present clear, actionable validation and authorization error messages without revealing information the user is not authorized to access.
- **FR-033**: The system MUST keep organization-specific leave rules configurable and MUST NOT rely on fixed leave counts, fixed weekly-off days, or fixed cancellation rules.
- **FR-034**: The system MUST apply the same configured calculation and balance rules consistently wherever it previews, submits, approves, cancels, reports, or displays a leave request.
- **FR-035**: The system MUST let an administrator make an exceptional correction to a leave request or its balance after normal self-service or manager action is unavailable, requiring a reason and retaining a complete audit event; this authority MUST NOT grant managers any exceptional scope or self-approval ability.

### Key Entities *(include if feature involves data)*

- **User Account**: An authenticated identity with one or more roles and an active/inactive status.
- **Employee Profile**: An employee's organization identity, employment status, manager assignment, and relationship to a user account.
- **Reporting Relationship**: The assignment that defines a manager's normal direct-report scope.
- **Leave Type**: A configurable category of leave with allowance, balance, duration, holiday, weekly-off, validation, and cancellation rules.
- **Leave Balance**: An employee's available, reserved, and consumed quantity for a leave type, including its source and adjustment history.
- **Leave Request**: An employee's requested time away, including dates, duration option, calculated chargeable days, reason, status, any balance reservation or consumption, decision data, and applicable rule context.
- **Company Holiday**: A named organization holiday date used in calendars and leave-day calculations.
- **Leave Policy Configuration**: The organization-managed rules governing leave eligibility, calculation, balance validation and reservation, rejection comments, cancellation cutoffs, and status transitions.
- **Audit Event**: An immutable trace of a material action, actor, time, relevant prior and new values, and reason or comment where applicable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In usability testing, at least 90% of employees can submit a valid leave request and confirm its calculated duration in under 3 minutes without assistance.
- **SC-002**: In acceptance testing, 100% of valid test requests calculate the expected chargeable days across configured weekly offs, holidays, full days, and permitted half days.
- **SC-003**: In authorization testing, 100% of attempted employee, manager, and administrator actions outside the actor's permitted role or reporting scope are denied without changing protected data.
- **SC-004**: In acceptance testing, 100% of approved, rejected, and cancelled test requests leave balances consistent with the configured policy and their recorded audit events.
- **SC-005**: At least 95% of manager test participants can find and decide a pending direct-report request in under 2 minutes after signing in.
- **SC-006**: In acceptance testing, administrators can configure a leave type, employee balance, manager relationship, and company holiday and observe each setting affect the relevant workflow without changing application behavior manually.
- **SC-007**: In acceptance testing, every material action listed in FR-027 produces an audit entry containing the fields required by FR-028.
- **SC-008**: At least 90% of tested users rate validation and error messages as clear enough to identify the action needed to proceed.

## Assumptions

- The MVP serves one organization and does not require multi-company or multi-tenant behavior.
- The organization supplies employee records, initial role assignments, reporting relationships, leave policies, balances, weekly offs, and holidays before routine leave use begins.
- Leave rules are configured by an administrator and may differ by leave type; the specification intentionally does not prescribe organization-specific counts, carry-forward, accrual, or encashment policies.
- A balance-validated request reserves the calculated chargeable days at successful submission; the reservation is final consumption only after approval and is released or restored as stated in FR-024 and FR-025.
- Administrator corrections are exceptional, auditable actions; they do not create delegated manager authority or permit manager self-approval.
- A basic calendar communicates leave visibility and status; it does not provide attendance tracking, shift planning, or staffing optimization.
- Basic reports are limited to organization leave-request totals and summaries; exporting, payroll calculations, attendance reconciliation, and advanced analytics are out of scope.
- Payroll, attendance tracking, recruitment, performance management, expense management, and other HRMS modules are explicitly out of scope.
- Secure sign-in and session handling are required, but the specific identity method is a planning-stage decision.
