package org.ticketing.payment.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.presentation.dto.request.UpdatePaymentStatusRequestDto;

@Getter
@AllArgsConstructor
public class UpdatePaymentStatusCommand {
    private PaymentStatus status;

    public static UpdatePaymentStatusCommand from(UpdatePaymentStatusRequestDto dto) {
        return new UpdatePaymentStatusCommand(dto.getStatus());
    }
}