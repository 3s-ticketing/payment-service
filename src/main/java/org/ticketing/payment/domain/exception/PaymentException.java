package org.ticketing.payment.domain.exception;

import lombok.Getter;
import org.ticketing.common.exception.CustomException;

@Getter
public class PaymentException extends CustomException {

    private final PaymentErrorCode code;

    public PaymentException(PaymentErrorCode code) {
        super(code.getMessage(), code.getStatus());
        this.code = code;
    }

    public PaymentException(PaymentErrorCode code, String detail) {
        super(code.getMessage() + ": " + detail, code.getStatus());
        this.code = code;
    }
}
