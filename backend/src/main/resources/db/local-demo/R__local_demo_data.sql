-- Profile-isolated synthetic data only. This repeatable migration is activated by local-demo.
-- Password values are supplied as Flyway placeholders from DEMO_*_PASSWORD_HASH environment variables.

DO $$
DECLARE
    demo_now timestamptz := TIMESTAMPTZ '2026-01-01 00:00:00+00';
    demo_admin uuid := md5('demo-account-admin')::uuid;
    demo_admin_employee uuid := md5('demo-employee-admin')::uuid;
    annual_type uuid := md5('demo-leave-type-annual')::uuid;
    sick_type uuid := md5('demo-leave-type-sick')::uuid;
    personal_type uuid := md5('demo-leave-type-personal')::uuid;
    annual_policy uuid := md5('demo-policy-annual-v1')::uuid;
    sick_policy uuid := md5('demo-policy-sick-v1')::uuid;
    personal_policy uuid := md5('demo-policy-personal-v1')::uuid;
BEGIN
    -- Remove only deterministic demo rows, including rows created by a previous repeatable run.
    -- The three history tables are immutable in normal application use. Disable their user
    -- triggers only for this profile-isolated, deterministic teardown; a failed migration
    -- rolls the trigger changes back with the rest of the transaction.
    ALTER TABLE audit_event DISABLE TRIGGER trg_audit_event_immutable;
    ALTER TABLE leave_request_status_history DISABLE TRIGGER trg_status_history_immutable;
    ALTER TABLE leave_balance_movement DISABLE TRIGGER trg_balance_movement_immutable;
    DELETE FROM audit_event
     WHERE actor_user_id IN (SELECT id FROM user_account WHERE normalized_login LIKE 'demo.%')
        OR entity_id IN (
            SELECT md5('demo-request-' || n)::uuid FROM generate_series(1, 50) AS s(n)
        );
    DELETE FROM leave_request_status_history AS status_history
     WHERE status_history.request_id IN (SELECT md5('demo-request-' || n)::uuid FROM generate_series(1, 50) AS s(n));
    DELETE FROM leave_balance_movement AS balance_movement
     WHERE balance_movement.actor_user_id IN (SELECT account.id FROM user_account AS account WHERE account.normalized_login LIKE 'demo.%')
        OR balance_movement.request_id IN (SELECT md5('demo-request-' || n)::uuid FROM generate_series(1, 50) AS s(n));
    ALTER TABLE audit_event ENABLE TRIGGER trg_audit_event_immutable;
    ALTER TABLE leave_request_status_history ENABLE TRIGGER trg_status_history_immutable;
    ALTER TABLE leave_balance_movement ENABLE TRIGGER trg_balance_movement_immutable;
    DELETE FROM leave_request_balance_line AS balance_line
     WHERE balance_line.request_id IN (SELECT md5('demo-request-' || n)::uuid FROM generate_series(1, 50) AS s(n));
    DELETE FROM leave_request_slot AS request_slot
     WHERE request_slot.request_id IN (SELECT md5('demo-request-' || n)::uuid FROM generate_series(1, 50) AS s(n));
    DELETE FROM leave_request
     WHERE id IN (SELECT md5('demo-request-' || n)::uuid FROM generate_series(1, 50) AS s(n));
    DELETE FROM leave_balance
     WHERE id IN (
         SELECT md5('demo-balance-annual-' || n)::uuid FROM generate_series(1, 55) AS s(n)
         UNION ALL SELECT md5('demo-balance-sick-' || n)::uuid FROM generate_series(1, 55) AS s(n)
         UNION ALL SELECT md5('demo-balance-personal-' || n)::uuid FROM generate_series(1, 55) AS s(n)
     );
    DELETE FROM policy_weekly_off
     WHERE policy_version_id IN (annual_policy, sick_policy, personal_policy);
    DELETE FROM leave_policy_version
     WHERE id IN (annual_policy, sick_policy, personal_policy);
    DELETE FROM company_holiday
     WHERE id IN (md5('demo-holiday-1')::uuid, md5('demo-holiday-2')::uuid, md5('demo-holiday-3')::uuid);
    DELETE FROM leave_type WHERE id IN (annual_type, sick_type, personal_type);
    DELETE FROM employee_profile
     WHERE id = demo_admin_employee
        OR id IN (SELECT md5('demo-employee-manager-' || n)::uuid FROM generate_series(1, 4) AS s(n))
        OR id IN (SELECT md5('demo-employee-' || n)::uuid FROM generate_series(1, 50) AS s(n));
    DELETE FROM user_account_role WHERE account_id IN (SELECT id FROM user_account WHERE normalized_login LIKE 'demo.%');
    DELETE FROM user_account WHERE normalized_login LIKE 'demo.%';
    DELETE FROM organization_settings WHERE id = md5('demo-organization')::uuid;

    INSERT INTO organization_settings(id, name, time_zone_id, active, version)
    VALUES (md5('demo-organization')::uuid, 'Demo Leave Organization', 'Asia/Kolkata', true, 0);

    INSERT INTO user_account(id, login, normalized_login, password_hash, enabled, credentials_updated_at, created_at, updated_at, version)
    VALUES (demo_admin, 'demo.admin', 'demo.admin', '${demo_admin_password_hash}', true, demo_now, demo_now, demo_now, 0);
    INSERT INTO user_account(id, login, normalized_login, password_hash, enabled, credentials_updated_at, created_at, updated_at, version)
    SELECT md5('demo-account-manager-' || n)::uuid, 'demo.manager' || lpad(n::text, 2, '0'), 'demo.manager' || lpad(n::text, 2, '0'), '${demo_manager_password_hash}', true, demo_now, demo_now, demo_now, 0
      FROM generate_series(1, 4) AS s(n);
    INSERT INTO user_account(id, login, normalized_login, password_hash, enabled, credentials_updated_at, created_at, updated_at, version)
    SELECT md5('demo-account-' || n)::uuid, 'demo.employee' || lpad(n::text, 2, '0'), 'demo.employee' || lpad(n::text, 2, '0'), '${demo_employee_password_hash}', true, demo_now, demo_now, demo_now, 0
      FROM generate_series(1, 50) AS s(n);

    INSERT INTO user_account_role(account_id, role_code) VALUES (demo_admin, 'ADMINISTRATOR');
    INSERT INTO user_account_role(account_id, role_code)
    SELECT md5('demo-account-manager-' || n)::uuid, 'MANAGER' FROM generate_series(1, 4) AS s(n);
    INSERT INTO user_account_role(account_id, role_code)
    SELECT md5('demo-account-' || n)::uuid, 'EMPLOYEE' FROM generate_series(1, 50) AS s(n);

    INSERT INTO employee_profile(id, employee_number, user_account_id, display_name, email, manager_id, active, created_at, updated_at, version)
    VALUES (demo_admin_employee, 'DEMO-ADMIN-001', demo_admin, 'Demo Administrator', 'demo.admin@example.test', null, true, demo_now, demo_now, 0);
    INSERT INTO employee_profile(id, employee_number, user_account_id, display_name, email, manager_id, active, created_at, updated_at, version)
    SELECT md5('demo-employee-manager-' || n)::uuid, 'DEMO-MGR-' || lpad(n::text, 2, '0'), md5('demo-account-manager-' || n)::uuid, 'Demo Manager ' || n, 'demo.manager' || lpad(n::text, 2, '0') || '@example.test', demo_admin_employee, true, demo_now, demo_now, 0
      FROM generate_series(1, 4) AS s(n);
    INSERT INTO employee_profile(id, employee_number, user_account_id, display_name, email, manager_id, active, created_at, updated_at, version)
    SELECT md5('demo-employee-' || n)::uuid, 'DEMO-EMP-' || lpad(n::text, 3, '0'), md5('demo-account-' || n)::uuid, 'Demo Employee ' || lpad(n::text, 2, '0'), 'demo.employee' || lpad(n::text, 2, '0') || '@example.test', md5('demo-employee-manager-' || (((n - 1) % 4) + 1))::uuid, true, demo_now, demo_now, 0
      FROM generate_series(1, 50) AS s(n);

    INSERT INTO leave_type(id, code, name, description, active, version) VALUES
      (annual_type, 'ANNUAL', 'Annual Leave', 'Demo annual leave', true, 0),
      (sick_type, 'SICK', 'Sick Leave', 'Demo sick leave', true, 0),
      (personal_type, 'PERSONAL', 'Personal Leave', 'Demo personal leave', true, 0);
    INSERT INTO leave_policy_version(id, leave_type_id, version_number, effective_from, effective_to, tracks_balance, allows_half_day, weekly_off_treatment, holiday_treatment, rejection_comment_required, cancellation_cutoff_days, created_at)
    VALUES
      (annual_policy, annual_type, 1, DATE '2026-01-01', null, true, true, 'INCLUDE', 'INCLUDE', false, 2, demo_now),
      (sick_policy, sick_type, 1, DATE '2026-01-01', null, true, true, 'EXCLUDE', 'EXCLUDE', true, 0, demo_now),
      (personal_policy, personal_type, 1, DATE '2026-01-01', null, false, true, 'EXCLUDE', 'EXCLUDE', false, 1, demo_now);
    INSERT INTO policy_weekly_off(policy_version_id, iso_day) VALUES (sick_policy, 6), (sick_policy, 7), (personal_policy, 7);
    INSERT INTO company_holiday(id, holiday_date, name, active, version, created_at, updated_at) VALUES
      (md5('demo-holiday-1')::uuid, DATE '2026-08-15', 'Demo Independence Day', true, 0, demo_now, demo_now),
      (md5('demo-holiday-2')::uuid, DATE '2026-10-02', 'Demo Foundation Day', true, 0, demo_now, demo_now),
      (md5('demo-holiday-3')::uuid, DATE '2026-12-25', 'Demo Winter Holiday', true, 0, demo_now, demo_now);

    INSERT INTO leave_balance(id, employee_id, leave_type_id, period_start, period_end, allocated_units, adjustment_units, reserved_units, consumed_units, version, created_at, updated_at)
    SELECT md5('demo-balance-annual-' || n)::uuid, CASE WHEN n = 1 THEN demo_admin_employee WHEN n BETWEEN 2 AND 5 THEN md5('demo-employee-manager-' || (n - 1))::uuid ELSE md5('demo-employee-' || (n - 5))::uuid END, annual_type, DATE '2026-01-01', DATE '2026-12-31', 20, 0,
           CASE WHEN n > 5 AND ((n - 5) % 4) = 1 THEN 2 ELSE 0 END,
           CASE WHEN n > 5 AND ((n - 5) % 4) = 2 THEN 2 ELSE 0 END,
           CASE WHEN n <= 5 THEN 0 WHEN ((n - 5) % 4) = 1 THEN 1 WHEN ((n - 5) % 4) IN (2, 3) THEN 2 ELSE 3 END,
           demo_now,
           CASE WHEN n <= 5 THEN demo_now WHEN ((n - 5) % 4) = 1 THEN demo_now + make_interval(days => n - 5) WHEN ((n - 5) % 4) IN (2, 3) THEN demo_now + make_interval(days => n - 4) ELSE demo_now + make_interval(days => n - 3) END
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO leave_balance(id, employee_id, leave_type_id, period_start, period_end, allocated_units, adjustment_units, reserved_units, consumed_units, version, created_at, updated_at)
    SELECT md5('demo-balance-sick-' || n)::uuid, CASE WHEN n = 1 THEN demo_admin_employee WHEN n BETWEEN 2 AND 5 THEN md5('demo-employee-manager-' || (n - 1))::uuid ELSE md5('demo-employee-' || (n - 5))::uuid END, sick_type, DATE '2026-01-01', DATE '2026-12-31', 10, 0, 0, 0, 0, demo_now, demo_now
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO leave_balance(id, employee_id, leave_type_id, period_start, period_end, allocated_units, adjustment_units, reserved_units, consumed_units, version, created_at, updated_at)
    SELECT md5('demo-balance-personal-' || n)::uuid, CASE WHEN n = 1 THEN demo_admin_employee WHEN n BETWEEN 2 AND 5 THEN md5('demo-employee-manager-' || (n - 1))::uuid ELSE md5('demo-employee-' || (n - 5))::uuid END, personal_type, DATE '2026-01-01', DATE '2026-12-31', 5, 0, 0, 0, 0, demo_now, demo_now
      FROM generate_series(1, 55) AS s(n);

    INSERT INTO leave_request(id, employee_id, leave_type_id, submitted_policy_version_id, start_date, end_date, duration_mode, chargeable_units, reason, status, submitted_at, decided_at, decided_by_user_id, decision_comment, cancelled_at, cancelled_by_user_id, policy_snapshot, idempotency_key, version)
    SELECT md5('demo-request-' || n)::uuid, md5('demo-employee-' || n)::uuid, annual_type, annual_policy, DATE '2026-09-01' + (n - 1), DATE '2026-09-01' + (n - 1), 'FULL_DAY', 2, 'Demo request for workflow verification',
           CASE WHEN n % 4 = 1 THEN 'PENDING' WHEN n % 4 = 2 THEN 'APPROVED' WHEN n % 4 = 3 THEN 'REJECTED' ELSE 'CANCELLED' END,
           demo_now + make_interval(days => n), CASE WHEN n % 4 = 1 THEN null ELSE demo_now + make_interval(days => n + 1) END,
           CASE WHEN n % 4 = 1 THEN null ELSE md5('demo-account-manager-' || (((n - 1) % 4) + 1))::uuid END,
           CASE WHEN n % 4 = 3 THEN 'Demo rejection comment' WHEN n % 4 = 0 THEN 'Demo cancellation' ELSE null END,
           CASE WHEN n % 4 = 0 THEN demo_now + make_interval(days => n + 2) ELSE null END,
           CASE WHEN n % 4 = 0 THEN md5('demo-account-' || n)::uuid ELSE null END,
           jsonb_build_object('policyVersionId', annual_policy::text, 'tracksBalance', true, 'allowsHalfDay', true, 'weeklyOffTreatment', 'INCLUDE', 'holidayTreatment', 'INCLUDE', 'weeklyOffDays', jsonb_build_array(), 'rejectionCommentRequired', false, 'cancellationCutoffDays', 2),
           'demo-request-key-' || n,
           CASE WHEN n % 4 = 1 THEN 0 WHEN n % 4 IN (2, 3) THEN 1 ELSE 2 END
      FROM generate_series(1, 50) AS s(n);

    INSERT INTO leave_request_slot(id, request_id, employee_id, leave_date, slot, active)
    SELECT md5('demo-slot-' || n || '-' || slot)::uuid, md5('demo-request-' || n)::uuid, md5('demo-employee-' || n)::uuid, DATE '2026-09-01' + (n - 1), slot,
           (n % 4) IN (1, 2)
      FROM generate_series(1, 50) AS s(n), unnest(ARRAY['AM', 'PM']) AS u(slot);
    INSERT INTO leave_request_balance_line(id, request_id, balance_id, units, state, updated_at, version)
    SELECT md5('demo-line-' || n)::uuid, md5('demo-request-' || n)::uuid, md5('demo-balance-annual-' || (n + 5))::uuid, 2,
           CASE WHEN n % 4 = 1 THEN 'RESERVED' WHEN n % 4 = 2 THEN 'CONSUMED' WHEN n % 4 = 3 THEN 'RELEASED' ELSE 'RESTORED' END,
           CASE WHEN n % 4 = 1 THEN demo_now + make_interval(days => n) WHEN n % 4 IN (2, 3) THEN demo_now + make_interval(days => n + 1) ELSE demo_now + make_interval(days => n + 2) END,
           CASE WHEN n % 4 = 1 THEN 0 WHEN n % 4 IN (2, 3) THEN 1 ELSE 2 END
      FROM generate_series(1, 50) AS s(n);

    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-allocation-' || n)::uuid, md5('demo-balance-annual-' || n)::uuid, null, 'ALLOCATE', 20, 'Demo allocation', demo_admin, demo_now, 'demo-allocation-' || n
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-sick-allocation-' || n)::uuid, md5('demo-balance-sick-' || n)::uuid, null, 'ALLOCATE', 10, 'Demo allocation', demo_admin, demo_now, 'demo-sick-allocation-' || n
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-personal-allocation-' || n)::uuid, md5('demo-balance-personal-' || n)::uuid, null, 'ALLOCATE', 5, 'Demo allocation', demo_admin, demo_now, 'demo-personal-allocation-' || n
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-reserve-' || n)::uuid, md5('demo-balance-annual-' || (n + 5))::uuid, md5('demo-request-' || n)::uuid, 'RESERVE', 2, null, md5('demo-account-' || n)::uuid, demo_now + make_interval(days => n), null
      FROM generate_series(1, 50) AS s(n);
    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-decision-' || n)::uuid, md5('demo-balance-annual-' || (n + 5))::uuid, md5('demo-request-' || n)::uuid, 'CONSUME_RESERVED', 2, null, md5('demo-account-manager-' || (((n - 1) % 4) + 1))::uuid, demo_now + make_interval(days => n + 1), null
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 IN (0, 2);
    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-release-' || n)::uuid, md5('demo-balance-annual-' || (n + 5))::uuid, md5('demo-request-' || n)::uuid, 'RELEASE_RESERVED', -2, null, md5('demo-account-manager-' || (((n - 1) % 4) + 1))::uuid, demo_now + make_interval(days => n + 1), null
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 = 3;
    INSERT INTO leave_balance_movement(id, balance_id, request_id, movement_type, units, reason, actor_user_id, created_at, idempotency_key)
    SELECT md5('demo-restore-' || n)::uuid, md5('demo-balance-annual-' || (n + 5))::uuid, md5('demo-request-' || n)::uuid, 'RESTORE_CONSUMED', -2, 'Demo cancellation', md5('demo-account-' || n)::uuid, demo_now + make_interval(days => n + 2), null
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 = 0;

    INSERT INTO leave_request_status_history(id, request_id, from_status, to_status, actor_user_id, comment, created_at)
    SELECT md5('demo-history-submit-' || n)::uuid, md5('demo-request-' || n)::uuid, null, 'PENDING', md5('demo-account-' || n)::uuid, null, demo_now + make_interval(days => n)
      FROM generate_series(1, 50) AS s(n);
    INSERT INTO leave_request_status_history(id, request_id, from_status, to_status, actor_user_id, comment, created_at)
    SELECT md5('demo-history-decision-' || n)::uuid, md5('demo-request-' || n)::uuid, 'PENDING', CASE WHEN n % 4 IN (0, 2) THEN 'APPROVED' ELSE 'REJECTED' END, md5('demo-account-manager-' || (((n - 1) % 4) + 1))::uuid, CASE WHEN n % 4 = 3 THEN 'Demo rejection comment' ELSE null END, demo_now + make_interval(days => n + 1)
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 IN (0, 2, 3);
    INSERT INTO leave_request_status_history(id, request_id, from_status, to_status, actor_user_id, comment, created_at)
    SELECT md5('demo-history-cancel-' || n)::uuid, md5('demo-request-' || n)::uuid, 'APPROVED', 'CANCELLED', md5('demo-account-' || n)::uuid, 'Demo cancellation', demo_now + make_interval(days => n + 2)
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 = 0;

    INSERT INTO audit_event(id, actor_user_id, action, entity_type, entity_id, occurred_at, reason, before_data, after_data, request_correlation_id)
    SELECT md5('demo-audit-allocation-annual-' || n)::uuid, demo_admin, 'BALANCE_ALLOCATED', 'LEAVE_BALANCE', md5('demo-balance-annual-' || n)::uuid, demo_now, 'Demo allocation', null, jsonb_build_object('allocatedUnits', 20), 'demo-allocation-annual-' || n
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO audit_event(id, actor_user_id, action, entity_type, entity_id, occurred_at, reason, before_data, after_data, request_correlation_id)
    SELECT md5('demo-audit-allocation-sick-' || n)::uuid, demo_admin, 'BALANCE_ALLOCATED', 'LEAVE_BALANCE', md5('demo-balance-sick-' || n)::uuid, demo_now, 'Demo allocation', null, jsonb_build_object('allocatedUnits', 10), 'demo-allocation-sick-' || n
      FROM generate_series(1, 55) AS s(n);
    INSERT INTO audit_event(id, actor_user_id, action, entity_type, entity_id, occurred_at, reason, before_data, after_data, request_correlation_id)
    SELECT md5('demo-audit-allocation-personal-' || n)::uuid, demo_admin, 'BALANCE_ALLOCATED', 'LEAVE_BALANCE', md5('demo-balance-personal-' || n)::uuid, demo_now, 'Demo allocation', null, jsonb_build_object('allocatedUnits', 5), 'demo-allocation-personal-' || n
      FROM generate_series(1, 55) AS s(n);

    INSERT INTO audit_event(id, actor_user_id, action, entity_type, entity_id, occurred_at, reason, before_data, after_data, request_correlation_id)
    SELECT md5('demo-audit-submit-' || n)::uuid, md5('demo-account-' || n)::uuid, 'LEAVE_SUBMITTED', 'LEAVE_REQUEST', md5('demo-request-' || n)::uuid, demo_now + make_interval(days => n), null, null, jsonb_build_object('status', 'PENDING'), 'demo-' || n
      FROM generate_series(1, 50) AS s(n);
    INSERT INTO audit_event(id, actor_user_id, action, entity_type, entity_id, occurred_at, reason, before_data, after_data, request_correlation_id)
    SELECT md5('demo-audit-decision-' || n)::uuid, md5('demo-account-manager-' || (((n - 1) % 4) + 1))::uuid, CASE WHEN n % 4 IN (0, 2) THEN 'LEAVE_APPROVED' ELSE 'LEAVE_REJECTED' END, 'LEAVE_REQUEST', md5('demo-request-' || n)::uuid, demo_now + make_interval(days => n + 1), CASE WHEN n % 4 = 3 THEN 'Demo rejection comment' ELSE null END, jsonb_build_object('status', 'PENDING'), jsonb_build_object('status', CASE WHEN n % 4 IN (0, 2) THEN 'APPROVED' ELSE 'REJECTED' END), 'demo-' || n
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 IN (0, 2, 3);
    INSERT INTO audit_event(id, actor_user_id, action, entity_type, entity_id, occurred_at, reason, before_data, after_data, request_correlation_id)
    SELECT md5('demo-audit-cancel-' || n)::uuid, md5('demo-account-' || n)::uuid, 'LEAVE_CANCELLED', 'LEAVE_REQUEST', md5('demo-request-' || n)::uuid, demo_now + make_interval(days => n + 2), 'Demo cancellation', jsonb_build_object('status', 'APPROVED'), jsonb_build_object('status', 'CANCELLED'), 'demo-' || n
      FROM generate_series(1, 50) AS s(n) WHERE n % 4 = 0;
END $$;
