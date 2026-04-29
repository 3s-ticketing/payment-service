package org.ticketing.payment.infrastructure.kafka;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.event.PaymentCompletedEvent;
import org.ticketing.payment.domain.event.PaymentEventPublisher;

@Component
@RequiredArgsConstructor
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final String TOPIC = "payment.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public CompletableFuture<Void> publishPaymentCompleted(UUID paymentId, UUID orderId) {
        return kafkaTemplate
                .send(TOPIC, paymentId.toString(), new PaymentCompletedEvent(paymentId, orderId))
                .thenApply(result -> null);
    }
}