package org.ticketing.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentAmountMismatchException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void startPayment(UUID paymentId, Long expectedAmount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getTotalPrice().equals(expectedAmount)) {
            throw new PaymentAmountMismatchException(payment.getTotalPrice(), expectedAmount);
        }
        payment.start();
    }

    @Transactional
    public PaymentResult succeedPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.succeed();
        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult failPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.fail();
        return PaymentResult.from(payment);
    }
}