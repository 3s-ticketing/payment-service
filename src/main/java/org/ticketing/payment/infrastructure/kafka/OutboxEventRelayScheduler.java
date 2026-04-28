package org.ticketing.payment.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.domain.event.PaymentEventPublisher;
import org.ticketing.payment.domain.outbox.OutboxStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelayScheduler {

    private final PaymentOutboxRepository outboxRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Scheduled(fixedDelay = 5000) // 얼마가 적절할지 추후에 수정하겠습니다
    @Transactional
    public void relay() {
        outboxRepository.findAllByStatus(OutboxStatus.PENDING).forEach(outbox -> {
            try {
                paymentEventPublisher.publishPaymentCompleted(outbox.getPaymentId(), outbox.getOrderId());
                outbox.markPublished();
                log.info("Outbox event published: paymentId={}", outbox.getPaymentId());
            } catch (Exception e) {
                outbox.markFailed();
                log.error("Failed to relay outbox event: id={}, paymentId={}", outbox.getId(), outbox.getPaymentId(), e);
            }
            outboxRepository.save(outbox);
        });
    }
}