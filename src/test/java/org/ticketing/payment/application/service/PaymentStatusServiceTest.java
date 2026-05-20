package org.ticketing.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.model.PaymentLog;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.async.PaymentLogAsyncQueue;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentStatusService")
class PaymentStatusServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentOutboxRepository paymentOutboxRepository;
    @Mock PaymentLogAsyncQueue paymentLogQueue;
    @InjectMocks PaymentStatusService paymentStatusService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final long PRICE = 10_000L;

    @Nested
    @DisplayName("startPayment")
    class StartPayment {

        @Test
        void 정상_전이_INIT_to_PAYING_로그_큐_적재() {
            given(paymentRepository.tryStartPayment(PAYMENT_ID, PRICE)).willReturn(Optional.of(payingCtx()));

            PaymentContext result = paymentStatusService.startPayment(PAYMENT_ID, PRICE);

            assertThat(result.status()).isEqualTo(PaymentStatus.PAYING);
            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogQueue).enqueue(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.INIT);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.PAYING);
        }

        @Test
        void CAS_실패_INVALID_STATUS_TRANSITION() {
            given(paymentRepository.tryStartPayment(PAYMENT_ID, PRICE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentStatusService.startPayment(PAYMENT_ID, PRICE))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.INVALID_STATUS_TRANSITION));

            verify(paymentLogQueue, never()).enqueue(any());
        }
    }

    @Nested
    @DisplayName("succeedPayment")
    class SucceedPayment {

        @Test
        void 정상_전이_PAYING_to_SUCCESS_로그_큐_적재_Outbox_저장() {
            given(paymentRepository.casUpdateStatusWithKey(PAYMENT_ID, PaymentStatus.PAYING, PaymentStatus.SUCCESS, "toss-key")).willReturn(1);
            given(paymentOutboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            PaymentResult result = paymentStatusService.succeedPayment(payingCtx(), "toss-key");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogQueue).enqueue(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.PAYING);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.SUCCESS);

            ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);
            verify(paymentOutboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("payment.completed");
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPayment {

        @Test
        void 정상_전이_PAYING_to_FAIL_로그_큐_적재_Outbox_저장() {
            given(paymentRepository.casUpdateStatus(PAYMENT_ID, PaymentStatus.PAYING, PaymentStatus.FAIL)).willReturn(1);
            given(paymentOutboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            PaymentResult result = paymentStatusService.failPayment(payingCtx());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAIL);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogQueue).enqueue(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.PAYING);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.FAIL);

            ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);
            verify(paymentOutboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("payment.failed");
        }
    }

    @Nested
    @DisplayName("startRefund")
    class StartRefund {

        @Test
        void 정상_전이_SUCCESS_to_REFUNDING_로그_큐_적재() {
            given(paymentRepository.findSuccessContextByReservationId(RESERVATION_ID))
                    .willReturn(Optional.of(successCtx()));
            given(paymentRepository.casUpdateStatus(PAYMENT_ID, PaymentStatus.SUCCESS, PaymentStatus.REFUNDING)).willReturn(1);

            PaymentContext result = paymentStatusService.startRefund(RESERVATION_ID);

            assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDING);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogQueue).enqueue(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        void SUCCESS_결제_없음_PAYMENT_NOT_FOUND() {
            given(paymentRepository.findSuccessContextByReservationId(RESERVATION_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentStatusService.startRefund(RESERVATION_ID))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("revertRefund")
    class RevertRefund {

        @Test
        void 정상_전이_REFUNDING_to_SUCCESS_로그_큐_적재() {
            given(paymentRepository.casUpdateStatus(PAYMENT_ID, PaymentStatus.REFUNDING, PaymentStatus.SUCCESS)).willReturn(1);

            paymentStatusService.revertRefund(refundingCtx());

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogQueue).enqueue(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.REFUNDING);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        void 정상_전이_REFUNDING_to_REFUNDED_로그_큐_적재_Outbox_저장() {
            given(paymentRepository.casUpdateStatus(PAYMENT_ID, PaymentStatus.REFUNDING, PaymentStatus.REFUNDED)).willReturn(1);
            given(paymentOutboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            PaymentResult result = paymentStatusService.refundPayment(refundingCtx());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogQueue).enqueue(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.REFUNDING);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.REFUNDED);

            ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);
            verify(paymentOutboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("payment.refunded");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PaymentContext initCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, null, PaymentStatus.INIT, LocalDateTime.now());
    }

    private PaymentContext payingCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, null, PaymentStatus.PAYING, LocalDateTime.now());
    }

    private PaymentContext successCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, "toss-key", PaymentStatus.SUCCESS, LocalDateTime.now());
    }

    private PaymentContext refundingCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, "toss-key", PaymentStatus.REFUNDING, LocalDateTime.now());
    }
}
