package org.ticketing.payment.domain.exception;

import java.util.UUID;

public class PaymentReservationMismatchException extends RuntimeException {

    public PaymentReservationMismatchException(UUID expected, UUID actual) {
        super("예약 ID 불일치: 저장된 ID " + expected + ", 요청 ID " + actual);
    }
}