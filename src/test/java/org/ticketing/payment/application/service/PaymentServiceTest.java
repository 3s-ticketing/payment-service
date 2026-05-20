package org.ticketing.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.client.ReservationClient;
import org.ticketing.payment.domain.client.ReservationClient.ReservationDetail;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.domain.provider.TossPaymentProvider.ConfirmResult;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.kafka.event.ReservationCanceledEvent.CancelReason;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock TossPaymentProvider tossPaymentProvider;
    @Mock PaymentStatusService paymentStatusService;
    @Mock ReservationClient reservationClient;
    @InjectMocks PaymentService paymentService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final long PRICE = 10_000L;

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        private CreatePaymentCommand command;

        @BeforeEach
        void setUp() {
            command = new CreatePaymentCommand(USER_ID, RESERVATION_ID);
        }

        @Test
        void 정상_생성() {
            given(reservationClient.getReservationDetail(RESERVATION_ID))
                    .willReturn(new ReservationDetail(RESERVATION_ID, USER_ID, PRICE, true));
            given(paymentRepository.save(any(Payment.class)))
                    .willReturn(Payment.create(USER_ID, RESERVATION_ID, PRICE));

            PaymentResult result = paymentService.createPayment(command);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getReservationId()).isEqualTo(RESERVATION_ID);
            assertThat(result.getTotalPrice()).isEqualTo(PRICE);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.INIT);
        }

        @Test
        void 예약_상태_유효하지_않음_INVALID_RESERVATION_STATE() {
            given(reservationClient.getReservationDetail(RESERVATION_ID))
                    .willReturn(new ReservationDetail(RESERVATION_ID, USER_ID, PRICE, false));

            assertThatThrownBy(() -> paymentService.createPayment(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.INVALID_RESERVATION_STATE));

            verify(paymentRepository, never()).save(any());
        }

        @Test
        void 예약_userId_불일치_USER_MISMATCH() {
            UUID otherUser = UUID.randomUUID();
            given(reservationClient.getReservationDetail(RESERVATION_ID))
                    .willReturn(new ReservationDetail(RESERVATION_ID, otherUser, PRICE, true));

            assertThatThrownBy(() -> paymentService.createPayment(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.USER_MISMATCH));

            verify(paymentRepository, never()).save(any());
        }

        @Test
        void 동일_예약에_진행중인_결제_존재시_DUPLICATE_PAYMENT() {
            given(reservationClient.getReservationDetail(RESERVATION_ID))
                    .willReturn(new ReservationDetail(RESERVATION_ID, USER_ID, PRICE, true));
            given(paymentRepository.save(any(Payment.class)))
                    .willThrow(new DataIntegrityViolationException("unique constraint"));

            assertThatThrownBy(() -> paymentService.createPayment(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.DUPLICATE_PAYMENT));
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        private ConfirmPaymentCommand command;

        @BeforeEach
        void setUp() {
            command = new ConfirmPaymentCommand("toss-key", PAYMENT_ID, PRICE);
        }

        @Test
        void 정상_승인() {
            given(paymentStatusService.startPayment(PAYMENT_ID, PRICE)).willReturn(payingCtx());
            given(tossPaymentProvider.confirm("toss-key", PAYMENT_ID.toString(), PRICE))
                    .willReturn(new ConfirmResult("toss-confirmed-key", PAYMENT_ID.toString(), "DONE", PRICE));
            given(paymentStatusService.succeedPayment(any(PaymentContext.class), eq("toss-confirmed-key")))
                    .willReturn(paymentResult(PaymentStatus.SUCCESS));

            PaymentResult result = paymentService.confirmPayment(command);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentStatusService).startPayment(PAYMENT_ID, PRICE);
            verify(tossPaymentProvider).confirm("toss-key", PAYMENT_ID.toString(), PRICE);
            verify(paymentStatusService).succeedPayment(any(PaymentContext.class), eq("toss-confirmed-key"));
        }

        @Test
        void Toss_실패시_failPayment_호출_후_예외_전파() {
            given(paymentStatusService.startPayment(PAYMENT_ID, PRICE)).willReturn(payingCtx());
            given(tossPaymentProvider.confirm(any(), any(), any()))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(RuntimeException.class);

            verify(paymentStatusService).failPayment(any(PaymentContext.class));
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        void Toss_실패시_startPayment는_이미_호출된다() {
            given(paymentStatusService.startPayment(PAYMENT_ID, PRICE)).willReturn(payingCtx());
            given(tossPaymentProvider.confirm(any(), any(), any()))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(RuntimeException.class);

            verify(paymentStatusService).startPayment(PAYMENT_ID, PRICE);
        }

        @Test
        void startPayment_실패시_Toss_미호출() {
            willThrow(new RuntimeException("금액 불일치"))
                    .given(paymentStatusService).startPayment(PAYMENT_ID, PRICE);

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(RuntimeException.class);

            verify(tossPaymentProvider, never()).confirm(any(), any(), any());
            verify(paymentStatusService, never()).failPayment(any());
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        void 결제_없음_PAYMENT_NOT_FOUND() {
            given(paymentStatusService.startPayment(PAYMENT_ID, PRICE))
                    .willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, PAYMENT_ID.toString()));

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        void 존재하는_결제_조회() {
            given(paymentRepository.findById(PAYMENT_ID))
                    .willReturn(Optional.of(Payment.create(USER_ID, RESERVATION_ID, PRICE)));

            PaymentResult result = paymentService.getPayment(PAYMENT_ID);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.INIT);
        }

        @Test
        void 존재하지_않는_결제_조회시_PAYMENT_NOT_FOUND() {
            given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(PAYMENT_ID))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getMyPayment")
    class GetMyPayment {

        @Test
        void 본인_결제_정상_조회() {
            given(paymentRepository.findByIdAndUserId(PAYMENT_ID, USER_ID))
                    .willReturn(Optional.of(Payment.create(USER_ID, RESERVATION_ID, PRICE)));

            PaymentResult result = paymentService.getMyPayment(USER_ID, PAYMENT_ID);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        void 다른_사용자_결제_조회시_PAYMENT_NOT_FOUND() {
            UUID otherUser = UUID.randomUUID();
            given(paymentRepository.findByIdAndUserId(PAYMENT_ID, otherUser)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getMyPayment(otherUser, PAYMENT_ID))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getCode())
                            .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        void 정상_환불() {
            given(paymentStatusService.startRefund(RESERVATION_ID)).willReturn(refundingCtx());
            given(paymentStatusService.refundPayment(any(PaymentContext.class)))
                    .willReturn(paymentResult(PaymentStatus.REFUNDED));

            PaymentResult result = paymentService.refundPayment(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(tossPaymentProvider).cancel("toss-key", "고객 요청 취소");
        }

        @Test
        void Toss_취소_실패시_revertRefund_호출_후_예외_전파() {
            given(paymentStatusService.startRefund(RESERVATION_ID)).willReturn(refundingCtx());
            doThrow(new RuntimeException("Toss 취소 실패"))
                    .when(tossPaymentProvider).cancel(any(), any());

            assertThatThrownBy(() -> paymentService.refundPayment(RESERVATION_ID))
                    .isInstanceOf(RuntimeException.class);

            verify(paymentStatusService).revertRefund(any(PaymentContext.class));
            verify(paymentStatusService, never()).refundPayment(any());
        }
    }

    @Nested
    @DisplayName("handleReservationCanceled")
    class HandleReservationCanceled {

        @Test
        void SUCCESS_결제_없으면_환불_무시() {
            given(paymentRepository.findSuccessContextByReservationId(RESERVATION_ID))
                    .willReturn(Optional.empty());

            paymentService.handleReservationCanceled(RESERVATION_ID, CancelReason.USER_CANCEL);

            verify(paymentStatusService, never()).startRefund(any());
        }

        @Test
        void SUCCESS_결제_있으면_환불_실행() {
            given(paymentRepository.findSuccessContextByReservationId(RESERVATION_ID))
                    .willReturn(Optional.of(successCtx()));
            given(paymentStatusService.startRefund(RESERVATION_ID)).willReturn(refundingCtx());
            given(paymentStatusService.refundPayment(any(PaymentContext.class)))
                    .willReturn(paymentResult(PaymentStatus.REFUNDED));

            paymentService.handleReservationCanceled(RESERVATION_ID, CancelReason.USER_CANCEL);

            verify(tossPaymentProvider).cancel("toss-key", "고객 요청 취소");
            verify(paymentStatusService).refundPayment(any(PaymentContext.class));
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PaymentContext payingCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, null, PaymentStatus.PAYING, LocalDateTime.now());
    }

    private PaymentContext successCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, "toss-key", PaymentStatus.SUCCESS, LocalDateTime.now());
    }

    private PaymentContext refundingCtx() {
        return new PaymentContext(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, "toss-key", PaymentStatus.REFUNDING, LocalDateTime.now());
    }

    private PaymentResult paymentResult(PaymentStatus status) {
        return new PaymentResult(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, status);
    }
}
