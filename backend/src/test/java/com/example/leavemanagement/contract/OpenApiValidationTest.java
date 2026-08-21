package com.example.leavemanagement.contract;

import com.example.leavemanagement.auth.api.AuthController;
import com.example.leavemanagement.balance.api.AdminBalanceController;
import com.example.leavemanagement.people.api.AdminEmployeeController;
import com.example.leavemanagement.policy.api.AdminHolidayController;
import com.example.leavemanagement.policy.api.AdminPolicyController;
import com.example.leavemanagement.request.api.AdminCorrectionController;
import com.example.leavemanagement.request.api.LeaveCancellationController;
import com.example.leavemanagement.request.api.ManagerLeaveRequestController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiValidationTest {
    private static Map<String,Object> api;

    @BeforeAll static void loadApprovedContract() throws IOException {
        var path = Path.of("..", "specs", "001-employee-leave-management", "contracts", "openapi.yaml").normalize();
        assertThat(path).isRegularFile();
        try (var reader = Files.newBufferedReader(path)) { api = new Yaml().load(reader); }
        assertThat(api.get("openapi")).isEqualTo("3.1.0");
    }

    @Test void everyLocalReferenceResolves() {
        var refs = new ArrayList<String>();
        collectRefs(api, refs);
        assertThat(refs).isNotEmpty().allMatch(ref -> ref.startsWith("#/"));
        assertThat(refs).allSatisfy(ref -> assertThat(resolve(ref)).as(ref).isNotNull());
    }

    @Test void requiredClosedAndPaginationSchemasRemainApproved() {
        var schemas = map(map(api, "components"), "schemas");
        assertThat(map(schemas, "Role").get("enum")).isEqualTo(List.of("EMPLOYEE", "MANAGER", "ADMINISTRATOR"));
        assertThat(map(schemas, "Roles")).containsEntry("minItems", 1).containsEntry("uniqueItems", true);
        assertThat(map(schemas, "LeaveStatus").get("enum")).isEqualTo(List.of("PENDING", "APPROVED", "REJECTED", "CANCELLED"));
        assertThat(map(map(map(schemas, "CorrectionCommand"), "properties"), "action").get("enum"))
                .isEqualTo(List.of("CANCEL_PENDING", "CANCEL_APPROVED", "REOPEN_REJECTED"));
        assertThat(required(schemas, "EmployeeCreateCommand")).contains("initialPassword", "roles");
        assertThat(map(map(map(schemas, "EmployeeCreateCommand"), "properties"), "initialPassword"))
                .containsEntry("writeOnly", true);
        for (var name : List.of("LeaveRequestPage", "EmployeePage", "AuditEventPage"))
            assertThat(required(schemas, name)).containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
        assertThat(required(schemas, "Problem")).containsExactlyInAnyOrder("type", "title", "status", "code", "detail", "correlationId");
        assertThat(map(map(map(schemas, "Problem"), "properties"), "fieldErrors").get("items"))
                .isEqualTo(Map.of("$ref", "#/components/schemas/FieldError"));
    }

    @Test void commandRecordShapesMatchTheApprovedRequestSchemas() {
        assertRecord(AuthController.LoginRequest.class, "LoginRequest");
        assertRecord(ManagerLeaveRequestController.DecisionCommand.class, "DecisionCommand");
        assertRecord(LeaveCancellationController.CommentCommand.class, "CommentCommand");
        assertRecord(AdminCorrectionController.CorrectionCommand.class, "CorrectionCommand");
        assertRecord(AdminEmployeeController.Create.class, "EmployeeCreateCommand");
        assertRecord(AdminEmployeeController.Update.class, "EmployeeUpdateCommand");
        assertRecord(AdminPolicyController.TypeCreate.class, "LeaveTypeCreateCommand");
        assertRecord(AdminPolicyController.TypeUpdate.class, "LeaveTypeUpdateCommand");
        assertRecord(AdminPolicyController.Policy.class, "LeavePolicyCommand");
        assertRecord(AdminHolidayController.Create.class, "HolidayCreateCommand");
        assertRecord(AdminHolidayController.Update.class, "HolidayUpdateCommand");
        assertRecord(AdminBalanceController.Allocation.class, "BalanceAllocationCommand");
        assertRecord(AdminBalanceController.Adjustment.class, "BalanceAdjustmentCommand");
    }

    @Test void operationResponsesAndCsrfDocumentationCannotSilentlyDrift() {
        var paths = map(api, "paths");
        var requiredResponses = Map.ofEntries(
                Map.entry("post /auth/login", Set.of("200","400","401","403")),
                Map.entry("post /employee/leave-requests", Set.of("201","400","401","403","409","422")),
                Map.entry("post /employee/leave-requests/{requestId}/cancel", Set.of("200","400","401","403","404","409","422")),
                Map.entry("post /manager/leave-requests/{requestId}/approve", Set.of("200","400","401","403","404","409")),
                Map.entry("post /manager/leave-requests/{requestId}/reject", Set.of("200","400","401","403","404","409","422")),
                Map.entry("patch /admin/employees/{employeeId}", Set.of("200","400","401","403","404","409")),
                Map.entry("post /admin/leave-requests/{requestId}/corrections", Set.of("200","400","401","403","404","409")));
        requiredResponses.forEach((key, expected) -> {
            var split = key.split(" ", 2); var operation = map(map(paths, split[1]), split[0]);
            assertThat(map(operation, "responses").keySet()).as(key).containsAll(expected);
        });
        paths.forEach((path, itemValue) -> map(itemValue).forEach((method, operationValue) -> {
            if (!Set.of("post", "patch", "put", "delete").contains(method)) return;
            var operation = map(operationValue);
            var refs = list(operation.get("parameters")).stream().map(OpenApiValidationTest::map)
                    .map(p -> String.valueOf(p.get("$ref"))).toList();
            assertThat(refs).as(method + " " + path).contains("#/components/parameters/CsrfHeader");
            assertThat(map(operation, "responses")).as(method + " " + path).containsKey("403");
        }));
    }

    @Test void successfulResponseSchemasNeverExposeCredentialOrSessionSecrets() {
        var forbidden = Set.of("password", "passwordHash", "initialPassword", "sessionSecret", "csrfSecret", "demoCredentialHash");
        var paths = map(api, "paths");
        paths.values().forEach(item -> map(item).values().forEach(operationValue -> {
            var operation = map(operationValue); if (!operation.containsKey("responses")) return;
            map(operation, "responses").forEach((status, responseValue) -> {
                if (!status.startsWith("2")) return;
                var properties = new HashSet<String>(); collectResponseProperties(responseValue, properties, new HashSet<>());
                assertThat(properties).as(status + " response properties").doesNotContainAnyElementsOf(forbidden);
            });
        }));
    }

    private static void assertRecord(Class<?> type, String schemaName) {
        var schemas = map(map(api, "components"), "schemas");
        var recordFields = Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).collect(java.util.stream.Collectors.toSet());
        assertThat(recordFields).as(type.getSimpleName()).containsExactlyInAnyOrderElementsOf(map(map(schemas, schemaName), "properties").keySet());
        assertThat(required(schemas, schemaName)).isSubsetOf(recordFields);
    }
    private static Set<String> required(Map<String,Object> schemas, String name) { return new LinkedHashSet<>(list(map(schemas, name).get("required")).stream().map(String::valueOf).toList()); }
    private static void collectRefs(Object value, List<String> refs) { if (value instanceof Map<?,?> m) m.forEach((k,v)->{if ("$ref".equals(k)) refs.add(String.valueOf(v)); collectRefs(v,refs);}); else if(value instanceof Collection<?> c)c.forEach(v->collectRefs(v,refs)); }
    private static Object resolve(String ref) { Object node=api; for(var token:ref.substring(2).split("/")) node=map(node).get(token.replace("~1","/").replace("~0","~")); return node; }
    private static void collectResponseProperties(Object value, Set<String> names, Set<String> visited) {
        var node=map(value); if(node.containsKey("$ref")){var ref=String.valueOf(node.get("$ref"));if(visited.add(ref))collectResponseProperties(resolve(ref),names,visited);return;}
        if(node.containsKey("properties")){names.addAll(map(node,"properties").keySet());map(node,"properties").values().forEach(v->collectResponseProperties(v,names,visited));}
        for(var key:List.of("content","schema","items","allOf","oneOf","anyOf")){var child=node.get(key);if(child instanceof Collection<?> c)c.forEach(v->collectResponseProperties(v,names,visited));else if(child instanceof Map<?,?>)collectResponseProperties(child,names,visited);}
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value){return value instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();}
    private static Map<String,Object> map(Map<String,Object> parent,String key){return map(parent.get(key));}
    private static List<?> list(Object value){return value instanceof List<?> l?l:List.of();}
}
