package org.ticketing.payment.infrastructure.feign;

import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.exception.ReservationNotFoundException;
import org.ticketing.payment.domain.provider.ReservationProvider;
import org.ticketing.payment.infrastructure.feign.dto.InternalReservationResponse;

@Component
@RequiredArgsConstructor
public class ReservationProviderImpl implements ReservationProvider {

    private final ReservationFeignClient reservationFeignClient;

    /**
     * @throws ReservationNotFoundException 예약이 존재하지 않는 경우 (404)
     * @throws RuntimeException             그 외 인프라 장애
     */
    @Override
    public ReservationSnapshot getReservation(UUID reservationId) {
        try {
            InternalReservationResponse response = reservationFeignClient.getReservation(reservationId);
            return new ReservationSnapshot(
                    response.getReservationId(),
                    response.getUserId(),
                    response.getTotalPrice()
            );
        } catch (FeignException.NotFound e) {
            throw new ReservationNotFoundException(reservationId);
        } catch (FeignException e) {
            throw new RuntimeException("Reservation 서비스 호출 실패: " + e.status(), e);
        }
    }
}