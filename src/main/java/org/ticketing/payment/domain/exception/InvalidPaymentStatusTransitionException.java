package org.ticketing.payment.domain.exception;

import org.ticketing.payment.domain.model.PaymentStatus;

public class InvalidPaymentStatusTransitionException extends RuntimeException {

    public InvalidPaymentStatusTransitionException(PaymentStatus from, PaymentStatus to) {
        super("상태 변경이 불가합니다: " + from + "에서 " + to + "로 변경될 수 없습니다.");
    }
}
