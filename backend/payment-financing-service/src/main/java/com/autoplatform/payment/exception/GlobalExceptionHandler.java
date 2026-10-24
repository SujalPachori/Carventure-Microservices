package com.autoplatform.payment.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CarAlreadySoldException.class)
    public ProblemDetail handleCarSold(CarAlreadySoldException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Car Already Sold", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidState(InvalidStateTransitionException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Invalid State Transition", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(CarServiceUnavailableException.class)
    public ProblemDetail handleCarServiceDown(CarServiceUnavailableException ex, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Car Service Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ProblemDetail handlePaymentProcessing(PaymentProcessingException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_GATEWAY, "Payment Processing Error", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ProblemDetail handleBadSignature(InvalidWebhookSignatureException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Webhook Signature", ex.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Concurrent Modification",
                "This order was modified concurrently - please retry with the latest state", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more fields are invalid", request);
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setInstance(java.net.URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
