package org.ticketing.payment.infrastructure.persistence;

import java.util.List;
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

    @Override
    public void saveAll(List<PaymentLog> logs) {
        jpaPaymentLogRepository.saveAll(logs);
    }
}