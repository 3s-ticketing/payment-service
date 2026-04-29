package org.ticketing.payment.domain.outbox;

import java.util.List;

public interface PaymentOutboxRepository {
    PaymentOutbox save(PaymentOutbox outbox);
    List<PaymentOutbox> fetchAndMarkProcessing(int size);
}