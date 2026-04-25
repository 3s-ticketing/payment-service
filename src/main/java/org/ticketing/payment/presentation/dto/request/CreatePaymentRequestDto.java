package org.ticketing.payment.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDto {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID reservationId;

    @NotNull
    @Positive
    private Long totalPrice;

    public CreatePaymentCommand toCommand() {
        return new CreatePaymentCommand(userId, reservationId, totalPrice);
    }
}