CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE organization_settings (
    id uuid PRIMARY KEY,
    name varchar(160) NOT NULL CHECK (btrim(name) <> ''),
    time_zone_id varchar(64) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_active_organization ON organization_settings (active) WHERE active;

CREATE TABLE user_account (
    id uuid PRIMARY KEY,
    login varchar(254) NOT NULL,
    normalized_login varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    credentials_updated_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE role (
    code varchar(32) PRIMARY KEY CHECK (code IN ('EMPLOYEE', 'MANAGER', 'ADMINISTRATOR'))
);

INSERT INTO role(code) VALUES ('EMPLOYEE'), ('MANAGER'), ('ADMINISTRATOR');

CREATE TABLE user_account_role (
    account_id uuid NOT NULL REFERENCES user_account(id),
    role_code varchar(32) NOT NULL REFERENCES role(code),
    PRIMARY KEY (account_id, role_code)
);

CREATE TABLE employee_profile (
    id uuid PRIMARY KEY,
    employee_number varchar(64) NOT NULL UNIQUE,
    user_account_id uuid NOT NULL UNIQUE REFERENCES user_account(id),
    display_name varchar(160) NOT NULL CHECK (btrim(display_name) <> ''),
    email varchar(254) NOT NULL,
    manager_id uuid NULL REFERENCES employee_profile(id),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_employee_not_own_manager CHECK (manager_id IS NULL OR manager_id <> id)
);

CREATE UNIQUE INDEX uq_employee_email_normalized ON employee_profile (lower(email));
CREATE INDEX ix_employee_manager ON employee_profile(manager_id);

