package org.ticketing.payment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;
import org.ticketing.common.domain.BaseEntity;
import org.ticketing.payment.domain.exception.InvalidPaymentStatusTransitionException;
import org.ticketing.payment.domain.exception.PaymentAlreadyTerminatedException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    private PaymentStatus status;

    @Builder
    private Payment(UUID userId, UUID reservationId, Long totalPrice) {
        this.userId = userId;
        this.reservationId = reservationId;
        this.totalPrice = totalPrice;
        this.status = PaymentStatus.INIT;
    }

    public static Payment create(UUID userId, UUID reservationId, Long totalPrice) {
        return Payment.builder()
                .userId(userId)
                .reservationId(reservationId)
                .totalPrice(totalPrice)
                .build();
    }

    private void updateStatus(PaymentStatus next) {
        if (this.status.isTerminal()) {
            throw new PaymentAlreadyTerminatedException(this.status);
        }

        if (!this.status.canTransitionTo(next)) {
            throw new InvalidPaymentStatusTransitionException(this.status, next);
        }

        this.status = next;
    }

    public void start() {
        updateStatus(PaymentStatus.PAYING);
    }

    public void succeed(String paymentKey) {
        updateStatus(PaymentStatus.SUCCESS);
        if (paymentKey != null) this.paymentKey = paymentKey;
    }

    public void fail() {
        updateStatus(PaymentStatus.FAIL);
    }

    public void startRefund() {
        updateStatus(PaymentStatus.REFUNDING);
    }

    public void refund() {
        updateStatus(PaymentStatus.REFUNDED);
    }

    public void expire() {
        updateStatus(PaymentStatus.EXPIRED);
    }
}