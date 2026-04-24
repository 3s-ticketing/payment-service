package org.ticketing.payment.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.payment.domain.model.PaymentStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequestDto {
    private PaymentStatus status;
}