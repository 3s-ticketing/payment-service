package org.ticketing.payment.infrastructure.kafka;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.event.PaymentEventPublisher;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelayScheduler {

    private static final int BATCH_SIZE = 100; // 얼마가 적절할지 추후에 수정하겠습니다

    private final PaymentOutboxRepository outboxRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Scheduled(fixedDelay = 5000) // 얼마가 적절할지 추후에 수정하겠습니다
    public void relay() throws Exception {
        List<PaymentOutbox> outboxes = outboxRepository.fetchAndMarkProcessing(BATCH_SIZE);

        if (outboxes.isEmpty()) return;

        for (PaymentOutbox outbox : outboxes) {
            publish(outbox).whenComplete((result, ex) -> {
                if (ex == null) {
                    outbox.markPublished();
                } else {
                    outbox.markFailed();
                }
                outboxRepository.save(outbox);
            });
        }
    }

    private CompletableFuture<Void> publish(PaymentOutbox outbox) {
        UUID paymentId = outbox.getPaymentId();
        UUID orderId   = outbox.getOrderId();
        return switch (outbox.getTopic()) {
            case "payment.completed" -> paymentEventPublisher.publishPaymentCompleted(paymentId, orderId);
            case "payment.refunded"  -> paymentEventPublisher.publishPaymentRefunded(paymentId, orderId);
            default -> CompletableFuture.failedFuture(
                    new IllegalStateException("알 수 없는 outbox 토픽: " + outbox.getTopic()));
        };
    }
}
