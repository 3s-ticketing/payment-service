package org.ticketing.payment.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PaymentStatus {
    INIT,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    CANCELED,
    EXPIRED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            INIT,        EnumSet.of(IN_PROGRESS, CANCELED, EXPIRED),
            IN_PROGRESS, EnumSet.of(SUCCESS, FAILED, CANCELED, EXPIRED),
            SUCCESS,     EnumSet.noneOf(PaymentStatus.class),
            FAILED,      EnumSet.noneOf(PaymentStatus.class),
            CANCELED,    EnumSet.noneOf(PaymentStatus.class),
            EXPIRED,     EnumSet.noneOf(PaymentStatus.class)
    );

    public boolean canTransitionTo(PaymentStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return this == SUCCESS ||
                this == FAILED ||
                this == CANCELED ||
                this == EXPIRED;
    }
}