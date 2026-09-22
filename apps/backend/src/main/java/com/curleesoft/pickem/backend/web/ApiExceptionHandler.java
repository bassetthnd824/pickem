package com.curleesoft.pickem.backend.web;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.curleesoft.pickem.backend.repository.StaleDocumentVersionException;
import com.curleesoft.pickem.backend.service.ConflictException;
import com.curleesoft.pickem.backend.service.InvalidRequestException;
import com.curleesoft.pickem.backend.service.ResourceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail badRequest(InvalidRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + defaultMessage(error.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        if (!StringUtils.hasText(detail)) {
            detail = "Request is invalid";
        }

        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ProblemDetail unreadable(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Request is invalid");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StaleDocumentVersionException.class)
    public ProblemDetail staleVersion(StaleDocumentVersionException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }

    private static String defaultMessage(String message) {
        return message == null ? "is invalid" : message;
    }
}
