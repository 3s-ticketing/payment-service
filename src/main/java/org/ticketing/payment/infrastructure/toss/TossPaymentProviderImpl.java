package org.ticketing.payment.infrastructure.toss;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.infrastructure.toss.dto.TossConfirmResponse;
import org.ticketing.payment.infrastructure.toss.dto.TossPaymentStatusResponse;

@Component
@RequiredArgsConstructor
public class TossPaymentProviderImpl implements TossPaymentProvider {

    private final TossPaymentClient tossPaymentClient;

    @Override
    public ConfirmResult confirm(String paymentKey, String orderId, Long amount) {
        TossConfirmResponse response = tossPaymentClient.confirm(paymentKey, orderId, amount);
        return new ConfirmResult(
                response.getPaymentKey(),
                response.getOrderId(),
                response.getStatus(),
                response.getTotalAmount()
        );
    }

    @Override
    public void cancel(String paymentKey, String cancelReason) {
        tossPaymentClient.cancel(paymentKey, cancelReason);
    }

    @Override
    public StatusResult getByOrderId(String orderId) {
        TossPaymentStatusResponse response = tossPaymentClient.getByOrderId(orderId);
        return new StatusResult(
                response.getPaymentKey(),
                response.getOrderId(),
                response.getStatus(),
                response.getTotalAmount()
        );
    }

    @Override
    public StatusResult getByPaymentKey(String paymentKey) {
        TossPaymentStatusResponse response = tossPaymentClient.getByPaymentKey(paymentKey);
        return new StatusResult(
                response.getPaymentKey(),
                response.getOrderId(),
                response.getStatus(),
                response.getTotalAmount()
        );
    }
}