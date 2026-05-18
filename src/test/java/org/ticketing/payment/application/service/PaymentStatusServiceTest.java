package org.ticketing.payment.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentLog;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;
import org.ticketing.payment.domain.repository.PaymentLogRepository;
import org.ticketing.payment.domain.repository.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentStatusService")
class PaymentStatusServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentOutboxRepository paymentOutboxRepository;
    @Mock PaymentLogRepository paymentLogRepository;
    @InjectMocks PaymentStatusService paymentStatusService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final long PRICE = 10_000L;

    @Nested
    @DisplayName("startPayment")
    class StartPayment {

        @Test
        void 정상_전이_INIT_to_PAYING_로그_저장() {
            Payment payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);
            given(paymentLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentStatusService.startPayment(payment, PRICE);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogRepository).save(logCaptor.capture());
            PaymentLog log = logCaptor.getValue();
            assertThat(log.getFromStatus()).isEqualTo(PaymentStatus.INIT);
            assertThat(log.getToStatus()).isEqualTo(PaymentStatus.PAYING);
        }

        @Test
        void 금액_불일치_AMOUNT_MISMATCH() {
            Payment payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);

            assertThatThrownBy(() -> paymentStatusService.startPayment(payment, PRICE + 1))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.AMOUNT_MISMATCH));

            verify(paymentLogRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("succeedPayment")
    class SucceedPayment {

        @Test
        void 정상_전이_PAYING_to_SUCCESS_로그_및_Outbox_저장() {
            Payment payment = payingPayment();
            given(paymentLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(paymentOutboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            PaymentResult result = paymentStatusService.succeedPayment(payment, "toss-key");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogRepository).save(logCaptor.capture());
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
        void 정상_전이_PAYING_to_FAIL_로그_및_Outbox_저장() {
            Payment payment = payingPayment();
            given(paymentLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(paymentOutboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            PaymentResult result = paymentStatusService.failPayment(payment);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAIL);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogRepository).save(logCaptor.capture());
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
        void 정상_전이_SUCCESS_to_REFUNDING_로그_저장() {
            Payment payment = successPayment();
            given(paymentRepository.findSuccessPaymentByReservationId(RESERVATION_ID))
                    .willReturn(Optional.of(payment));
            given(paymentLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Payment result = paymentStatusService.startRefund(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDING);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        void SUCCESS_결제_없음_PAYMENT_NOT_FOUND() {
            given(paymentRepository.findSuccessPaymentByReservationId(RESERVATION_ID))
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
        void 정상_전이_REFUNDING_to_SUCCESS_로그_저장() {
            Payment payment = refundingPayment();
            given(paymentLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentStatusService.revertRefund(payment);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.REFUNDING);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        void 정상_전이_REFUNDING_to_REFUNDED_로그_및_Outbox_저장() {
            Payment payment = refundingPayment();
            given(paymentLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(paymentOutboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            PaymentResult result = paymentStatusService.refundPayment(payment);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            ArgumentCaptor<PaymentLog> logCaptor = ArgumentCaptor.forClass(PaymentLog.class);
            verify(paymentLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(PaymentStatus.REFUNDING);
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(PaymentStatus.REFUNDED);

            ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);
            verify(paymentOutboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("payment.refunded");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Payment payingPayment() {
        Payment p = Payment.create(USER_ID, RESERVATION_ID, PRICE);
        p.start();
        return p;
    }

    private Payment successPayment() {
        Payment p = payingPayment();
        p.succeed("toss-key");
        return p;
    }

    private Payment refundingPayment() {
        Payment p = successPayment();
        p.startRefund();
        return p;
    }
}
