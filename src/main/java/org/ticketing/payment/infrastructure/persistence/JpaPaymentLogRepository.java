package org.ticketing.payment.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketing.payment.domain.model.PaymentLog;

public interface JpaPaymentLogRepository extends JpaRepository<PaymentLog, UUID> {
}