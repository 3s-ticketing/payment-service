package org.ticketing.payment.domain.exception;

import java.util.UUID;

public class PaymentUserMismatchException extends RuntimeException {

    public PaymentUserMismatchException(UUID expected, UUID actual) {
        super("사용자 ID 불일치: 예약의 userId=" + expected + ", 요청 userId=" + actual);
    }
}
