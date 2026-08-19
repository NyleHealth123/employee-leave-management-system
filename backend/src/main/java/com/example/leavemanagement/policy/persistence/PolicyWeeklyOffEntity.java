package com.example.leavemanagement.policy.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class PolicyWeeklyOffEntity implements Serializable {
    private UUID policyVersionId;
    private Integer isoDay;
    public PolicyWeeklyOffEntity() {}
    @Override public boolean equals(Object value){return value instanceof PolicyWeeklyOffEntity other && Objects.equals(policyVersionId,other.policyVersionId)&&Objects.equals(isoDay,other.isoDay);}
    @Override public int hashCode(){return Objects.hash(policyVersionId,isoDay);}
}

