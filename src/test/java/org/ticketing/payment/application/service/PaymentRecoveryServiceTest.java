package org.ticketing.payment.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.domain.provider.TossPaymentProvider.StatusResult;
import org.ticketing.payment.domain.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRecoveryService")
class PaymentRecoveryServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentStatusService paymentStatusService;
    @Mock TossPaymentProvider tossPaymentProvider;
    @InjectMocks PaymentRecoveryService paymentRecoveryService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final String PAYMENT_KEY = "toss-key-abc";

    // ─── recoverPaying ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recoverPaying")
    class RecoverPaying {

        @Test
        @DisplayName("Toss DONE → succeedPayment 호출")
        void toss_DONE_호출_succeedPayment() {
            Payment payment = mockPayment(PaymentStatus.PAYING, null);
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", 10_000L));
            lenient().when(paymentStatusService.succeedPayment(any(Payment.class), eq(PAYMENT_KEY)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverPaying(payment);

            verify(paymentStatusService).succeedPayment(any(Payment.class), eq(PAYMENT_KEY));
            verify(paymentStatusService, never()).failPayment(any());
        }

        @Test
        @DisplayName("Toss ABORTED → failPayment 호출")
        void toss_ABORTED_호출_failPayment() {
            Payment payment = mockPayment(PaymentStatus.PAYING, null);
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "ABORTED", 10_000L));
            lenient().when(paymentStatusService.failPayment(any(Payment.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverPaying(payment);

            verify(paymentStatusService).failPayment(any(Payment.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss EXPIRED → failPayment 호출")
        void toss_EXPIRED_호출_failPayment() {
            Payment payment = mockPayment(PaymentStatus.PAYING, null);
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "EXPIRED", 10_000L));
            lenient().when(paymentStatusService.failPayment(any(Payment.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverPaying(payment);

            verify(paymentStatusService).failPayment(any(Payment.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss 조회 예외 → 아무것도 호출 안 함(warn 로그만)")
        void toss_조회_실패_아무것도_안함() {
            Payment payment = mockPayment(PaymentStatus.PAYING, null);
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            paymentRecoveryService.recoverPaying(payment);

            verify(paymentStatusService, never()).succeedPayment(any(), any());
            verify(paymentStatusService, never()).failPayment(any());
        }

        @Test
        @DisplayName("Toss IN_PROGRESS 등 알 수 없는 상태 → 스킵(debug 로그만)")
        void toss_기타_상태_스킵() {
            Payment payment = mockPayment(PaymentStatus.PAYING, null);
            given(tossPaymentProvider.getByOrderId(PAYMENT_ID.toString()))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "IN_PROGRESS", 10_000L));

            paymentRecoveryService.recoverPaying(payment);

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
            Payment payment = mockPayment(PaymentStatus.REFUNDING, LocalDateTime.now().minusMinutes(5));
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "CANCELED", 10_000L));
            lenient().when(paymentStatusService.refundPayment(any(Payment.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverRefunding(payment);

            verify(paymentStatusService).refundPayment(any(Payment.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss DONE + 경과 15분 미만 → cancel 재시도 후 refundPayment 호출")
        void toss_DONE_미만15분_재시도_후_환불() {
            Payment payment = mockPayment(PaymentStatus.REFUNDING, LocalDateTime.now().minusMinutes(5));
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", 10_000L));
            lenient().when(paymentStatusService.refundPayment(any(Payment.class)))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverRefunding(payment);

            verify(tossPaymentProvider).cancel(PAYMENT_KEY, "고객 요청 취소");
            verify(paymentStatusService).refundPayment(any(Payment.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss DONE + 경과 15분 초과 → succeedPayment 호출(환불 포기)")
        void toss_DONE_초과15분_환불포기_succeedPayment() {
            Payment payment = mockPayment(PaymentStatus.REFUNDING, LocalDateTime.now().minusMinutes(20));
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", 10_000L));
            lenient().when(paymentStatusService.succeedPayment(any(Payment.class), isNull()))
                    .thenReturn(mock(PaymentResult.class));

            paymentRecoveryService.recoverRefunding(payment);

            verify(paymentStatusService).succeedPayment(any(Payment.class), isNull());
            verify(paymentStatusService, never()).refundPayment(any());
            verify(tossPaymentProvider, never()).cancel(any(), any());
        }

        @Test
        @DisplayName("Toss DONE + 15분 미만 + cancel 재시도 실패 → 예외 삼킴, refundPayment 미호출")
        void toss_DONE_미만15분_cancel_재시도_실패_삼킴() {
            Payment payment = mockPayment(PaymentStatus.REFUNDING, LocalDateTime.now().minusMinutes(3));
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willReturn(new StatusResult(PAYMENT_KEY, PAYMENT_ID.toString(), "DONE", 10_000L));
            willThrow(new RuntimeException("취소 API 오류"))
                    .given(tossPaymentProvider).cancel(eq(PAYMENT_KEY), any());

            paymentRecoveryService.recoverRefunding(payment);

            verify(paymentStatusService, never()).refundPayment(any());
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        @DisplayName("Toss 조회 예외 → 아무것도 호출 안 함(warn 로그만)")
        void toss_조회_실패_아무것도_안함() {
            Payment payment = mockPayment(PaymentStatus.REFUNDING, LocalDateTime.now().minusMinutes(1));
            given(tossPaymentProvider.getByPaymentKey(PAYMENT_KEY))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            paymentRecoveryService.recoverRefunding(payment);

            verify(paymentStatusService, never()).refundPayment(any());
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /**
     * Payment JPA 엔티티의 modifiedAt, getPaymentKey 등은 런타임 JPA가 주입하므로
     * 단위 테스트에서는 mock으로 필요한 값만 stubbing한다.
     * 테스트마다 호출되는 메서드가 다르므로 lenient()로 불필요한 stubbing 경고를 방지한다.
     */
    private Payment mockPayment(PaymentStatus status, LocalDateTime modifiedAt) {
        Payment payment = mock(Payment.class);
        lenient().when(payment.getId()).thenReturn(PAYMENT_ID);
        lenient().when(payment.getPaymentKey()).thenReturn(PAYMENT_KEY);
        lenient().when(payment.getStatus()).thenReturn(status);
        if (modifiedAt != null) {
            lenient().when(payment.getModifiedAt()).thenReturn(modifiedAt);
        }
        return payment;
    }
}
