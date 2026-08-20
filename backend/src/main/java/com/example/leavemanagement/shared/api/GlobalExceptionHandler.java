package com.example.leavemanagement.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemResponse> domain(DomainException ex,HttpServletRequest request){return problem(ex.status(),ex.code(),ex.getMessage(),request,List.of());}
    @ExceptionHandler({HttpMessageNotReadableException.class})
    ResponseEntity<ProblemResponse> unreadable(Exception ex,HttpServletRequest request){return problem(400,"VALIDATION_FAILED","Request body is missing or malformed",request,List.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> invalid(MethodArgumentNotValidException ex,HttpServletRequest request){var fields=ex.getBindingResult().getFieldErrors().stream().map(e->new ProblemResponse.FieldError(e.getField(),e.getCode(),e.getDefaultMessage())).toList();return problem(400,"VALIDATION_FAILED","Request validation failed",request,fields);}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> unexpected(Exception ex,HttpServletRequest request){return problem(500,"INTERNAL_ERROR","The request could not be completed",request,List.of());}
    private ResponseEntity<ProblemResponse> problem(int status,String code,String detail,HttpServletRequest request,List<ProblemResponse.FieldError> fields){var id=String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE));var body=new ProblemResponse(java.net.URI.create("about:blank"),HttpStatus.valueOf(status).getReasonPhrase(),status,code,detail,id,fields);return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);}
}
