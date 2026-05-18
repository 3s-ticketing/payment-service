package org.ticketing.payment.infrastructure.toss;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.infrastructure.toss.dto.TossConfirmResponse;
import org.ticketing.payment.infrastructure.toss.dto.TossPaymentStatusResponse;

@Component
@Profile("!loadtest")
@RequiredArgsConstructor
public class TossPaymentProviderImpl implements TossPaymentProvider {

    private static final String CIRCUIT_BREAKER_NAME = "toss";

    private final TossPaymentClient tossPaymentClient;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "confirmFallback")
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
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "cancelFallback")
    public void cancel(String paymentKey, String cancelReason) {
        tossPaymentClient.cancel(paymentKey, cancelReason);
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getByOrderIdFallback")
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
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getByPaymentKeyFallback")
    public StatusResult getByPaymentKey(String paymentKey) {
        TossPaymentStatusResponse response = tossPaymentClient.getByPaymentKey(paymentKey);
        return new StatusResult(
                response.getPaymentKey(),
                response.getOrderId(),
                response.getStatus(),
                response.getTotalAmount()
        );
    }

    private ConfirmResult confirmFallback(String paymentKey, String orderId, Long amount, Throwable t) {
        throw new PaymentException(PaymentErrorCode.TOSS_CONFIRM_FAILED, "PG 일시 장애: " + t.getMessage());
    }

    private void cancelFallback(String paymentKey, String cancelReason, Throwable t) {
        throw new PaymentException(PaymentErrorCode.TOSS_CANCEL_FAILED, "PG 일시 장애: " + t.getMessage());
    }

    private StatusResult getByOrderIdFallback(String orderId, Throwable t) {
        throw new PaymentException(PaymentErrorCode.TOSS_STATUS_FETCH_FAILED, "PG 일시 장애: " + t.getMessage());
    }

    private StatusResult getByPaymentKeyFallback(String paymentKey, Throwable t) {
        throw new PaymentException(PaymentErrorCode.TOSS_STATUS_FETCH_FAILED, "PG 일시 장애: " + t.getMessage());
    }
}