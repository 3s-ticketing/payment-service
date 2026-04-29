package org.ticketing.payment.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketing.payment.domain.outbox.PaymentOutbox;

public interface JpaPaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
}