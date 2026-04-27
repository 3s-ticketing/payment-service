package org.ticketing.payment.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.payment.application.dto.command.UpdatePaymentStatusCommand;
import org.ticketing.payment.domain.model.PaymentStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequestDto {

    @NotNull
    private PaymentStatus status;

    public UpdatePaymentStatusCommand toCommand() {
        return new UpdatePaymentStatusCommand(status);
    }
}