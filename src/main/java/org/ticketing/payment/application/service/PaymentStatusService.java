package org.ticketing.payment.application.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentAmountMismatchException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;
import org.ticketing.payment.domain.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;

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
    public PaymentResult succeedPayment(UUID paymentId, String paymentKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.succeed(paymentKey);
        paymentOutboxRepository.save(PaymentOutbox.createCompleted(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult failPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.fail();
        paymentOutboxRepository.save(PaymentOutbox.createFailed(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }

    @Transactional
    public Payment startRefund(UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        payment.startRefund();
        return payment;
    }

    @Transactional
    public PaymentResult refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.refund();
        paymentOutboxRepository.save(PaymentOutbox.createRefund(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }
}