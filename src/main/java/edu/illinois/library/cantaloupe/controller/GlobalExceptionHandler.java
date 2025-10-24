package edu.illinois.library.cantaloupe.controller;

import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.ResourceException;

/**
 * Global exception handler for Spring Boot controllers.
 * Handles exceptions that were previously handled by individual Resource classes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EndpointDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleEndpointDisabledException(
            EndpointDisabledException ex, WebRequest request) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 403);
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", "This endpoint is disabled");
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 403);
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }


    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<Map<String, Object>> handleResourceException(
            ResourceException ex, WebRequest request) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", ex.getStatus().getCode());
        errorResponse.put("error", HttpStatus.valueOf(ex.getStatus().getCode()).getReasonPhrase());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(ex.getStatus().getCode())
                             .header("Cache-Control", "no-cache, must-revalidate")
                             .body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 400);
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public  ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 404);
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested resource was not found");
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .header("Cache-Control", "no-cache, must-revalidate")
                             .body(errorResponse);
    }

    @ExceptionHandler(NoSuchFileException.class)
    public  ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoSuchFileException ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 404);
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested resource was not found");
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .header("Cache-Control", "no-cache, must-revalidate")
                             .body(errorResponse);
    }

    @ExceptionHandler(SourceFormatException.class)
    public  ResponseEntity<Map<String, Object>> handleSourceFormatException(SourceFormatException ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
                ex.printStackTrace();

        errorResponse.put("status", 501);
        errorResponse.put("error", "Not Implemented");
        errorResponse.put("message", "Unable to use the requested source format");
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
    }

    // This is a catch all, for any exception not more closely handled.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {

        Map<String, Object> errorResponse = new HashMap<>();
        ex.printStackTrace();
        errorResponse.put("status", 500);
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
