package org.ticketing.payment.infrastructure.toss;

import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.provider.TossPaymentProvider;

@Component
@Profile("loadtest")
public class MockTossPaymentProvider implements TossPaymentProvider {

    @Value("${toss.mock.delay-min-ms:200}")
    private int delayMinMs;

    @Value("${toss.mock.delay-max-ms:500}")
    private int delayMaxMs;

    // 0.0 ~ 1.0: 카드 거절 비율 (e.g. 0.05 = 5%)
    @Value("${toss.mock.failure-rate:0.0}")
    private double failureRate;

    // 0.0 ~ 1.0: 타임아웃 응답 비율 (e.g. 0.01 = 1%)
    @Value("${toss.mock.timeout-rate:0.0}")
    private double timeoutRate;

    @Value("${toss.mock.timeout-ms:3000}")
    private int timeoutMs;

    private final Random random = new Random();

    @Override
    public ConfirmResult confirm(String paymentKey, String orderId, Long amount) {
        simulateDelay();
        simulateFailure();
        return new ConfirmResult(paymentKey, orderId, "DONE", amount);
    }

    @Override
    public void cancel(String paymentKey, String cancelReason) {
        simulateDelay();
    }

    @Override
    public StatusResult getByOrderId(String orderId) {
        return new StatusResult("mock_pk_" + orderId, orderId, "DONE", 0L);
    }

    @Override
    public StatusResult getByPaymentKey(String paymentKey) {
        return new StatusResult(paymentKey, "mock_order", "DONE", 0L);
    }

    private void simulateDelay() {
        int sleepMs = (random.nextDouble() < timeoutRate)
                ? timeoutMs
                : delayMinMs + random.nextInt(Math.max(1, delayMaxMs - delayMinMs));
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateFailure() {
        if (random.nextDouble() < failureRate) {
            throw new PaymentException(PaymentErrorCode.TOSS_CONFIRM_FAILED, "status: 400, body: CARD_DECLINED: 카드 한도 초과 (Mock)");
        }
    }
}