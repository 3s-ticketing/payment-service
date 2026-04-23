package org.ticketing.payment.presentation.dto.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDto {

    private UUID userId;
    private UUID reservationId;
    private UUID seatId;
    private Long totalPrice;
}