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

    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID seatId;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    private PaymentStatus status;

    @Builder
    private Payment(UUID userId, UUID reservationId, UUID seatId, Long totalPrice) {
        this.userId = userId;
        this.reservationId = reservationId;
        this.seatId = seatId;
        this.totalPrice = totalPrice;
        this.status = PaymentStatus.READY;
    }

    public static Payment create(UUID userId, UUID reservationId, UUID seatId, Long totalPrice) {
        return Payment.builder()
                .userId(userId)
                .reservationId(reservationId)
                .seatId(seatId)
                .totalPrice(totalPrice)
                .build();
    }

    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }

    /*
    @Override
    public void delete(String deletedBy) {
        super.delete(deletedBy);
    }
     */
}