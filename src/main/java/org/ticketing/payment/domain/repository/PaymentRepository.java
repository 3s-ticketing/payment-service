package org.ticketing.payment.domain.repository;

import org.ticketing.payment.domain.model.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}
