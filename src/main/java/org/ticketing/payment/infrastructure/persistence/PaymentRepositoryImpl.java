package org.ticketing.payment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.repository.PaymentRepository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {
    private final JpaPaymentRepository jpaPaymentRepository;

    @Override
    public Payment save(Payment payment) {
        return jpaPaymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaPaymentRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Payment> findAll(Pageable pageable) {
        return jpaPaymentRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public boolean existsActivePayment(UUID reservationId) {
        return jpaPaymentRepository.existsByReservationIdAndStatusIn(
                reservationId, List.of(PaymentStatus.INIT, PaymentStatus.PAYING));
    }

    @Override
    public Page<Payment> findByReservationId(UUID reservationId, Pageable pageable) {
        return jpaPaymentRepository.findByReservationIdAndDeletedAtIsNull(reservationId, pageable);
    }

    @Override
    public Optional<Payment> findSuccessPaymentByReservationId(UUID reservationId) {
        return jpaPaymentRepository.findFirstByReservationIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(reservationId, PaymentStatus.SUCCESS);
    }

    @Override
    public Page<Payment> findByUserId(UUID userId, Pageable pageable) {
        return jpaPaymentRepository.findByUserIdAndDeletedAtIsNull(userId, pageable);
    }

    @Override
    public List<Payment> findStuckPayments(PaymentStatus status, LocalDateTime before) {
        return jpaPaymentRepository.findByStatusAndModifiedAtBeforeAndDeletedAtIsNull(status, before);
    }
}
