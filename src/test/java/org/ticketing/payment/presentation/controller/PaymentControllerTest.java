package org.ticketing.payment.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.infrastructure.config.PaymentSecurityConfig;
import org.ticketing.payment.presentation.advice.PaymentControllerAdvice;
import org.ticketing.payment.presentation.dto.request.CreatePaymentRequestDto;
import org.ticketing.payment.presentation.dto.request.PaymentSuccessRequestDto;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PaymentController.class, PaymentInternalController.class})
@Import({PaymentSecurityConfig.class, PaymentControllerAdvice.class})
@DisplayName("PaymentController 슬라이스 테스트")
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PaymentService paymentService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final long PRICE = 10_000L;

    // ─── POST /api/payments ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/payments — 결제 생성")
    class CreatePayment {

        @Test
        @DisplayName("정상 요청 → 200")
        void 정상_요청() throws Exception {
            given(paymentService.createPayment(any())).willReturn(paymentResult(PaymentStatus.INIT));

            mockMvc.perform(post("/api/payments")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePaymentRequestDto(RESERVATION_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INIT"));
        }

        @Test
        @DisplayName("reservationId 누락 → 400")
        void reservationId_누락() throws Exception {
            mockMvc.perform(post("/api/payments")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("X-User-Id 헤더 누락 → 400")
        void userId_헤더_누락() throws Exception {
            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePaymentRequestDto(RESERVATION_ID))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DUPLICATE_PAYMENT → 409")
        void 중복_결제_409() throws Exception {
            given(paymentService.createPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT));

            mockMvc.perform(post("/api/payments")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePaymentRequestDto(RESERVATION_ID))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_PAYMENT"));
        }

        @Test
        @DisplayName("INVALID_RESERVATION_STATE → 409")
        void 예약_상태_유효하지_않음_409() throws Exception {
            given(paymentService.createPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.INVALID_RESERVATION_STATE));

            mockMvc.perform(post("/api/payments")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePaymentRequestDto(RESERVATION_ID))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
        }

        @Test
        @DisplayName("낙관적 락 충돌 → 409")
        void 낙관적_락_충돌_409() throws Exception {
            given(paymentService.createPayment(any()))
                    .willThrow(new OptimisticLockingFailureException("version conflict"));

            mockMvc.perform(post("/api/payments")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePaymentRequestDto(RESERVATION_ID))))
                    .andExpect(status().isConflict());
        }
    }

    // ─── POST /api/payments/success ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/payments/success — 결제 승인")
    class ConfirmPayment {

        @Test
        @DisplayName("정상 요청 → 200")
        void 정상_요청() throws Exception {
            given(paymentService.confirmPayment(any())).willReturn(paymentResult(PaymentStatus.SUCCESS));

            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PaymentSuccessRequestDto("toss-key", PAYMENT_ID, PRICE))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("paymentKey 누락 → 400")
        void paymentKey_누락() throws Exception {
            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"paymentId\":\"" + PAYMENT_ID + "\",\"totalPrice\":10000}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("totalPrice 음수 → 400")
        void totalPrice_음수() throws Exception {
            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PaymentSuccessRequestDto("toss-key", PAYMENT_ID, -1L))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PAYMENT_NOT_FOUND → 404")
        void 결제_없음_404() throws Exception {
            given(paymentService.confirmPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PaymentSuccessRequestDto("toss-key", PAYMENT_ID, PRICE))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Toss PG 승인 실패(TOSS_CONFIRM_FAILED) → 502")
        void toss_승인_실패_502() throws Exception {
            given(paymentService.confirmPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.TOSS_CONFIRM_FAILED, "PG 승인 거절"));

            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PaymentSuccessRequestDto("toss-key", PAYMENT_ID, PRICE))))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("TOSS_CONFIRM_FAILED"));
        }

        @Test
        @DisplayName("결제 금액 불일치(AMOUNT_MISMATCH) → 400")
        void 금액_불일치_400() throws Exception {
            given(paymentService.confirmPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.AMOUNT_MISMATCH, "금액 불일치"));

            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PaymentSuccessRequestDto("toss-key", PAYMENT_ID, PRICE))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
        }

        @Test
        @DisplayName("이미 종료된 결제 재승인(ALREADY_TERMINATED) → 409")
        void 이미_종료된_결제_409() throws Exception {
            given(paymentService.confirmPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.ALREADY_TERMINATED, "FAIL"));

            mockMvc.perform(post("/api/payments/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PaymentSuccessRequestDto("toss-key", PAYMENT_ID, PRICE))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ALREADY_TERMINATED"));
        }
    }

    // ─── GET /api/payments/my/{paymentId} ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/payments/my/{paymentId} — 내 결제 단건 조회")
    class GetMyPayment {

        @Test
        @DisplayName("정상 조회 → 200")
        void 정상_조회() throws Exception {
            given(paymentService.getMyPayment(USER_ID, PAYMENT_ID))
                    .willReturn(paymentResult(PaymentStatus.SUCCESS));

            mockMvc.perform(get("/api/payments/my/{paymentId}", PAYMENT_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("존재하지 않는 paymentId → 404")
        void 존재하지_않는_paymentId() throws Exception {
            given(paymentService.getMyPayment(any(), any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            mockMvc.perform(get("/api/payments/my/{paymentId}", PAYMENT_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }
    }

    // ─── GET /api/payments/{paymentId} ───────────────────────────────────────

    @Nested
    @DisplayName("GET /api/payments/{paymentId} — 결제 단건 조회 [Admin]")
    class GetPayment {

        @Test
        @DisplayName("정상 조회 → 200")
        void 정상_조회() throws Exception {
            given(paymentService.getPayment(PAYMENT_ID))
                    .willReturn(paymentResult(PaymentStatus.SUCCESS));

            mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()));
        }

        @Test
        @DisplayName("존재하지 않는 paymentId → 404")
        void 존재하지_않는_paymentId() throws Exception {
            given(paymentService.getPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }
    }

    // ─── POST /api/payments/refund/reservations/{reservationId} ──────────────

    @Nested
    @DisplayName("POST /api/payments/refund/reservations/{reservationId} — 환불")
    class RefundPayment {

        @Test
        @DisplayName("정상 환불 → 200")
        void 정상_환불() throws Exception {
            given(paymentService.refundPayment(RESERVATION_ID))
                    .willReturn(paymentResult(PaymentStatus.REFUNDED));

            mockMvc.perform(post("/api/payments/refund/reservations/{reservationId}", RESERVATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }

        @Test
        @DisplayName("PAYMENT_NOT_FOUND → 404")
        void 결제_없음_404() throws Exception {
            given(paymentService.refundPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            mockMvc.perform(post("/api/payments/refund/reservations/{reservationId}", RESERVATION_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── GET /internal/payments/{paymentId} ──────────────────────────────────

    @Nested
    @DisplayName("GET /internal/payments/{paymentId} — 내부 단건 조회")
    class InternalGetPayment {

        @Test
        @DisplayName("정상 조회 → 200")
        void 정상_조회() throws Exception {
            given(paymentService.getPayment(PAYMENT_ID))
                    .willReturn(paymentResult(PaymentStatus.SUCCESS));

            mockMvc.perform(get("/internal/payments/{paymentId}", PAYMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()));
        }

        @Test
        @DisplayName("PAYMENT_NOT_FOUND → 404")
        void 존재하지_않는_paymentId() throws Exception {
            given(paymentService.getPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            mockMvc.perform(get("/internal/payments/{paymentId}", PAYMENT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private PaymentResult paymentResult(PaymentStatus status) {
        return new PaymentResult(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, status);
    }
}
