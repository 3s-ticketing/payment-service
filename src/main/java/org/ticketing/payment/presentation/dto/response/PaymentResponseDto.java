package org.ticketing.payment.presentation.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ticketing.payment.application.dto.result.PaymentResult;

@Getter
@AllArgsConstructor
public class PaymentResponseDto {

    private UUID id;
    private UUID userId;
    private UUID reservationId;
    private UUID seatId;
    private Long totalPrice;
    private String status;

    public static PaymentResponseDto from(PaymentResult result) {
        return new PaymentResponseDto(
                result.getId(),
                result.getUserId(),
                result.getReservationId(),
                result.getSeatId(),
                result.getTotalPrice(),
                result.getStatus().name()
        );
    }
}