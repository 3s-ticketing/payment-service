package org.ticketing.payment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
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
    @Positive
    private Long totalPrice;

    public ConfirmPaymentCommand toCommand(UUID userId) {
        return new ConfirmPaymentCommand(userId, paymentKey, paymentId, totalPrice);
    }
}