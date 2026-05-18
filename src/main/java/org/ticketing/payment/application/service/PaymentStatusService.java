package org.ticketing.payment.application.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
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
    public Payment startPayment(UUID paymentId, Long expectedAmount) {
        int updated = paymentRepository.tryStartPayment(paymentId, expectedAmount);
        if (updated == 0) {
            Payment p = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, paymentId.toString()));
            if (p.getStatus() == PaymentStatus.SUCCESS) return p;
            if (!p.getTotalPrice().equals(expectedAmount)) {
                throw new PaymentException(PaymentErrorCode.AMOUNT_MISMATCH, "저장된 금액 " + p.getTotalPrice() + ", 요청 금액 " + expectedAmount);
            }
            throw new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, p.getStatus() + " → PAYING 불가");
        }
        paymentLogRepository.save(PaymentLog.create(paymentId, PaymentStatus.INIT, PaymentStatus.PAYING));
        return paymentRepository.findById(paymentId).orElseThrow();
    }

    @Transactional
    public PaymentResult succeedPayment(Payment payment, String paymentKey) {
        PaymentStatus fromStatus = payment.getStatus();
        payment.succeed(paymentKey);
        paymentRepository.save(payment);
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        paymentOutboxRepository.save(PaymentOutbox.createCompleted(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult failPayment(Payment payment) {
        PaymentStatus fromStatus = payment.getStatus();
        payment.fail();
        paymentRepository.save(payment);
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        paymentOutboxRepository.save(PaymentOutbox.createFailed(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }

    @Transactional
    public Payment startRefund(UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationId(reservationId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, "reservationId=" + reservationId));
        PaymentStatus fromStatus = payment.getStatus();
        payment.startRefund();
        paymentRepository.save(payment);
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        return payment;
    }

    @Transactional
    public void revertRefund(Payment payment) {
        PaymentStatus fromStatus = payment.getStatus();
        payment.revertRefund();
        paymentRepository.save(payment);
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
    }

    @Transactional
    public PaymentResult refundPayment(Payment payment) {
        PaymentStatus fromStatus = payment.getStatus();
        payment.refund();
        paymentRepository.save(payment);
        paymentLogRepository.save(PaymentLog.create(payment.getId(), fromStatus, payment.getStatus()));
        paymentOutboxRepository.save(PaymentOutbox.createRefund(payment.getId(), payment.getReservationId()));
        return PaymentResult.from(payment);
    }
}
