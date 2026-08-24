package com.example.leavemanagement.request.domain;

/** A transport-independent failure raised by reusable leave calculation rules. */
public final class LeaveRuleViolation extends RuntimeException {
    private final Reason reason;

    public LeaveRuleViolation(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        VALIDATION_FAILED,
        NO_CHARGEABLE_DAYS
    }
}
