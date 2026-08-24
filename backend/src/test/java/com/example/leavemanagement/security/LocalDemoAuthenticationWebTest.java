package com.example.leavemanagement.security;

import com.example.leavemanagement.auth.api.AuthController;
import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.auth.persistence.UserAccountEntity;
import com.example.leavemanagement.auth.persistence.UserAccountRepository;
import com.example.leavemanagement.people.persistence.EmployeeProfileEntity;
import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import com.example.leavemanagement.shared.security.SecurityConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfiguration.class, AccountUserDetailsService.class, CurrentActorProvider.class})
class LocalDemoAuthenticationWebTest {
    private static final String LOGIN = "demo.admin";
    private static final String PASSWORD = "ephemeral-demo-password";

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UserAccountRepository accounts;
    @MockitoBean EmployeeProfileRepository employees;

    @BeforeEach
    void demoAdministrator() {
        var accountId = UUID.randomUUID();
        var encoded = encoder.encode(PASSWORD).replace("{bcrypt}$2a$", "{bcrypt}$2y$");
        var account = UserAccountEntity.create(
                accountId, LOGIN, LOGIN, encoded, Set.of("ADMINISTRATOR"), Instant.now());
        var employee = EmployeeProfileEntity.create(
                UUID.randomUUID(), "DEMO-ADMIN-001", account, "Demo Administrator",
                "demo.admin@example.test", null, true, Instant.now());
        when(accounts.findByNormalizedLogin(LOGIN)).thenReturn(Optional.of(account));
        when(employees.findByUserAccountId(accountId)).thenReturn(Optional.of(employee));
    }

    @Test
    void validCsrfAndDemoCredentialsReturnASuccessfulAdministratorLogin() throws Exception {
        var csrf = csrfBootstrap();

        mvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"demo.admin\",\"password\":\"ephemeral-demo-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Demo Administrator"))
                .andExpect(jsonPath("$.roles[0]").value("ADMINISTRATOR"));
    }

    @Test
    void wrongDemoPasswordWithValidCsrfReturns401() throws Exception {
        var csrf = csrfBootstrap();

        mvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"demo.admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    private CsrfBootstrap csrfBootstrap() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();
        return new CsrfBootstrap(
                JsonPath.read(body, "$.token"),
                JsonPath.read(body, "$.headerName"),
                result.getResponse().getCookie("XSRF-TOKEN"));
    }

    private record CsrfBootstrap(String token, String headerName, Cookie cookie) {}
}
