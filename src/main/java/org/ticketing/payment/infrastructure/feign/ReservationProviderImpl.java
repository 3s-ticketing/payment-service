package org.ticketing.payment.infrastructure.feign;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.provider.ReservationProvider;
import org.ticketing.payment.infrastructure.feign.dto.InternalReservationResponse;

@Component
@RequiredArgsConstructor
public class ReservationProviderImpl implements ReservationProvider {

    private final ReservationFeignClient reservationFeignClient;

    @Override
    public ReservationSnapshot getReservation(UUID reservationId) {
        InternalReservationResponse response = reservationFeignClient.getReservation(reservationId);
        return new ReservationSnapshot(
                response.getReservationId(),
                response.getUserId(),
                response.getTotalPrice()
        );
    }
}