package org.ticketing.payment.infrastructure.toss;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.provider.TossPaymentProvider;

@Component
@Profile("loadtest")
public class MockTossPaymentProvider implements TossPaymentProvider {

    @Override
    public ConfirmResult confirm(String paymentKey, String orderId, Long amount) {
        // Toss API 호출 없이 바로 성공 응답 반환
        return new ConfirmResult(paymentKey, orderId, "DONE", amount);
    }

    @Override
    public void cancel(String paymentKey, String cancelReason) {
        // no operation
    }

    @Override
    public StatusResult getByOrderId(String orderId) {
        return new StatusResult("mock_pk_" + orderId, orderId, "DONE", 0L);
    }

    @Override
    public StatusResult getByPaymentKey(String paymentKey) {
        return new StatusResult(paymentKey, "mock_order", "DONE", 0L);
    }
}