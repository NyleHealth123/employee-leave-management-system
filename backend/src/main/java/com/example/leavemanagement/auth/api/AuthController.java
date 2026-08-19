package com.example.leavemanagement.auth.api;

import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.UUID;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;private final SecurityContextRepository contexts;private final CsrfTokenRepository csrfTokens;private final CurrentActorProvider actors;
    public AuthController(AuthenticationManager authenticationManager,SecurityContextRepository contexts,CsrfTokenRepository csrfTokens,CurrentActorProvider actors){this.authenticationManager=authenticationManager;this.contexts=contexts;this.csrfTokens=csrfTokens;this.actors=actors;}
    @GetMapping("/csrf") public CsrfResponse csrf(HttpServletRequest request,HttpServletResponse response){var token=csrfTokens.loadDeferredToken(request,response).get();return new CsrfResponse(token.getToken(),token.getHeaderName(),token.getParameterName());}
    @PostMapping("/login") public PrincipalResponse login(@Valid @RequestBody LoginRequest input,HttpServletRequest request,HttpServletResponse response){try{var authentication=authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.login(),input.password()));request.getSession(true);request.changeSessionId();var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(authentication);SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);return principal();}catch(AuthenticationException ex){throw new DomainException(401,"INVALID_CREDENTIALS","Invalid login or password");}}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(HttpServletRequest request,HttpServletResponse response){new SecurityContextLogoutHandler().logout(request,response,SecurityContextHolder.getContext().getAuthentication());}
    @GetMapping("/me") public PrincipalResponse principal(){var a=actors.require();return new PrincipalResponse(a.userId(),a.employeeId(),a.displayName(),a.roles());}
    public record LoginRequest(@NotBlank @Size(max=254) String login,@NotBlank @Size(max=200) String password){}
    public record CsrfResponse(String token,String headerName,String parameterName){}
    public record PrincipalResponse(UUID userId,UUID employeeId,String displayName,Set<String> roles){}
}
