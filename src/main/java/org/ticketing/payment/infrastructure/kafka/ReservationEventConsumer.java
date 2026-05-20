package org.ticketing.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.infrastructure.kafka.event.ReservationCanceledEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reservation.canceled", groupId = "payment-service")
    public void handleReservationCanceled(@Payload String payload) {
        log.debug("Received reservation.canceled event. payload={}", payload);

        ReservationCanceledEvent event;
        try {
            event = objectMapper.readValue(payload, ReservationCanceledEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize reservation.canceled event. payload={}", payload, e);
            // Todo: DLT?
            return;
        }

        log.info("Processing reservation.canceled: reservationId={}, cancelReason={}",
                event.reservationId(), event.cancelReason());

        try {
            paymentService.handleReservationCanceled(event.reservationId(), event.cancelReason());
        } catch (PaymentException e) {
            if (e.getCode() == PaymentErrorCode.TOSS_CANCEL_FAILED) {
                log.error("Toss 취소 API 실패, 재시도 대상: reservationId={}", event.reservationId(), e);
                throw e;
            }
            log.error("Failed to handle reservation.canceled: reservationId={}", event.reservationId(), e);
        } catch (Exception e) {
            log.error("Failed to handle reservation.canceled: reservationId={}", event.reservationId(), e);
        }
    }
}