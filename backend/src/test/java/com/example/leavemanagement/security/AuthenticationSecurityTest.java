package com.example.leavemanagement.security;
import com.example.leavemanagement.auth.api.AuthController;
import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.shared.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Set;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(AuthController.class) @Import(SecurityConfiguration.class)
class AuthenticationSecurityTest {
 @Autowired MockMvc mvc;@MockitoBean AuthenticationManager manager;@MockitoBean CurrentActorProvider actors;@MockitoBean AccountUserDetailsService users;
 @Test void csrfBootstrapIsAvailableBeforeLogin()throws Exception{mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN")).andExpect(jsonPath("$.token").isNotEmpty());}
 @Test void missingCsrfRejectsLoginBeforeAuthentication()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"employee\",\"password\":\"secret\"}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));verifyNoInteractions(manager);}
 @Test void invalidCsrfRejectsLoginBeforeAuthentication()throws Exception{mvc.perform(post("/api/auth/login").header("X-XSRF-TOKEN","invalid").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"employee\",\"password\":\"secret\"}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));verifyNoInteractions(manager);}
 @Test void malformedOrMissingBodyIsValidationFailure()throws Exception{mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));}
 @Test void invalidCredentialsAreGeneric()throws Exception{when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"unknown\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS")).andExpect(jsonPath("$.detail").value("Invalid login or password"));}
 @Test void principalReturnsClosedAssignedRoles()throws Exception{var id=UUID.randomUUID();when(actors.require()).thenReturn(new CurrentActor(id,UUID.randomUUID(),null,Set.of("EMPLOYEE"),"Employee"));mvc.perform(get("/api/auth/me").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(id.toString()).roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));}
}
