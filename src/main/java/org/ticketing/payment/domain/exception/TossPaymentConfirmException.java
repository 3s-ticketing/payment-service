package org.ticketing.payment.domain.exception;

public class TossPaymentConfirmException extends RuntimeException {

    public TossPaymentConfirmException(int statusCode, String responseBody) {
        super("Toss 결제 승인 실패. status: " + statusCode + ", body: " + responseBody);
    }
}