package org.ticketing.payment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Payment> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Page<Payment> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByReservationIdAndStatusIn(UUID reservationId, List<PaymentStatus> statuses);
    Page<Payment> findByReservationIdAndDeletedAtIsNull(UUID reservationId, Pageable pageable);
    Page<Payment> findByReservationIdAndUserIdAndDeletedAtIsNull(UUID reservationId, UUID userId, Pageable pageable);
    Optional<Payment> findFirstByReservationIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(UUID reservationId, PaymentStatus status);
    Optional<Payment> findFirstByReservationIdAndUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(UUID reservationId, UUID userId, PaymentStatus status);
    Page<Payment> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
    List<Payment> findByStatusAndModifiedAtBeforeAndDeletedAtIsNull(PaymentStatus status, LocalDateTime before);
}
