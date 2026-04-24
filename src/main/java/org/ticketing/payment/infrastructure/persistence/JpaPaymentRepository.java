package org.ticketing.payment.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketing.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);
    Page<Payment> findAllByDeletedAtIsNull(Pageable pageable);
}
