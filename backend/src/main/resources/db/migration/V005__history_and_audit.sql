CREATE TABLE leave_request_status_history (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES leave_request(id),
    from_status varchar(16),
    to_status varchar(16) NOT NULL CHECK (to_status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    actor_user_id uuid NOT NULL REFERENCES user_account(id),
    comment varchar(1000),
    created_at timestamptz NOT NULL
);

CREATE TABLE audit_event (
    id uuid PRIMARY KEY,
    actor_user_id uuid NOT NULL REFERENCES user_account(id),
    action varchar(80) NOT NULL,
    entity_type varchar(80) NOT NULL,
    entity_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL,
    reason varchar(1000),
    before_data jsonb,
    after_data jsonb,
    request_correlation_id varchar(100)
);

CREATE INDEX ix_history_request_time ON leave_request_status_history(request_id, created_at DESC);
CREATE INDEX ix_audit_entity_time ON audit_event(entity_type, entity_id, occurred_at DESC);

CREATE FUNCTION reject_immutable_row_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'immutable history rows cannot be updated or deleted';
END;
$$;

CREATE TRIGGER trg_balance_movement_immutable BEFORE UPDATE OR DELETE ON leave_balance_movement FOR EACH ROW EXECUTE FUNCTION reject_immutable_row_change();
CREATE TRIGGER trg_status_history_immutable BEFORE UPDATE OR DELETE ON leave_request_status_history FOR EACH ROW EXECUTE FUNCTION reject_immutable_row_change();
CREATE TRIGGER trg_audit_event_immutable BEFORE UPDATE OR DELETE ON audit_event FOR EACH ROW EXECUTE FUNCTION reject_immutable_row_change();

