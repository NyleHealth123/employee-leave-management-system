<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.0.0
- Modified principles: none
- Added sections: none
- Removed sections: none
- Follow-up TODOs: none
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
support. Technology, framework, database, and detailed architecture choices remain planning-stage
decisions unless a specification explicitly requires them.

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

**Version**: 1.0.0 | **Ratified**: 2026-08-19 | **Last Amended**: 2026-08-19
