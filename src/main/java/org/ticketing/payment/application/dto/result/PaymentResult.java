package org.ticketing.payment.application.dto.result;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;

@Getter
@AllArgsConstructor
public class PaymentResult {

    private UUID id;
    private UUID userId;
    private UUID reservationId;
    private Long totalPrice;
    private PaymentStatus status;

    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getUserId(),
                payment.getReservationId(),
                payment.getTotalPrice(),
                payment.getStatus()
        );
    }
}