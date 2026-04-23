package org.ticketing.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketing.payment.domain.model.Payment;

import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {
}
