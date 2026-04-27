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
    public void startPayment(UUID reservationId, Long expectedAmount) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        if (!payment.getTotalPrice().equals(expectedAmount)) {
            throw new PaymentAmountMismatchException(payment.getTotalPrice(), expectedAmount);
        }
        payment.start();
    }

    @Transactional
    public PaymentResult succeedPayment(UUID reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        payment.succeed();
        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult failPayment(UUID reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        payment.fail();
        return PaymentResult.from(payment);
    }
}