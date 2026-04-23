package org.ticketing.payment.presentation.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResponseDto {

    private UUID id;
    private UUID userId;
    private UUID reservationId;
    private UUID seatId;
    private Long totalPrice;
    private String status;
}