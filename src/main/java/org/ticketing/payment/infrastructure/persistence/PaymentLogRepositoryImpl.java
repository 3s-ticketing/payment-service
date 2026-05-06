package org.ticketing.payment.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.ticketing.payment.domain.model.PaymentLog;
import org.ticketing.payment.domain.repository.PaymentLogRepository;

@Repository
@RequiredArgsConstructor
public class PaymentLogRepositoryImpl implements PaymentLogRepository {
    private final JpaPaymentLogRepository jpaPaymentLogRepository;

    @Override
    public PaymentLog save(PaymentLog paymentLog) {
        return jpaPaymentLogRepository.save(paymentLog);
    }
}