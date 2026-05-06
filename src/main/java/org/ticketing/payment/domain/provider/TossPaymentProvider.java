package org.ticketing.payment.domain.provider;

public interface TossPaymentProvider {

    ConfirmResult confirm(String paymentKey, String orderId, Long amount);

    void cancel(String paymentKey, String cancelReason);

    StatusResult getByOrderId(String orderId);

    StatusResult getByPaymentKey(String paymentKey);

    record ConfirmResult(String paymentKey, String orderId, String status, Long totalAmount) {}

    record StatusResult(String paymentKey, String orderId, String status, Long totalAmount) {}
}