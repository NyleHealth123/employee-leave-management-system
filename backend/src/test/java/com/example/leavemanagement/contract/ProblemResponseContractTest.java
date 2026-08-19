package com.example.leavemanagement.contract;
import com.example.leavemanagement.shared.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ProblemResponseContractTest {
 @RestController static class Probe { @PostMapping("/probe") void probe(@Valid @RequestBody Input input){} @GetMapping("/forbidden") void forbidden(){throw new DomainException(403,"ACCESS_DENIED","Access is denied");} record Input(@NotBlank String value){} }
 private MockMvc mvc(){return MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).addFilters(new CorrelationIdFilter()).build();}
 @Test void validationIsStableAndCorrelated()throws Exception{mvc().perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.fieldErrors[0].field").value("value")).andExpect(header().exists("X-Correlation-ID"));}
 @Test void authorizationMessageIsSafe()throws Exception{mvc().perform(get("/forbidden")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));}
}

