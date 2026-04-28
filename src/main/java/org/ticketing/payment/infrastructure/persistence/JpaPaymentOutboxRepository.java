package org.ticketing.payment.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.ticketing.payment.domain.outbox.OutboxStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;

public interface JpaPaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
    List<PaymentOutbox> findAllByStatus(OutboxStatus status);

    @Modifying
    @Query("UPDATE PaymentOutbox o SET o.status = 'PROCESSING' WHERE o.id = :id AND o.status = 'PENDING'")
    int markProcessingIfPending(UUID id);
}