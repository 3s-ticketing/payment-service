package org.ticketing.payment.domain.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}