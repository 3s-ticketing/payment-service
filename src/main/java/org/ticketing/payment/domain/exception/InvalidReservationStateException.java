package org.ticketing.payment.domain.exception;

import java.util.UUID;

public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(UUID reservationId) {
        super("결제 불가한 예약 상태: reservationId=" + reservationId);
    }
}
