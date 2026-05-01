package org.ticketing.payment.infrastructure.kafka.event;

import java.util.UUID;

public record ReservationCanceledEvent(
        UUID reservationId,
        UUID userId,
        CancelReason cancelReason
) {
    public enum CancelReason {
        USER_CANCEL, EXPIRED, MATCH_CANCELED
    }
}
