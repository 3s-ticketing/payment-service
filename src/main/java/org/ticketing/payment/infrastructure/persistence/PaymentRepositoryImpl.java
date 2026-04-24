package org.ticketing.payment.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

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
}
