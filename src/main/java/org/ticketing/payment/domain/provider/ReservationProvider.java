package org.ticketing.payment.domain.provider;

import java.util.UUID;

public interface ReservationProvider {

    ReservationSnapshot getReservation(UUID reservationId);

    record ReservationSnapshot(UUID reservationId, UUID userId, Long totalPrice) {}
}