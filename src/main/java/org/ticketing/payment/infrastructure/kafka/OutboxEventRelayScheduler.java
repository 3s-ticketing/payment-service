package org.ticketing.payment.infrastructure.kafka;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public void relay() throws Exception {
        List<PaymentOutbox> outboxes = outboxRepository.fetchAndMarkProcessing(BATCH_SIZE);

        if (outboxes.isEmpty()) return;

        for (PaymentOutbox outbox : outboxes) {
            paymentEventPublisher
                    .publishPaymentCompleted(outbox.getPaymentId(), outbox.getOrderId())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            outbox.markPublished();
                        } else {
                            outbox.markFailed();
                        }
                    });
        }
    }
}
