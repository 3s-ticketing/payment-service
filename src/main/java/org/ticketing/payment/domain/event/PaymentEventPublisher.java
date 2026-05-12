package org.ticketing.payment.domain.event;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PaymentEventPublisher {
    CompletableFuture<Void> publishPaymentCompleted(UUID messageId, UUID paymentId, UUID orderId);
    CompletableFuture<Void> publishPaymentFailed(UUID messageId, UUID paymentId, UUID orderId);
    CompletableFuture<Void> publishPaymentRefunded(UUID messageId, UUID paymentId, UUID orderId);
}