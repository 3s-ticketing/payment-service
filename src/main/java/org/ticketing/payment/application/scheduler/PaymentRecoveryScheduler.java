package org.ticketing.payment.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.application.service.PaymentRecoveryService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    private static final int PAYING_STUCK_MINUTES    = 10;
    private static final int REFUNDING_STUCK_MINUTES = 2;

    private final PaymentRecoveryService recoveryService;

    @Scheduled(fixedDelay = 300_000) // 5분 (test용) 어떻게 해야할지 고민
    public void recoverStuckPaying() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PAYING_STUCK_MINUTES);
        List<PaymentContext> stuck = recoveryService.findStuckPaying(threshold);

        if (stuck.isEmpty()) return;
        stuck.forEach(ctx -> {
            try {
                recoveryService.recoverPaying(ctx);
            } catch (Exception e) {
                log.error("[복구 스케줄러] PAYING 복구 실패. paymentId={}", ctx.paymentId(), e);
            }
        });
    }

    @Scheduled(fixedDelay = 60_000) // 1분 (test용)  어떻게 해야할지 고민
    public void recoverStuckRefunding() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(REFUNDING_STUCK_MINUTES);
        List<PaymentContext> stuck = recoveryService.findStuckRefunding(threshold);

        if (stuck.isEmpty()) return;
        stuck.forEach(ctx -> {
            try {
                recoveryService.recoverRefunding(ctx);
            } catch (Exception e) {
                log.error("[복구 스케줄러] REFUNDING 복구 실패. paymentId={}", ctx.paymentId(), e);
            }
        });
    }
}