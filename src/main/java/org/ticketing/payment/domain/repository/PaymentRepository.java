package org.ticketing.payment.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ticketing.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Page<Payment> findAll(Pageable pageable);
    boolean existsActivePayment(UUID reservationId);
}
