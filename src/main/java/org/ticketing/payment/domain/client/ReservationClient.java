package org.ticketing.payment.domain.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public interface ReservationClient {

    ReservationDetail getReservationDetail(UUID reservationId);

    record ReservationDetail(
            @JsonProperty("reservationId") UUID reservationId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("totalPrice") Long totalPrice,
            @JsonProperty("isValid") boolean isValid
    ) {}
}
