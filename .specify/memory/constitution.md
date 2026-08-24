<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.1.0
- Modified principles: V. Simplicity and Deliberate Change (clarified the scope of
  planning-stage architecture decisions)
- Added principles/sections: VI. Six-Layer Architecture and Dependency Boundaries
- Removed sections: none
- Follow-up TODOs:
  - TODO(PLAN_SYNC): Reconcile plan.md's Constitution Check and architecture narrative with
    Principle VI and the six named layers.
  - TODO(TASKS_SYNC): Reconcile tasks.md with any approved work needed to verify the expanded
    layer boundaries; do not create naming-only production refactors.
  - spec.md requires no synchronization because this amendment governs implementation
    architecture without changing feature requirements or acceptance criteria.
-->
# Employee Leave Management System Constitution

## Core Principles

### I. Spec Kit Artifacts Are Authoritative

The approved Spec Kit specification, clarification record, plan, and task list are the source of
truth for project work. Generated implementation MUST remain consistent with those artifacts, and
documented requirements MUST NOT be removed or silently changed during implementation. When
requirements are missing or ambiguous, contributors MUST inspect the repository and existing
artifacts first, then obtain clarification rather than guessing or hardcoding project-specific
behavior. This preserves a reliable, auditable basis for delivery.

### II. Traceable, Task-Gated Delivery

No application feature MAY be implemented until it has been specified, clarified where necessary,
planned, and converted into actionable tasks. Every implementation change MUST trace to an
approved specification and task. A feature is complete only after it runs successfully and meets
its documented acceptance criteria. This prevents scope drift and makes delivery status
verifiable.

### III. Domain Integrity and Role Separation

The system MUST clearly separate employee, manager, and administrator responsibilities in its
requirements, workflows, and authorization rules. Business rules for leave balances, leave types,
holidays, overlapping leave, approvals, cancellation, and authorization MUST be specified and
enforced consistently across every applicable interface and workflow. This protects leave records
and ensures equivalent requests receive equivalent outcomes.

### IV. Security, Quality, and Verified Workflows

Protected functionality MUST enforce security and role-based access control. Critical business
workflows MUST have automated tests that cover their specified rules and acceptance criteria.
Before completion, contributors MUST run the relevant verification and record or address failures.
Security and workflow correctness are release requirements, not optional follow-up work.

### V. Simplicity and Deliberate Change

Architecture and implementation MUST be simple, maintainable, modular, and appropriate to an
employee leave management application. Contributors MUST prefer simple solutions over premature
optimization or unnecessary complexity. Database and schema changes MUST be deliberate,
documented in the relevant planning and task artifacts, and verified with the feature they
support. Technology, framework, database, and architecture choices not governed by this
constitution remain planning-stage decisions unless a specification explicitly requires them.

### VI. Six-Layer Architecture and Dependency Boundaries

The backend MUST preserve the modular-monolith design and use the following six architectural
layers across its feature modules and shared code. These layer names describe responsibilities;
they do not require every module to contain every package or require existing packages to be
renamed.

1. **API/Presentation Layer**: Controllers, HTTP request/response DTOs, transport validation, and
   API error mapping receive and present REST interactions. This layer MUST delegate use cases to
   the Application Layer and MUST NOT directly access repositories, persistence entities, or
   database APIs.
2. **Application Layer**: Application services coordinate use cases, authorization context,
   domain operations, persistence operations, and atomic outcomes. Application-service commands
   MUST own transaction boundaries for multi-step business workflows.
3. **Domain Layer**: Domain types and services express reusable leave-management rules,
   calculations, invariants, and state behavior. Domain rules MUST remain independent of HTTP,
   controller, request/response DTO, and other transport concerns and MUST NOT depend on
   persistence or framework implementation details.
4. **Persistence Layer**: Repository interfaces, persistence entities, query projections, and
   database-access implementations map and store normalized state. This layer MUST encapsulate
   JPA/database access, locking, and persistence-specific queries; database constraints and
   migrations remain final integrity guards where the approved design assigns that role.
5. **Infrastructure Layer**: Framework wiring, runtime configuration, clocks, database and
   migration configuration, and external integration adapters provide technical capabilities to
   the other layers. Infrastructure MUST implement approved technical concerns without becoming
   an alternate location for business rules or use-case orchestration.
6. **Security/Cross-Cutting Layer**: Authentication, coarse endpoint authorization, current-actor
   context, CSRF/session controls, correlation, shared failure handling, and similar system-wide
   policies apply consistently across modules. These concerns MUST NOT bypass application-level
   scope checks, authorization rules, domain invariants, or transactional workflows.

Dependencies MUST be deliberate, reviewable, and free of circular dependencies. Cross-module
calls MUST continue through application/domain interfaces rather than controller or repository
shortcuts. New implementation MUST preserve these responsibilities and boundaries. Existing code
MUST NOT be refactored merely to align package or layer naming; a refactor requires a real boundary
violation identified through an approved specification, plan, or task and verified in proportion
to its risk.

## Domain and Data Constraints

Leave-related behavior MUST be defined in terms of explicit, approved business rules before code
is changed. Specifications and plans MUST identify affected roles, authorization boundaries,
workflow states, validation rules, and data changes. Schema migrations or other persistent-data
changes MUST describe their purpose, impact, rollout considerations, and verification approach.

## Spec-Driven Delivery Workflow

Work MUST proceed through Spec Kit in this order: specify, clarify when needed, plan, generate
tasks, implement, and verify. Implementation review MUST confirm traceability to the approved
artifacts, domain-rule consistency, security and role enforcement, automated-test coverage for
critical workflows, and acceptance-criteria results. Git commits MUST be meaningful checkpoints
that describe cohesive, verified progress; commits do not substitute for required Spec Kit
artifacts or validation.

## Governance

This constitution supersedes conflicting development practices for this project. Amendments MUST
be documented in this file with a Sync Impact Report, reviewed for their effects on active
specifications, plans, tasks, and implementation, and approved before dependent work proceeds.
Versioning follows semantic intent: MAJOR for incompatible removals or redefinitions of governance,
MINOR for added principles or materially expanded guidance, and PATCH for non-semantic
clarifications or corrections. Each feature review MUST assess compliance with this constitution;
exceptions require an explicit, documented approval and a follow-up plan where applicable.

**Version**: 1.1.0 | **Ratified**: 2026-08-19 | **Last Amended**: 2026-08-24
