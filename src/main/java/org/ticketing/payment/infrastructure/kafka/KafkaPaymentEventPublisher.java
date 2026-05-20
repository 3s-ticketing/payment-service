package org.ticketing.payment.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
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
    private static final String HEADER_MESSAGE_ID = "message_id";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public CompletableFuture<Void> publishPaymentCompleted(UUID messageId, UUID paymentId, UUID orderId) {
        return send(TOPIC_COMPLETED, messageId, paymentId.toString(), new PaymentCompletedEvent(paymentId, orderId));
    }

    @Override
    public CompletableFuture<Void> publishPaymentRefunded(UUID messageId, UUID paymentId, UUID orderId) {
        return send(TOPIC_REFUNDED, messageId, paymentId.toString(), new PaymentRefundedEvent(paymentId, orderId));
    }

    @Override
    public CompletableFuture<Void> publishPaymentFailed(UUID messageId, UUID paymentId, UUID orderId) {
        return send(TOPIC_FAILED, messageId, paymentId.toString(), new PaymentFailedEvent(paymentId, orderId));
    }

    private CompletableFuture<Void> send(String topic, UUID messageId, String key, Object value) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, value);
        record.headers().add(new RecordHeader(
                HEADER_MESSAGE_ID,
                messageId.toString().getBytes(StandardCharsets.UTF_8)
        ));
        return kafkaTemplate.send(record).thenApply(result -> null);
    }
}