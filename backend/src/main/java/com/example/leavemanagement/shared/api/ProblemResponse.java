package com.example.leavemanagement.shared.api;

import java.net.URI;
import java.util.List;

public record ProblemResponse(URI type,String title,int status,String code,String detail,String correlationId,List<FieldError> fieldErrors) {
    public record FieldError(String field,String code,String message) {}
    public static ProblemResponse of(int status,String code,String detail,String correlationId){return new ProblemResponse(URI.create("about:blank"),title(status),status,code,detail,correlationId,List.of());}
    private static String title(int status){return switch(status){case 400->"Bad Request";case 401->"Unauthorized";case 403->"Forbidden";case 404->"Not Found";case 409->"Conflict";case 422->"Unprocessable Entity";default->"Request Failed";};}
}
