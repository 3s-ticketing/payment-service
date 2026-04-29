package org.ticketing.payment.domain.event;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PaymentEventPublisher {
    CompletableFuture<Void> publishPaymentCompleted(UUID paymentId, UUID orderId);
    CompletableFuture<Void> publishPaymentRefunded(UUID paymentId, UUID orderId);
}