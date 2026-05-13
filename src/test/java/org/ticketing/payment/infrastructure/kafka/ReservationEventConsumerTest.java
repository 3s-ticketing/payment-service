package org.ticketing.payment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.infrastructure.kafka.event.ReservationCanceledEvent;
import org.ticketing.payment.infrastructure.kafka.event.ReservationCanceledEvent.CancelReason;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationEventConsumer")
class ReservationEventConsumerTest {

    @Mock PaymentService paymentService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ReservationEventConsumer consumer;

    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("handleReservationCanceled")
    class HandleReservationCanceled {

        @Test
        @DisplayName("정상 JSON → handleReservationCanceled 호출")
        void 정상_JSON_서비스_호출() throws Exception {
            String payload = validPayload();
            ReservationCanceledEvent event = new ReservationCanceledEvent(
                    RESERVATION_ID, USER_ID, CancelReason.USER_CANCEL);
            given(objectMapper.readValue(payload, ReservationCanceledEvent.class)).willReturn(event);

            consumer.handleReservationCanceled(payload);

            verify(paymentService).handleReservationCanceled(RESERVATION_ID, CancelReason.USER_CANCEL);
        }

        @Test
        @DisplayName("잘못된 JSON → 역직렬화 실패, 서비스 호출 없음")
        void 잘못된_JSON_서비스_미호출() throws Exception {
            String payload = "invalid-json";
            given(objectMapper.readValue(payload, ReservationCanceledEvent.class))
                    .willThrow(new JsonProcessingException("parse error") {});

            consumer.handleReservationCanceled(payload);

            verify(paymentService, never()).handleReservationCanceled(any(), any());
        }

        @Test
        @DisplayName("TOSS_CANCEL_FAILED 예외 → 재던지기(Kafka 재시도 트리거)")
        void TOSS_CANCEL_FAILED_재던지기() throws Exception {
            String payload = validPayload();
            ReservationCanceledEvent event = new ReservationCanceledEvent(
                    RESERVATION_ID, USER_ID, CancelReason.USER_CANCEL);
            given(objectMapper.readValue(payload, ReservationCanceledEvent.class)).willReturn(event);
            doThrow(new PaymentException(PaymentErrorCode.TOSS_CANCEL_FAILED, "취소 API 실패"))
                    .when(paymentService).handleReservationCanceled(eq(RESERVATION_ID), any());

            assertThatThrownBy(() -> consumer.handleReservationCanceled(payload))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.TOSS_CANCEL_FAILED));
        }

        @Test
        @DisplayName("일반 PaymentException → 예외 삼킴, 재던지기 없음")
        void 일반_PaymentException_삼킴() throws Exception {
            String payload = validPayload();
            ReservationCanceledEvent event = new ReservationCanceledEvent(
                    RESERVATION_ID, USER_ID, CancelReason.USER_CANCEL);
            given(objectMapper.readValue(payload, ReservationCanceledEvent.class)).willReturn(event);
            doThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, RESERVATION_ID.toString()))
                    .when(paymentService).handleReservationCanceled(eq(RESERVATION_ID), any());

            // 예외가 바깥으로 전파되지 않아야 한다
            consumer.handleReservationCanceled(payload);

            verify(paymentService).handleReservationCanceled(RESERVATION_ID, CancelReason.USER_CANCEL);
        }

        @Test
        @DisplayName("비즈니스 외 일반 예외 → 예외 삼킴, 재던지기 없음")
        void 일반_Exception_삼킴() throws Exception {
            String payload = validPayload();
            ReservationCanceledEvent event = new ReservationCanceledEvent(
                    RESERVATION_ID, USER_ID, CancelReason.USER_CANCEL);
            given(objectMapper.readValue(payload, ReservationCanceledEvent.class)).willReturn(event);
            doThrow(new RuntimeException("DB 연결 실패"))
                    .when(paymentService).handleReservationCanceled(eq(RESERVATION_ID), any());

            consumer.handleReservationCanceled(payload);

            verify(paymentService).handleReservationCanceled(RESERVATION_ID, CancelReason.USER_CANCEL);
        }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private String validPayload() {
        return """
                {"reservationId":"%s","userId":"%s","cancelReason":"USER_CANCEL"}
                """.formatted(RESERVATION_ID, USER_ID);
    }
}
