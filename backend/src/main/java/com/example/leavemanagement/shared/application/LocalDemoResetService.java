package com.example.leavemanagement.shared.application;

import java.util.Map;

/** Profile-isolated application port used only to recreate the authoritative local-demo fixture. */
public interface LocalDemoResetService {
    ResetResult reset();

    record ResetResult(
            int employees,
            int administrators,
            int managers,
            int employeeAccounts,
            int managerRelationships,
            int leaveTypes,
            int policies,
            int weeklyOffs,
            int holidays,
            int balances,
            int requests,
            int auditEvents,
            Map<String, Integer> requestStatuses) {
    }
}
