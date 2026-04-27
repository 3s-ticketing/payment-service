package org.ticketing.payment.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ticketing.payment.domain.model.PaymentStatus;

@Getter
@AllArgsConstructor
public class UpdatePaymentStatusCommand {
    private PaymentStatus status;
}