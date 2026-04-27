package org.ticketing.payment.domain.exception;

import org.ticketing.payment.domain.model.PaymentStatus;

public class PaymentAlreadyTerminatedException extends RuntimeException {

    public PaymentAlreadyTerminatedException(PaymentStatus status) {
        super("이미 종료된 결제입니다: " + status);
    }
}
