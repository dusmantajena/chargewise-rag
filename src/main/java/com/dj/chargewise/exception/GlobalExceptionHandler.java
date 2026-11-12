package com.dj.chargewise.exception;

import com.dj.chargewise.dto.response.FieldErrorResponse;
import com.dj.chargewise.util.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ChargewiseException.class)
    ResponseEntity<Object> handleChargewiseException(ChargewiseException ex, WebRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("{}: {}", ex.getTitle(), ex.getMessage(), ex);
        } else {
            log.warn("{}: {}", ex.getTitle(), ex.getMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getTitle());
        ex.getProperties().forEach(problem::setProperty);
        HttpHeaders headers = new HttpHeaders();
        ex.getHeaders().forEach(headers::set);
        return handleExceptionInternal(ex, problem, headers, ex.getStatus(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, authenticationDetail(ex));
        problem.setTitle("Unauthorized");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.");
        problem.setTitle("Forbidden");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Quote the correlation ID when reporting it.");
        problem.setTitle("Internal server error");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "One or more fields are invalid.");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "The request body is missing or is not valid JSON.");
        problem.setTitle("Malformed request");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "Parameter '%s' has an invalid value '%s'.".formatted(ex.getPropertyName(), ex.getValue()));
        problem.setTitle("Invalid parameter");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty("timestamp", Instant.now());
            problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static String authenticationDetail(AuthenticationException ex) {
        if (ex instanceof BadCredentialsException) {
            return "Invalid username or password.";
        }
        if (ex instanceof InvalidBearerTokenException) {
            return "The access token is invalid or has expired.";
        }
        return "Authentication is required to access this resource.";
    }

    private static FieldErrorResponse toFieldError(FieldError error) {
        String message = error.isBindingFailure()
                ? "Invalid value '%s'".formatted(error.getRejectedValue())
                : error.getDefaultMessage();
        return new FieldErrorResponse(error.getField(), message);
    }
}
