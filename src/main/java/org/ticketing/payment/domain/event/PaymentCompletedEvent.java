package org.ticketing.payment.domain.event;

import java.util.UUID;

public record PaymentCompletedEvent(UUID paymentId, UUID orderId) {
}