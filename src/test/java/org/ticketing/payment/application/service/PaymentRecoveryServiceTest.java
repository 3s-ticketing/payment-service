package org.ticketing.payment.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.domain.provider.TossPaymentProvider.StatusResult;
import org.ticketing.payment.domain.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRecoveryService")
class PaymentRecoveryServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentStatusService paymentStatusService;
    @Mock TossPaymentProvider tossPaymentProvider;
    @InjectMocks PaymentRecoveryService paymentRecoveryService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final long PRICE = 10_000L;
    private static final String PAYMENT_KEY = "toss-key-abc";

    // ─── recoverPaying ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recoverPaying")
    class RecoverPaying {

        @Test
        @DisplayName("Toss DONE → succeedPayment 호출")
        void toss_DONE_호출_succeedPayment() {
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", PRICE));
            lenient().when(paymentStatusService.succeedPayment(any(PaymentContext.class), eq(PAYMENT_KEY)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverPaying(payingCtx());

            verify(paymentStatusService).succeedPayment(any(PaymentContext.class), eq(PAYMENT_KEY));
            verify(paymentStatusService, never()).failPayment(any());
        }

        @Test
        @DisplayName("Toss ABORTED → failPayment 호출")
        void toss_ABORTED_호출_failPayment() {
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "ABORTED", PRICE));
            lenient().when(paymentStatusService.failPayment(any(PaymentContext.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverPaying(payingCtx());

            verify(paymentStatusService).failPayment(any(PaymentContext.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss EXPIRED → failPayment 호출")
        void toss_EXPIRED_호출_failPayment() {
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "EXPIRED", PRICE));
            lenient().when(paymentStatusService.failPayment(any(PaymentContext.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverPaying(payingCtx());

            verify(paymentStatusService).failPayment(any(PaymentContext.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss 조회 예외 → 아무것도 호출 안 함(warn 로그만)")
        void toss_조회_실패_아무것도_안함() {
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            paymentRecoveryService.recoverPaying(payingCtx());

            verify(paymentStatusService, never()).succeedPayment(any(), any());
            verify(paymentStatusService, never()).failPayment(any());
        }

        @Test
        @DisplayName("Toss IN_PROGRESS 등 알 수 없는 상태 → 스킵(debug 로그만)")
        void toss_기타_상태_스킵() {
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "IN_PROGRESS", PRICE));

            paymentRecoveryService.recoverPaying(payingCtx());

            verify(paymentStatusService, never()).succeedPayment(any(), any());
            verify(paymentStatusService, never()).failPayment(any());
        }
    }

    // ─── recoverRefunding ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("recoverRefunding")
    class RecoverRefunding {

        @Test
        @DisplayName("Toss CANCELED → refundPayment 호출")
        void toss_CANCELED_호출_refundPayment() {
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "CANCELED", PRICE));
            lenient().when(paymentStatusService.refundPayment(any(PaymentContext.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverRefunding(refundingCtx(LocalDateTime.now().minusMinutes(5)));

            verify(paymentStatusService).refundPayment(any(PaymentContext.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss DONE + 경과 15분 미만 → cancel 재시도 후 refundPayment 호출")
        void toss_DONE_미만15분_재시도_후_환불() {
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", PRICE));
            lenient().when(paymentStatusService.refundPayment(any(PaymentContext.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverRefunding(refundingCtx(LocalDateTime.now().minusMinutes(5)));

            verify(tossPaymentProvider).cancel(PAYMENT_KEY, "고객 요청 취소");
            verify(paymentStatusService).refundPayment(any(PaymentContext.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss DONE + 경과 15분 초과 → succeedPayment 호출(환불 포기)")
        void toss_DONE_초과15분_환불포기_succeedPayment() {
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", PRICE));
            lenient().when(paymentStatusService.succeedPayment(any(PaymentContext.class), isNull()))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverRefunding(refundingCtx(LocalDateTime.now().minusMinutes(20)));

            verify(paymentStatusService).succeedPayment(any(PaymentContext.class), isNull());
            verify(paymentStatusService, never()).refundPayment(any());
            verify(tossPaymentProvider, never()).cancel(any(), any());
        }

        @Test
        @DisplayName("Toss DONE + 15분 미만 + cancel 재시도 실패 → 예외 삼킴, refundPayment 미호출")
        void toss_DONE_미만15분_cancel_재시도_실패_삼킴() {
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", PRICE));
            willThrow(new RuntimeException("취소 API 오류"))
                    .given(tossPaymentProvider).cancel(eq(PAYMENT_KEY), any());

            paymentRecoveryService.recoverRefunding(refundingCtx(LocalDateTime.now().minusMinutes(3)));

            verify(paymentStatusService, never()).refundPayment(any());
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss 조회 예외 → 아무것도 호출 안 함(warn 로그만)")
        void toss_조회_실패_아무것도_안함() {
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            paymentRecoveryService.recoverRefunding(refundingCtx(LocalDateTime.now().minusMinutes(1)));

            verify(paymentStatusService, never()).refundPayment(any());
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private PaymentContext payingCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, null, PaymentStatus.PAYING, LocalDateTime.now());
    }

    private PaymentContext refundingCtx(LocalDateTime modifiedAt) {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, PAYMENT_KEY, PaymentStatus.REFUNDING, modifiedAt);
    }
}
