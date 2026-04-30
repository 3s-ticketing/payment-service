package org.ticketing.payment.infrastructure.kafka;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.event.PaymentCompletedEvent;
import org.ticketing.payment.domain.event.PaymentEventPublisher;
import org.ticketing.payment.domain.event.PaymentFailedEvent;
import org.ticketing.payment.domain.event.PaymentRefundedEvent;

@Component
@RequiredArgsConstructor
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final String TOPIC_COMPLETED = "payment.completed";
    private static final String TOPIC_REFUNDED  = "payment.refunded";
    private static final String TOPIC_FAILED    = "payment.failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public CompletableFuture<Void> publishPaymentCompleted(UUID paymentId, UUID orderId) {
        return kafkaTemplate
                .send(TOPIC_COMPLETED, paymentId.toString(), new PaymentCompletedEvent(paymentId, orderId))
                .thenApply(result -> null);
    }

    @Override
    public CompletableFuture<Void> publishPaymentRefunded(UUID paymentId, UUID orderId) {
        return kafkaTemplate
                .send(TOPIC_REFUNDED, paymentId.toString(), new PaymentRefundedEvent(paymentId, orderId))
                .thenApply(result -> null);
    }

    @Override
    public CompletableFuture<Void> publishPaymentFailed(UUID paymentId, UUID orderId) {
        return kafkaTemplate
                .send(TOPIC_FAILED, paymentId.toString(), new PaymentFailedEvent(paymentId, orderId))
                .thenApply(result -> null);
    }
}