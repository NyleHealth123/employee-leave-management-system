package com.example.leavemanagement.security;

import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.auth.persistence.UserAccountEntity;
import com.example.leavemanagement.auth.persistence.UserAccountRepository;
import com.example.leavemanagement.people.persistence.EmployeeProfileEntity;
import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountAuthenticationTest {
    private static final String LOGIN = "demo.admin";
    private static final String PASSWORD = "ephemeral-demo-password";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void springCompatibleBcryptAdminAuthenticationMapsOnlyApplicationRoles() {
        var fixture = fixture();

        var authentication = fixture.manager().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(LOGIN, PASSWORD));

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_ADMINISTRATOR", FactorGrantedAuthority.PASSWORD_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var actor = fixture.actors().require();

        assertThat(actor.roles()).containsExactly("ADMINISTRATOR");
        assertThat(actor.userId()).isEqualTo(fixture.accountId());
    }

    @Test
    void springCompatibleBcryptRejectsTheWrongPassword() {
        var fixture = fixture();

        assertThatThrownBy(() -> fixture.manager().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(LOGIN, "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private Fixture fixture() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var encoded = encoder.encode(PASSWORD).replace("{bcrypt}$2a$", "{bcrypt}$2y$");
        assertThat(encoded).startsWith("{bcrypt}$2y$");
        assertThat(encoder.matches(PASSWORD, encoded)).isTrue();

        var accountId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var account = UserAccountEntity.create(
                accountId, LOGIN, LOGIN, encoded, Set.of("ADMINISTRATOR"), Instant.now());
        var employee = EmployeeProfileEntity.create(
                employeeId, "DEMO-ADMIN-001", account, "Demo Administrator",
                "demo.admin@example.test", null, true, Instant.now());

        var accounts = mock(UserAccountRepository.class);
        when(accounts.findByNormalizedLogin(LOGIN)).thenReturn(Optional.of(account));
        var employees = mock(EmployeeProfileRepository.class);
        when(employees.findByUserAccountId(accountId)).thenReturn(Optional.of(employee));

        var provider = new DaoAuthenticationProvider(new AccountUserDetailsService(accounts));
        provider.setPasswordEncoder(encoder);
        return new Fixture(accountId, new ProviderManager(provider), new CurrentActorProvider(employees));
    }

    private record Fixture(UUID accountId, ProviderManager manager, CurrentActorProvider actors) {}
}
