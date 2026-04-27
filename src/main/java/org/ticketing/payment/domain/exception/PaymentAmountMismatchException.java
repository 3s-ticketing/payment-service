package org.ticketing.payment.domain.exception;

public class PaymentAmountMismatchException extends RuntimeException {

    public PaymentAmountMismatchException(Long expected, Long actual) {
        super("결제 금액 불일치: 저장된 금액 " + expected + ", 요청 금액 " + actual);
    }
}