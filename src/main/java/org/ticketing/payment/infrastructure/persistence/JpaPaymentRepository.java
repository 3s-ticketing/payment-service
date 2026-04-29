package org.ticketing.payment.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);
    Page<Payment> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByReservationIdAndStatusIn(UUID reservationId, List<PaymentStatus> statuses);
    Page<Payment> findByReservationIdAndDeletedAtIsNull(UUID reservationId, Pageable pageable);
    Optional<Payment> findFirstByReservationIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(UUID reservationId, PaymentStatus status);
    Page<Payment> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
}
