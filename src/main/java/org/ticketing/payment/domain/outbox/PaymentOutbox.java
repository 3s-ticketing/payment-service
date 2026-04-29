package org.ticketing.payment.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ticketing.common.domain.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_payment_outbox")
public class PaymentOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    private OutboxStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder
    private PaymentOutbox(String topic, UUID paymentId, UUID orderId) {
        this.topic = topic;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = OutboxStatus.PENDING;
    }

    public static PaymentOutbox createCompleted(UUID paymentId, UUID orderId) {
        return PaymentOutbox.builder()
                .topic("payment.completed")
                .paymentId(paymentId)
                .orderId(orderId)
                .build();
    }

    public static PaymentOutbox createRefund(UUID paymentId, UUID orderId) {
        return PaymentOutbox.builder()
                .topic("payment.refunded")
                .paymentId(paymentId)
                .orderId(orderId)
                .build();
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
}