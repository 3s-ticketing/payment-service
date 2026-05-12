package org.ticketing.payment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessRequestDto {

    @NotBlank
    private String paymentKey;

    @NotNull
    private UUID paymentId;

    @NotNull
    @PositiveOrZero
    private Long totalPrice;

    public ConfirmPaymentCommand toCommand() {
        return new ConfirmPaymentCommand(paymentKey, paymentId, totalPrice);
    }
}