package org.ticketing.payment.infrastructure.persistence;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.ticketing.payment.domain.outbox.OutboxStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;

@Repository
@RequiredArgsConstructor
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepository {

    private final JpaPaymentOutboxRepository jpa;

    @Override
    public PaymentOutbox save(PaymentOutbox outbox) {
        return jpa.save(outbox);
    }

    @Override
    public List<PaymentOutbox> findAllByStatus(OutboxStatus status) {
        return jpa.findAllByStatus(status);
    }
}