CREATE TABLE leave_balance (
    id uuid PRIMARY KEY,
    employee_id uuid NOT NULL REFERENCES employee_profile(id),
    leave_type_id uuid NOT NULL REFERENCES leave_type(id),
    period_start date NOT NULL,
    period_end date NOT NULL,
    allocated_units integer NOT NULL CHECK (allocated_units >= 0),
    adjustment_units integer NOT NULL DEFAULT 0,
    reserved_units integer NOT NULL DEFAULT 0 CHECK (reserved_units >= 0),
    consumed_units integer NOT NULL DEFAULT 0 CHECK (consumed_units >= 0),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_balance_period CHECK (period_end >= period_start),
    CONSTRAINT ck_balance_available CHECK (allocated_units + adjustment_units - reserved_units - consumed_units >= 0),
    UNIQUE(employee_id, leave_type_id, period_start, period_end),
    EXCLUDE USING gist (employee_id WITH =, leave_type_id WITH =, daterange(period_start, period_end, '[]') WITH &&)
);

CREATE INDEX ix_balance_owner_type_period ON leave_balance(employee_id, leave_type_id, period_start, period_end);

CREATE TABLE leave_balance_movement (
    id uuid PRIMARY KEY,
    balance_id uuid NOT NULL REFERENCES leave_balance(id),
    request_id uuid,
    movement_type varchar(32) NOT NULL CHECK (movement_type IN ('ALLOCATE','RESERVE','CONSUME_RESERVED','RELEASE_RESERVED','RESTORE_CONSUMED','ADMIN_ADJUST')),
    units integer NOT NULL CHECK (units <> 0),
    reason varchar(1000),
    actor_user_id uuid NOT NULL REFERENCES user_account(id),
    created_at timestamptz NOT NULL,
    idempotency_key varchar(100),
    UNIQUE(actor_user_id, idempotency_key)
);

