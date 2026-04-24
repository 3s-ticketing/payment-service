package org.ticketing.payment.presentation.dto.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDto {

    private UUID userId;
    private UUID reservationId;
    private UUID seatId;
    private Long totalPrice;

    public CreatePaymentCommand toCommand() {
        return new CreatePaymentCommand(userId, reservationId, seatId, totalPrice);
    }
}