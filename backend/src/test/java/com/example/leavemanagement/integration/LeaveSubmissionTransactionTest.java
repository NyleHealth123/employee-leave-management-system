package com.example.leavemanagement.integration;
import com.example.leavemanagement.request.application.LeaveSubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;
class LeaveSubmissionTransactionTest {
 @Test void submissionDeclaresOneTransactionBoundary()throws Exception{Method method=java.util.Arrays.stream(LeaveSubmissionService.class.getDeclaredMethods()).filter(m->m.getName().equals("submit")).findFirst().orElseThrow();assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();}
}

