package org.ticketing.payment.domain.event;

import java.util.UUID;

public record PaymentFailedEvent(UUID paymentId, UUID orderId) {
}