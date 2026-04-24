package org.ticketing.payment.application.dto.command;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreatePaymentCommand {

    private UUID userId;
    private UUID reservationId;
    private UUID seatId;
    private Long totalPrice;
}