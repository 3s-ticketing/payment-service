package org.ticketing.payment.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.DuplicatePaymentException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.toss.TossPaymentClient;
import org.ticketing.payment.infrastructure.toss.dto.TossConfirmResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock TossPaymentClient tossPaymentClient;
    @Mock PaymentStatusService paymentStatusService;
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
            command = new CreatePaymentCommand(USER_ID, RESERVATION_ID, PRICE);
        }

        @Test
        void 정상_생성() {
            Payment payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);
            given(paymentRepository.existsActivePayment(RESERVATION_ID)).willReturn(false);
            given(paymentRepository.save(any(Payment.class))).willReturn(payment);

            PaymentResult result = paymentService.createPayment(command);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getReservationId()).isEqualTo(RESERVATION_ID);
            assertThat(result.getTotalPrice()).isEqualTo(PRICE);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.INIT);
        }

        @Test
        void 결제_정보와_불일치시() {

        }

        @Test
        void 동일_예약에_진행중인_결제_존재시_DuplicatePaymentException() {
            given(paymentRepository.existsActivePayment(RESERVATION_ID)).willReturn(true);

            assertThatThrownBy(() -> paymentService.createPayment(command))
                    .isInstanceOf(DuplicatePaymentException.class);

            verify(paymentRepository, never()).save(any());
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
            TossConfirmResponse tossResponse = tossConfirmResponse("toss-confirmed-key");
            given(tossPaymentClient.confirm("toss-key", PAYMENT_ID.toString(), PRICE))
                    .willReturn(tossResponse);

            PaymentResult successResult = paymentResult(PaymentStatus.SUCCESS);
            given(paymentStatusService.succeedPayment(PAYMENT_ID, "toss-confirmed-key"))
                    .willReturn(successResult);

            PaymentResult result = paymentService.confirmPayment(command);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentStatusService).startPayment(PAYMENT_ID, PRICE);
            verify(tossPaymentClient).confirm("toss-key", PAYMENT_ID.toString(), PRICE);
            verify(paymentStatusService).succeedPayment(PAYMENT_ID, "toss-confirmed-key");
        }

        @Test
        void Toss_실패시_failPayment_호출_후_예외_전파() {
            given(tossPaymentClient.confirm(any(), any(), any()))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(RuntimeException.class);

            verify(paymentStatusService).failPayment(PAYMENT_ID);
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }

        @Test
        void Toss_실패시_startPayment는_이미_호출된다() {
            given(tossPaymentClient.confirm(any(), any(), any()))
                    .willThrow(new RuntimeException("Toss 통신 오류"));

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(RuntimeException.class);

            // INIT → PAYING 전이는 Toss 호출 전에 완료됨
            verify(paymentStatusService).startPayment(PAYMENT_ID, PRICE);
        }

        @Test
        void startPayment_실패시_Toss_미호출() {
            willThrow(new RuntimeException("금액 불일치"))
                    .given(paymentStatusService).startPayment(PAYMENT_ID, PRICE);

            assertThatThrownBy(() -> paymentService.confirmPayment(command))
                    .isInstanceOf(RuntimeException.class);

            verify(tossPaymentClient, never()).confirm(any(), any(), any());
            verify(paymentStatusService, never()).failPayment(any());
            verify(paymentStatusService, never()).succeedPayment(any(), any());
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        void 존재하는_결제_조회() {
            Payment payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);
            given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

            PaymentResult result = paymentService.getPayment(PAYMENT_ID);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.INIT);
        }

        @Test
        void 존재하지_않는_결제_조회시_PaymentNotFoundException() {
            given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(PAYMENT_ID))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        void 정상_환불() {
            Payment payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);
            payment.start();
            payment.succeed("toss-key");
            given(paymentStatusService.startRefund(RESERVATION_ID)).willReturn(payment);

            PaymentResult refundedResult = paymentResult(PaymentStatus.REFUNDED);
            given(paymentStatusService.refundPayment(payment.getId())).willReturn(refundedResult);

            PaymentResult result = paymentService.refundPayment(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(tossPaymentClient).cancel("toss-key", "고객 요청 취소");
        }

        @Test
        void Toss_취소_실패시_refundPayment_미호출() {
            Payment payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);
            payment.start();
            payment.succeed("toss-key");
            given(paymentStatusService.startRefund(RESERVATION_ID)).willReturn(payment);
            given(tossPaymentClient.cancel(any(), any())).willThrow(new RuntimeException("Toss 취소 실패"));

            assertThatThrownBy(() -> paymentService.refundPayment(RESERVATION_ID))
                    .isInstanceOf(RuntimeException.class);

            verify(paymentStatusService, never()).refundPayment(any());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TossConfirmResponse tossConfirmResponse(String paymentKey) {
        TossConfirmResponse response = mock(TossConfirmResponse.class);
        given(response.getPaymentKey()).willReturn(paymentKey);
        return response;
    }

    private PaymentResult paymentResult(PaymentStatus status) {
        return new PaymentResult(PAYMENT_ID, USER_ID, RESERVATION_ID, PRICE, status);
    }
}