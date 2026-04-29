package org.ticketing.payment.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Page<Payment> findAll(Pageable pageable);
    boolean existsActivePayment(UUID reservationId);
    Page<Payment> findByReservationId(UUID reservationId, Pageable pageable);
    Optional<Payment> findSuccessPaymentByReservationId(UUID reservationId);
    Page<Payment> findByUserId(UUID userId, Pageable pageable);
    List<Payment> findStuckPayments(PaymentStatus status, LocalDateTime before);
}
