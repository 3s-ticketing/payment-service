package org.ticketing.payment.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ticketing.payment.domain.exception.InvalidPaymentStatusTransitionException;
import org.ticketing.payment.domain.exception.PaymentAlreadyTerminatedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 도메인")
class PaymentTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final long PRICE = 10_000L;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.create(USER_ID, RESERVATION_ID, PRICE);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void 초기_상태는_INIT() {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INIT);
        }

        @Test
        void userId_reservationId_totalPrice_저장() {
            assertThat(payment.getUserId()).isEqualTo(USER_ID);
            assertThat(payment.getReservationId()).isEqualTo(RESERVATION_ID);
            assertThat(payment.getTotalPrice()).isEqualTo(PRICE);
        }

        @Test
        void paymentKey는_null로_초기화() {
            assertThat(payment.getPaymentKey()).isNull();
        }
    }

    @Nested
    @DisplayName("start — INIT → PAYING")
    class Start {

        @Test
        void 상태가_PAYING으로_변경된다() {
            payment.start();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYING);
        }

        @Test
        void PAYING_상태에서_재호출시_InvalidPaymentStatusTransitionException() {
            payment.start();
            assertThatThrownBy(payment::start)
                    .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("succeed — PAYING → SUCCESS")
    class Succeed {

        @Test
        void 상태가_SUCCESS로_변경되고_paymentKey_저장() {
            payment.start();
            payment.succeed("toss-key-abc");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getPaymentKey()).isEqualTo("toss-key-abc");
        }

        @Test
        void INIT_상태에서_직접_succeed_호출시_InvalidPaymentStatusTransitionException() {
            assertThatThrownBy(() -> payment.succeed("key"))
                    .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        }

        @Test
        void 이미_SUCCESS인_상태에서_succeed_호출시_PaymentAlreadyTerminatedException은_아니다() {
            payment.start();
            payment.succeed("key");

            assertThatThrownBy(() -> payment.succeed("key2"))
                    .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("fail — PAYING → FAIL")
    class Fail {

        @Test
        void 상태가_FAIL로_변경된다() {
            payment.start();
            payment.fail();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAIL);
        }

        @Test
        void INIT에서_fail_호출시_InvalidPaymentStatusTransitionException() {
            assertThatThrownBy(payment::fail)
                    .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        }

        @Test
        void FAIL_이후_모든_전이_시도시_PaymentAlreadyTerminatedException() {
            payment.start();
            payment.fail();

            assertThatThrownBy(payment::start)
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
            assertThatThrownBy(() -> payment.succeed("key"))
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
            assertThatThrownBy(payment::fail)
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
        }
    }

    @Nested
    @DisplayName("startRefund — SUCCESS → REFUNDING")
    class StartRefund {

        @Test
        void 상태가_REFUNDING으로_변경된다() {
            payment.start();
            payment.succeed("key");
            payment.startRefund();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        void INIT에서_startRefund_호출시_InvalidPaymentStatusTransitionException() {
            assertThatThrownBy(payment::startRefund)
                    .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("refund — REFUNDING → REFUNDED")
    class Refund {

        @Test
        void 상태가_REFUNDED로_변경된다() {
            payment.start();
            payment.succeed("key");
            payment.startRefund();
            payment.refund();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        void REFUNDED_이후_모든_전이_시도시_PaymentAlreadyTerminatedException() {
            payment.start();
            payment.succeed("key");
            payment.startRefund();
            payment.refund();

            assertThatThrownBy(payment::refund)
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
            assertThatThrownBy(payment::startRefund)
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
        }

        @Test
        void INIT에서_refund_직접_호출시_InvalidPaymentStatusTransitionException() {
            assertThatThrownBy(payment::refund)
                    .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("expire")
    class Expire {

        @Test
        void INIT에서_EXPIRED로_전이된다() {
            payment.expire();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        }

        @Test
        void PAYING에서_EXPIRED로_전이된다() {
            payment.start();
            payment.expire();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        }

        @Test
        void EXPIRED_이후_모든_전이_시도시_PaymentAlreadyTerminatedException() {
            payment.expire();

            assertThatThrownBy(payment::start)
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
            assertThatThrownBy(() -> payment.succeed("key"))
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
            assertThatThrownBy(payment::expire)
                    .isInstanceOf(PaymentAlreadyTerminatedException.class);
        }
    }

    @Nested
    @DisplayName("정상 결제 완료 시나리오 — INIT → PAYING → SUCCESS")
    class HappyPath {

        @Test
        void 전체_결제_성공_흐름() {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INIT);

            payment.start();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYING);

            payment.succeed("toss-confirmed-key");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getPaymentKey()).isEqualTo("toss-confirmed-key");
        }
    }

    @Nested
    @DisplayName("환불 시나리오 — SUCCESS → REFUNDING → REFUNDED")
    class RefundScenario {

        @Test
        void 전체_환불_흐름() {
            payment.start();
            payment.succeed("key");
            payment.startRefund();
            payment.refund();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }

    @Nested
    @DisplayName("결제 실패 시나리오 — INIT → PAYING → FAIL")
    class FailScenario {

        @Test
        void PG_실패로_인한_결제_실패_흐름() {
            payment.start();
            payment.fail();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAIL);
            assertThat(payment.getPaymentKey()).isNull();
        }
    }
}