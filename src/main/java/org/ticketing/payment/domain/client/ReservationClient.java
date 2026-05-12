package org.ticketing.payment.domain.client;

import java.util.UUID;

public interface ReservationClient {

    ReservationDetail getReservationDetail(UUID reservationId);

    record ReservationDetail(UUID reservationId, UUID userId, Long totalPrice, boolean isValid) {}
}
