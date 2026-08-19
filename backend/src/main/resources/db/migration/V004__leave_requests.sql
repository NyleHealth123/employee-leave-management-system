CREATE TABLE leave_request (
    id uuid PRIMARY KEY,
    employee_id uuid NOT NULL REFERENCES employee_profile(id),
    leave_type_id uuid NOT NULL REFERENCES leave_type(id),
    submitted_policy_version_id uuid NOT NULL REFERENCES leave_policy_version(id),
    start_date date NOT NULL,
    end_date date NOT NULL,
    duration_mode varchar(24) NOT NULL CHECK (duration_mode IN ('FULL_DAY','HALF_DAY_AM','HALF_DAY_PM')),
    chargeable_units integer NOT NULL CHECK (chargeable_units > 0),
    reason varchar(1000) NOT NULL CHECK (btrim(reason) <> ''),
    status varchar(16) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    submitted_at timestamptz NOT NULL,
    decided_at timestamptz,
    decided_by_user_id uuid REFERENCES user_account(id),
    decision_comment varchar(1000),
    cancelled_at timestamptz,
    cancelled_by_user_id uuid REFERENCES user_account(id),
    policy_snapshot jsonb NOT NULL,
    idempotency_key varchar(100),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_request_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_half_day_single_date CHECK (duration_mode = 'FULL_DAY' OR start_date = end_date),
    UNIQUE(employee_id, idempotency_key),
    UNIQUE(id, employee_id)
);

CREATE TABLE leave_request_slot (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    leave_date date NOT NULL,
    slot varchar(2) NOT NULL CHECK (slot IN ('AM','PM')),
    active boolean NOT NULL DEFAULT true,
    FOREIGN KEY(request_id, employee_id) REFERENCES leave_request(id, employee_id),
    UNIQUE(request_id, leave_date, slot)
);

CREATE UNIQUE INDEX uq_active_leave_slot ON leave_request_slot(employee_id, leave_date, slot) WHERE active;

CREATE TABLE leave_request_balance_line (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES leave_request(id),
    balance_id uuid NOT NULL REFERENCES leave_balance(id),
    units integer NOT NULL CHECK (units > 0),
    state varchar(16) NOT NULL CHECK (state IN ('RESERVED','CONSUMED','RELEASED','RESTORED')),
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    UNIQUE(request_id, balance_id)
);

ALTER TABLE leave_balance_movement ADD CONSTRAINT fk_movement_request FOREIGN KEY(request_id) REFERENCES leave_request(id);
CREATE INDEX ix_request_owner_submitted ON leave_request(employee_id, submitted_at DESC);
CREATE INDEX ix_request_owner_status_start ON leave_request(employee_id, status, start_date);
CREATE INDEX ix_request_status_start ON leave_request(status, start_date);

