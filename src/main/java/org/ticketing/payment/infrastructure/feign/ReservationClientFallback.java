package org.ticketing.payment.infrastructure.feign;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.client.ReservationClient;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;

@Component
public class ReservationClientFallback implements ReservationClient {

    @Override
    public ReservationDetail getReservationDetail(UUID reservationId) {
        throw new PaymentException(PaymentErrorCode.RESERVATION_SERVICE_UNAVAILABLE,
                "reservationId=" + reservationId);
    }
}