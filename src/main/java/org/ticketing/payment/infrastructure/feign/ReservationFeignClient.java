package org.ticketing.payment.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.ticketing.payment.domain.client.ReservationClient;

import java.util.UUID;

@FeignClient(name = "reservation-service", path = "/internal/reservations", fallback = ReservationClientFallback.class)
public interface ReservationFeignClient extends ReservationClient {

    @GetMapping("/{reservationId}/detail")
    @Override
    ReservationDetail getReservationDetail(@PathVariable UUID reservationId);
}