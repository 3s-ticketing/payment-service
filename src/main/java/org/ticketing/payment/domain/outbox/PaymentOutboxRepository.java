package org.ticketing.payment.domain.outbox;

import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository {
    PaymentOutbox save(PaymentOutbox outbox);
    List<PaymentOutbox> findAllByStatus(OutboxStatus status);
    boolean markProcessingIfPending(UUID id);
}