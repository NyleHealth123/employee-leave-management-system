CREATE TABLE leave_type (
    id uuid PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    name varchar(120) NOT NULL CHECK (btrim(name) <> ''),
    description varchar(500),
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE leave_policy_version (
    id uuid PRIMARY KEY,
    leave_type_id uuid NOT NULL REFERENCES leave_type(id),
    version_number integer NOT NULL CHECK (version_number > 0),
    effective_from date NOT NULL,
    effective_to date,
    tracks_balance boolean NOT NULL,
    allows_half_day boolean NOT NULL,
    weekly_off_treatment varchar(16) NOT NULL CHECK (weekly_off_treatment IN ('EXCLUDE', 'INCLUDE')),
    holiday_treatment varchar(16) NOT NULL CHECK (holiday_treatment IN ('EXCLUDE', 'INCLUDE')),
    rejection_comment_required boolean NOT NULL,
    cancellation_cutoff_days integer NOT NULL CHECK (cancellation_cutoff_days >= 0),
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_policy_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    UNIQUE(leave_type_id, version_number),
    EXCLUDE USING gist (leave_type_id WITH =, daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]') WITH &&)
);

CREATE TABLE policy_weekly_off (
    policy_version_id uuid NOT NULL REFERENCES leave_policy_version(id),
    iso_day integer NOT NULL CHECK (iso_day BETWEEN 1 AND 7),
    PRIMARY KEY(policy_version_id, iso_day)
);

CREATE TABLE company_holiday (
    id uuid PRIMARY KEY,
    holiday_date date NOT NULL UNIQUE,
    name varchar(160) NOT NULL CHECK (btrim(name) <> ''),
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX ix_holiday_active_date ON company_holiday(holiday_date) WHERE active;
CREATE INDEX ix_policy_effective ON leave_policy_version(leave_type_id, effective_from, effective_to);

