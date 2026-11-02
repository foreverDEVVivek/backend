
package devPilot.backend.exceptions;

import java.time.Instant;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<?> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {
        return handleError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<?> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {
        return handleError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<?> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {
        return handleError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");

        return handleError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        ex.printStackTrace();

        String message = ex.getMessage() != null
                ? ex.getMessage()
                : "Unexpected error";

        return handleError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                request
        );
    }

    private ResponseEntity<?> handleError(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        String accept = request.getHeader("Accept");

        // SSE requests should not receive a JSON Map.
        if (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message);
        }

        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }
}
