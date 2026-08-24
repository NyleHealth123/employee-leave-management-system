package com.example.leavemanagement.contract;
import com.example.leavemanagement.shared.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ProblemResponseContractTest {
 @RestController static class Probe { @PostMapping("/probe") void probe(@Valid @RequestBody Input input){} @GetMapping("/{kind}") void problem(@PathVariable String kind){throw switch(kind){case "unauthorized"->new DomainException(401,"AUTHENTICATION_REQUIRED","Authentication is required");case "forbidden"->new DomainException(403,"ACCESS_DENIED","Access is denied");case "missing"->new DomainException(404,"RESOURCE_NOT_FOUND","Resource was not found");case "unexpected"->new IllegalStateException("internal diagnostic detail");default->new DomainException(409,"LEAVE_OVERLAP","The request overlaps active leave");};} record Input(@NotBlank String value){} }
 private MockMvc mvc(){return MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).addFilters(new CorrelationIdFilter()).build();}
 @Test void validationIsStableAndCorrelated()throws Exception{mvc().perform(post("/probe").header("X-Correlation-ID","contract-correlation").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.fieldErrors[0].field").value("value")).andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank")).andExpect(jsonPath("$.correlationId").value("contract-correlation")).andExpect(header().string("X-Correlation-ID","contract-correlation"));}
 @Test void safeAuthenticationAuthorizationAndNotFoundProblemsUseStableCodes()throws Exception{mvc().perform(get("/unauthorized")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED")).andExpect(jsonPath("$.detail").value("Authentication is required"));mvc().perform(get("/forbidden")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));mvc().perform(get("/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));}
 @Test void businessProblemsPreserveStatusCodeAndCorrelation()throws Exception{mvc().perform(get("/conflict").header("X-Correlation-ID","business-correlation")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LEAVE_OVERLAP")).andExpect(jsonPath("$.correlationId").value("business-correlation"));}
 @Test void unexpectedProblemsRemainGenericAndCorrelated()throws Exception{mvc().perform(get("/unexpected").header("X-Correlation-ID","unexpected-correlation")).andExpect(status().isInternalServerError()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.code").value("INTERNAL_ERROR")).andExpect(jsonPath("$.detail").value("The request could not be completed")).andExpect(jsonPath("$.correlationId").value("unexpected-correlation")).andExpect(content().string(not(containsString("internal diagnostic detail")))).andExpect(content().string(not(containsString("IllegalStateException"))));}
}
