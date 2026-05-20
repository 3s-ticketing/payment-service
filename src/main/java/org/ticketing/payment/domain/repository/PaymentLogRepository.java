package org.ticketing.payment.domain.repository;

import java.util.List;
import org.ticketing.payment.domain.model.PaymentLog;

public interface PaymentLogRepository {
    PaymentLog save(PaymentLog paymentLog);
    void saveAll(List<PaymentLog> logs);
}