package org.ticketing.payment.application.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentAmountMismatchException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentLog;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;
import org.ticketing.payment.domain.repository.PaymentLogRepository;
import org.ticketing.payment.domain.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLogRepository paymentLogRepository;

    @Transactional
    public void startPayment(UUID paymentId, Long expectedAmount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getTotalPrice().equals(expectedAmount)) {
            throw new PaymentAmountMismatchException(payment.getTotalPrice(), expectedAmount);
        }
        PaymentStatus fromStatus = payment.getStatus();
        payment.start();
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
    }

    @Transactional
    public PaymentResult succeedPayment(UUID paymentId, String paymentKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        PaymentStatus fromStatus = payment.getStatus();
        payment.succeed(paymentKey);
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        paymentOutboxRepository.save(PaymentOutbox.createCompleted(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult failPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        PaymentStatus fromStatus = payment.getStatus();
        payment.fail();
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        paymentOutboxRepository.save(PaymentOutbox.createFailed(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }

    @Transactional
    public Payment startRefund(UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        PaymentStatus fromStatus = payment.getStatus();
        payment.startRefund();
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        return payment;
    }

    @Transactional
    public PaymentResult refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        PaymentStatus fromStatus = payment.getStatus();
        payment.refund();
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        paymentOutboxRepository.save(PaymentOutbox.createRefund(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }
}