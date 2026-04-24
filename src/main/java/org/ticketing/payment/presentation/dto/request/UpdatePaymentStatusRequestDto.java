package org.ticketing.payment.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.payment.application.dto.command.UpdatePaymentStatusCommand;
import org.ticketing.payment.domain.model.PaymentStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequestDto {
    private PaymentStatus status;

    public UpdatePaymentStatusCommand toCommand() {
        return new UpdatePaymentStatusCommand(status);
    }
}