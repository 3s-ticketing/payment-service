package org.ticketing.payment.infrastructure.feign;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.ticketing.payment.infrastructure.feign.dto.InternalReservationResponse;

@FeignClient(name = "reservation-service")
public interface ReservationFeignClient {

    @GetMapping("/internal/reservations/{reservationId}")
    InternalReservationResponse getReservation(@PathVariable UUID reservationId);
}