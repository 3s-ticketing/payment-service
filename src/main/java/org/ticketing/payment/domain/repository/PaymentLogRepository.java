package org.ticketing.payment.domain.repository;

import org.ticketing.payment.domain.model.PaymentLog;

public interface PaymentLogRepository {
    PaymentLog save(PaymentLog paymentLog);
}