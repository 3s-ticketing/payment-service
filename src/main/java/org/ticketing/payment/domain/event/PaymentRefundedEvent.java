package org.ticketing.payment.domain.event;

import java.util.UUID;

public record PaymentRefundedEvent(UUID paymentId, UUID orderId) {
}
