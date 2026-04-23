package org.ticketing.payment.domain.model;

public enum PaymentStatus {
    READY,
    IN_PROGRESS,
    APPROVED,
    COMPLETED,
    FAILED,
    CANCELED,
    EXPIRED
}