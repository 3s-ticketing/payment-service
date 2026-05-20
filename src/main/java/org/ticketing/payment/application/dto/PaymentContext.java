package org.ticketing.payment.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ticketing.payment.domain.model.PaymentStatus;

public record PaymentContext(
        UUID paymentId,
        UUID userId,
        UUID reservationId,
        Long totalPrice,
        String paymentKey,
        PaymentStatus status,
        LocalDateTime modifiedAt
) {
    public PaymentContext withStatus(PaymentStatus newStatus) {
        return new PaymentContext(paymentId, userId, reservationId, totalPrice, paymentKey, newStatus, modifiedAt);
    }
}
