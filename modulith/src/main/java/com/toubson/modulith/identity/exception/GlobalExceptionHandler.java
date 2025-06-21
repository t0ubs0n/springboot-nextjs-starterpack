package com.toubson.modulith.identity.exception;

import com.toubson.modulith.identity.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AuthenticationException.class,
            BadCredentialsException.class,
            UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            Exception ex, HttpServletRequest request) {
        String message = ex.getMessage();
        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password";
        }
        return getErrorResponseResponseEntity(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return getErrorResponseResponseEntity(HttpStatus.BAD_REQUEST, "Validation failed: " + errors, request);
    }

    private static ResponseEntity<ErrorResponse> getErrorResponseResponseEntity(HttpStatus httpStatus, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(httpStatus, message, request.getRequestURI());
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return getErrorResponseResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }
}