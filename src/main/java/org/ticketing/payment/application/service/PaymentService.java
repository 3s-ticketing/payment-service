package org.ticketing.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {
        Payment payment = Payment.create(
                command.getUserId(),
                command.getReservationId(),
                command.getSeatId(),
                command.getTotalPrice()
        );
        return PaymentResult.from(paymentRepository.save(payment));
    }
}