package org.ticketing.payment.infrastructure.feign.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InternalReservationResponse {

    private UUID reservationId;
    private UUID userId;
    private Long totalPrice;
}