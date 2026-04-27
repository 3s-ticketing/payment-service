package org.ticketing.payment.domain.exception;

import java.util.UUID;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(UUID reservationId) {
        super("Active payment already exists for reservation: " + reservationId);
    }
}