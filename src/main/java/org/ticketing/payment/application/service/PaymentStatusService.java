package org.ticketing.payment.application.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.model.PaymentLog;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.async.PaymentLogAsyncQueue;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLogAsyncQueue paymentLogQueue;

    /**
     * CAS UPDATE INIT→PAYING (status + totalPrice 동시 업데이트)
     */
    @Transactional
    public PaymentContext startPayment(UUID paymentId, Long expectedAmount) {
        PaymentContext ctx = paymentRepository.tryStartPayment(paymentId, expectedAmount)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, "INIT → PAYING 전이 실패 (상태 불일치 또는 동시 요청 충돌)"));

        paymentLogQueue.enqueue(PaymentLog.create(paymentId, PaymentStatus.INIT, PaymentStatus.PAYING));
        return ctx;
    }

    /**
     * CAS UPDATE PAYING→SUCCESS (paymentKey 포함)
     */
    @Transactional
    public PaymentResult succeedPayment(PaymentContext ctx, String paymentKey) {
        int updated = (paymentKey != null)
                ? paymentRepository.casUpdateStatusWithKey(ctx.paymentId(), ctx.status(), PaymentStatus.SUCCESS, paymentKey)
                : paymentRepository.casUpdateStatus(ctx.paymentId(), ctx.status(), PaymentStatus.SUCCESS);

        if (updated == 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, ctx.status() + " → SUCCESS 불가");
        }

        paymentLogQueue.enqueue(PaymentLog.create(ctx.paymentId(), ctx.status(), PaymentStatus.SUCCESS));
        paymentOutboxRepository.save(PaymentOutbox.createCompleted(ctx.paymentId(), ctx.reservationId()));
        return new PaymentResult(ctx.paymentId(), ctx.userId(), ctx.reservationId(), ctx.totalPrice(), PaymentStatus.SUCCESS);
    }

    @Transactional
    public PaymentResult failPayment(PaymentContext ctx) {
        int updated = paymentRepository.casUpdateStatus(ctx.paymentId(), ctx.status(), PaymentStatus.FAIL);
        if (updated == 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, ctx.status() + " → FAIL 불가");
        }

        paymentLogQueue.enqueue(PaymentLog.create(ctx.paymentId(), ctx.status(), PaymentStatus.FAIL));
        paymentOutboxRepository.save(PaymentOutbox.createFailed(ctx.paymentId(), ctx.reservationId()));
        return new PaymentResult(ctx.paymentId(), ctx.userId(), ctx.reservationId(), ctx.totalPrice(), PaymentStatus.FAIL);
    }

    @Transactional
    public PaymentContext startRefund(UUID reservationId) {
        PaymentContext ctx = paymentRepository.findSuccessContextByReservationId(reservationId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, "reservationId=" + reservationId));

        int updated = paymentRepository.casUpdateStatus(ctx.paymentId(), PaymentStatus.SUCCESS, PaymentStatus.REFUNDING);
        if (updated == 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, ctx.status() + " → REFUNDING 불가");
        }

        paymentLogQueue.enqueue(PaymentLog.create(ctx.paymentId(), PaymentStatus.SUCCESS, PaymentStatus.REFUNDING));
        return ctx.withStatus(PaymentStatus.REFUNDING);
    }

    @Transactional
    public void revertRefund(PaymentContext ctx) {
        int updated = paymentRepository.casUpdateStatus(ctx.paymentId(), PaymentStatus.REFUNDING, PaymentStatus.SUCCESS);
        if (updated == 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, ctx.status() + " → SUCCESS 불가");
        }
        paymentLogQueue.enqueue(PaymentLog.create(ctx.paymentId(), PaymentStatus.REFUNDING, PaymentStatus.SUCCESS));
    }

    @Transactional
    public PaymentResult refundPayment(PaymentContext ctx) {
        int updated = paymentRepository.casUpdateStatus(ctx.paymentId(), PaymentStatus.REFUNDING, PaymentStatus.REFUNDED);
        if (updated == 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION, ctx.status() + " → REFUNDED 불가");
        }

        paymentLogQueue.enqueue(PaymentLog.create(ctx.paymentId(), PaymentStatus.REFUNDING, PaymentStatus.REFUNDED));
        paymentOutboxRepository.save(PaymentOutbox.createRefund(ctx.paymentId(), ctx.reservationId()));
        return new PaymentResult(ctx.paymentId(), ctx.userId(), ctx.reservationId(), ctx.totalPrice(), PaymentStatus.REFUNDED);
    }
}
