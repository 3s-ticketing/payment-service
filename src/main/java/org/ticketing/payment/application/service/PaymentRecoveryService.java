package org.ticketing.payment.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.domain.provider.TossPaymentProvider.StatusResult;
import org.ticketing.payment.domain.repository.PaymentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {

    private static final Duration REFUND_ABANDON_THRESHOLD = Duration.ofMinutes(15);

    private final PaymentRepository paymentRepository;
    private final PaymentStatusService paymentStatusService;
    private final TossPaymentProvider tossPaymentProvider;

    @Transactional
    public List<PaymentContext> findStuckPaying(LocalDateTime before) {
        return paymentRepository.findStuckContexts(PaymentStatus.PAYING, before);
    }

    @Transactional
    public List<PaymentContext> findStuckRefunding(LocalDateTime before) {
        return paymentRepository.findStuckContexts(PaymentStatus.REFUNDING, before);
    }

    public void recoverPaying(PaymentContext ctx) {
        UUID paymentId = ctx.paymentId();
        StatusResult toss;
        try {
            toss = tossPaymentProvider.getByOrderId(paymentId.toString());
        } catch (Exception e) {
            log.warn("[복구 실패] PAYING 상태 Toss 조회 실패. paymentId={}", paymentId, e);
            return;
        }

        switch (toss.status()) {
            case "DONE" -> {
                log.info("[복구] PAYING → SUCCESS. paymentId={}", paymentId);
                paymentStatusService.succeedPayment(ctx, toss.paymentKey());
            }
            case "ABORTED", "EXPIRED" -> {
                log.info("[복구] PAYING → FAIL. paymentId={}, tossStatus={}", paymentId, toss.status());
                paymentStatusService.failPayment(ctx);
            }
            default -> log.debug("[복구 스킵] PAYING 아직 진행 중. paymentId={}, tossStatus={}", paymentId, toss.status());
        }
    }

    public void recoverRefunding(PaymentContext ctx) {
        UUID paymentId = ctx.paymentId();
        StatusResult toss;
        try {
            toss = tossPaymentProvider.getByPaymentKey(ctx.paymentKey());
        } catch (Exception e) {
            log.warn("[복구 실패] REFUNDING 상태 Toss 조회 실패. paymentId={}", paymentId, e);
            return;
        }

        switch (toss.status()) {
            case "CANCELED" -> {
                log.info("[복구] REFUNDING → REFUNDED. paymentId={}", paymentId);
                paymentStatusService.refundPayment(ctx);
            }
            case "DONE" -> {
                Duration stuckFor = Duration.between(ctx.modifiedAt(), LocalDateTime.now());
                if (stuckFor.compareTo(REFUND_ABANDON_THRESHOLD) > 0) {
                    log.error("[복구 포기] REFUNDING 15분 초과 — 환불 없던 일로 처리. paymentId={}, stuckMinutes={}",
                            paymentId, stuckFor.toMinutes());
                    paymentStatusService.succeedPayment(ctx, null);
                    return;
                }
                log.info("[복구] REFUNDING - Toss 미취소 상태, 취소 재시도. paymentId={}", paymentId);
                try {
                    tossPaymentProvider.cancel(ctx.paymentKey(), "고객 요청 취소");
                    paymentStatusService.refundPayment(ctx);
                } catch (Exception e) {
                    log.error("[복구 실패] 취소 재시도 실패. paymentId={}", paymentId, e);
                }
            }
            default -> log.warn("[복구 스킵] REFUNDING 알 수 없는 상태. paymentId={}, tossStatus={}", paymentId, toss.status());
        }
    }
}
