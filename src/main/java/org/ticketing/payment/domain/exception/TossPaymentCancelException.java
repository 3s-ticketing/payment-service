package org.ticketing.payment.domain.exception;

public class TossPaymentCancelException extends RuntimeException {

    public TossPaymentCancelException(int statusCode, String responseBody) {
        super("Toss 결제 취소 실패. status: " + statusCode + ", body: " + responseBody);
    }
}