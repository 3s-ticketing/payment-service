package org.ticketing.payment.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.toss.TossPaymentClient;
import org.ticketing.payment.infrastructure.toss.dto.TossPaymentStatusResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {

    private static final Duration REFUND_ABANDON_THRESHOLD = Duration.ofMinutes(15);

    private final PaymentRepository paymentRepository;
    private final PaymentStatusService paymentStatusService;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public List<Payment> findStuckPaying(LocalDateTime before) {
        return paymentRepository.findStuckPayments(PaymentStatus.PAYING, before);
    }

    @Transactional
    public List<Payment> findStuckRefunding(LocalDateTime before) {
        return paymentRepository.findStuckPayments(PaymentStatus.REFUNDING, before);
    }

    public void recoverPaying(Payment payment) {
        UUID paymentId = payment.getId();
        TossPaymentStatusResponse toss;
        try {
            toss = tossPaymentClient.getByOrderId(paymentId.toString());
        } catch (Exception e) {
            log.warn("[복구 실패] PAYING 상태 Toss 조회 실패. paymentId={}", paymentId, e);
            return;
        }

        switch (toss.getStatus()) {
            case "DONE" -> {
                // toss O, payment status X
                log.info("[복구] PAYING → SUCCESS. paymentId={}", paymentId);
                paymentStatusService.succeedPayment(paymentId, toss.getPaymentKey());
            }
            case "ABORTED", "EXPIRED" -> {
                // toss X, payment status X
                log.info("[복구] PAYING → FAIL. paymentId={}, tossStatus={}", paymentId, toss.getStatus());
                paymentStatusService.failPayment(paymentId);
            }
            default -> log.debug("[복구 스킵] PAYING 아직 진행 중. paymentId={}, tossStatus={}", paymentId, toss.getStatus());
        }
    }

    public void recoverRefunding(Payment payment) {
        UUID paymentId = payment.getId();
        TossPaymentStatusResponse toss;
        try {
            toss = tossPaymentClient.getByPaymentKey(payment.getPaymentKey());
        } catch (Exception e) {
            log.warn("[복구 실패] REFUNDING 상태 Toss 조회 실패. paymentId={}", paymentId, e);
            return;
        }

        switch (toss.getStatus()) {
            case "CANCELED" -> {
                // toss O, payment status X
                log.info("[복구] REFUNDING → REFUNDED. paymentId={}", paymentId);
                paymentStatusService.refundPayment(paymentId);
            }
            case "DONE" -> {
                // toss X, payment status X
                Duration stuckFor = Duration.between(payment.getModifiedAt(), LocalDateTime.now());
                if (stuckFor.compareTo(REFUND_ABANDON_THRESHOLD) > 0) {
                    log.error("[복구 포기] REFUNDING 15분 초과 — 실패 처리. paymentId={}, stuckMinutes={}", paymentId, stuckFor.toMinutes());
                    paymentStatusService.failPayment(paymentId);
                    return;
                }
                log.info("[복구] REFUNDING - Toss 미취소 상태, 취소 재시도. paymentId={}", paymentId);
                try {
                    tossPaymentClient.cancel(payment.getPaymentKey(), "고객 요청 취소");
                    paymentStatusService.refundPayment(paymentId);
                } catch (Exception e) {
                    log.error("[복구 실패] 취소 재시도 실패. paymentId={}", paymentId, e);
                }
            }
            default -> log.warn("[복구 스킵] REFUNDING 알 수 없는 상태. paymentId={}, tossStatus={}", paymentId, toss.getStatus());
        }
    }
}