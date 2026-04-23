package org.ticketing.payment.application.dto.command;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ticketing.payment.presentation.dto.request.CreatePaymentRequestDto;

@Getter
@AllArgsConstructor
public class CreatePaymentCommand {

    private UUID userId;
    private UUID reservationId;
    private UUID seatId;
    private Long totalPrice;

    public static CreatePaymentCommand from(CreatePaymentRequestDto dto) {
        return new CreatePaymentCommand(
                dto.getUserId(),
                dto.getReservationId(),
                dto.getSeatId(),
                dto.getTotalPrice()
        );
    }
}