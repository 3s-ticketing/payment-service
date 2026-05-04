package org.ticketing.payment.presentation.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.ticketing.payment.domain.exception.DuplicatePaymentException;
import org.ticketing.payment.domain.exception.InvalidPaymentStatusTransitionException;
import org.ticketing.payment.domain.exception.PaymentAlreadyTerminatedException;
import org.ticketing.payment.domain.exception.PaymentAmountMismatchException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.exception.PaymentReservationMismatchException;
import org.ticketing.payment.domain.exception.TossPaymentCancelException;
import org.ticketing.payment.domain.exception.TossPaymentConfirmException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentControllerAdvice {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePayment(DuplicatePaymentException ex) {
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        log.warn("DataIntegrityViolation: {}", cause);
        if (cause != null && cause.toLowerCase().contains("unique")) {
            return errorResponse(HttpStatus.CONFLICT, "Active payment already exists for this reservation");
        }
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "A database integrity error occurred");
    }

    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatusTransition(InvalidPaymentStatusTransitionException ex) {
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyTerminatedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyTerminated(PaymentAlreadyTerminatedException ex) {
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentAmountMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleAmountMismatch(PaymentAmountMismatchException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PaymentReservationMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleReservationMismatch(PaymentReservationMismatchException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TossPaymentConfirmException.class)
    public ResponseEntity<Map<String, Object>> handleTossConfirmFail(TossPaymentConfirmException ex) {
        log.error("Toss confirm failed: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_GATEWAY, "Payment confirmation failed due to an upstream error");
    }

    @ExceptionHandler(TossPaymentCancelException.class)
    public ResponseEntity<Map<String, Object>> handleTossCancelFail(TossPaymentCancelException ex) {
        log.error("Toss cancel failed: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_GATEWAY, "Payment cancellation failed due to an upstream error");
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.toString());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}