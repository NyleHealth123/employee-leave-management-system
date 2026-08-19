package com.example.leavemanagement.shared.api;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {
    public static final String ATTRIBUTE="correlationId";
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException{
        var http=(HttpServletRequest)request;var out=(HttpServletResponse)response;
        var supplied=http.getHeader("X-Correlation-ID");var id=supplied==null||supplied.isBlank()?UUID.randomUUID().toString():supplied.substring(0,Math.min(100,supplied.length()));
        request.setAttribute(ATTRIBUTE,id);out.setHeader("X-Correlation-ID",id);chain.doFilter(request,response);
    }
}

