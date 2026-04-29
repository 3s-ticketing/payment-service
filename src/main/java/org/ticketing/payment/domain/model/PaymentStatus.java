package org.ticketing.payment.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PaymentStatus {
    INIT,
    PAYING,
    SUCCESS,
    FAIL,
    REFUNDING,
    REFUNDED,
    EXPIRED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            INIT,        EnumSet.of(PAYING, REFUNDED, EXPIRED),
            PAYING, EnumSet.of(SUCCESS, FAIL, REFUNDING, EXPIRED),
            SUCCESS,     EnumSet.of(REFUNDED, FAIL, EXPIRED),
            FAIL,      EnumSet.noneOf(PaymentStatus.class),
            REFUNDING, EnumSet.of(REFUNDED, FAIL, EXPIRED),
            REFUNDED,    EnumSet.noneOf(PaymentStatus.class),
            EXPIRED,     EnumSet.noneOf(PaymentStatus.class)
    );

    public boolean canTransitionTo(PaymentStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return this == FAIL ||
                this == REFUNDED ||
                this == EXPIRED;
    }
}