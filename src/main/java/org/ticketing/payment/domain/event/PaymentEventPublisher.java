package org.ticketing.payment.domain.event;

import java.util.UUID;

public interface PaymentEventPublisher {
    void publishPaymentCompleted(UUID paymentId, UUID orderId);
}