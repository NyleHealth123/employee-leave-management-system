package com.example.leavemanagement.shared.security;

import com.example.leavemanagement.shared.api.ProblemResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.context.*;

@Configuration @EnableWebSecurity
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return PasswordEncoderFactories.createDelegatingPasswordEncoder();}
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)throws Exception{return configuration.getAuthenticationManager();}
    @Bean SecurityContextRepository securityContextRepository(){return new HttpSessionSecurityContextRepository();}
    @Bean CsrfTokenRepository csrfTokenRepository(){var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();csrf.setCookiePath("/");csrf.setHeaderName("X-XSRF-TOKEN");csrf.setCookieName("XSRF-TOKEN");return csrf;}
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,ObjectMapper mapper,SecurityContextRepository contexts,CsrfTokenRepository csrf)throws Exception{
        http.csrf(c->c.csrfTokenRepository(csrf))
            .securityContext(c->c.securityContextRepository(contexts).requireExplicitSave(true))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/actuator/health").permitAll().requestMatchers("/api/employee/**","/api/leave-types","/api/holidays").hasRole("EMPLOYEE").anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->write(mapper,res,401,"AUTHENTICATION_REQUIRED","Authentication is required")).accessDeniedHandler((req,res,ex)->write(mapper,res,403,"ACCESS_DENIED","Access is denied")))
            .sessionManagement(s->s.sessionFixation(f->f.changeSessionId()))
            .logout(l->l.disable()).httpBasic(h->h.disable()).formLogin(f->f.disable());
        return http.build();
    }
    private static void write(ObjectMapper mapper,HttpServletResponse response,int status,String code,String detail)throws java.io.IOException{response.setStatus(status);response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);mapper.writeValue(response.getOutputStream(),ProblemResponse.of(status,code,detail,"security"));}
}
