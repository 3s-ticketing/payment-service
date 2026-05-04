package org.ticketing.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentStatus 상태 머신")
class PaymentStatusTest {

    @Nested
    @DisplayName("허용된 전이")
    class AllowedTransitions {

        @Test
        void INIT에서_PAYING으로_전이_가능() {
            assertThat(PaymentStatus.INIT.canTransitionTo(PaymentStatus.PAYING)).isTrue();
        }

        @Test
        void INIT에서_EXPIRED로_전이_가능() {
            assertThat(PaymentStatus.INIT.canTransitionTo(PaymentStatus.EXPIRED)).isTrue();
        }

        @Test
        void PAYING에서_SUCCESS로_전이_가능() {
            assertThat(PaymentStatus.PAYING.canTransitionTo(PaymentStatus.SUCCESS)).isTrue();
        }

        @Test
        void PAYING에서_FAIL로_전이_가능() {
            assertThat(PaymentStatus.PAYING.canTransitionTo(PaymentStatus.FAIL)).isTrue();
        }

        @Test
        void PAYING에서_EXPIRED로_전이_가능() {
            assertThat(PaymentStatus.PAYING.canTransitionTo(PaymentStatus.EXPIRED)).isTrue();
        }

        @Test
        void SUCCESS에서_REFUNDING으로_전이_가능() {
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.REFUNDING)).isTrue();
        }

        @Test
        void SUCCESS에서_FAIL로_전이_가능() {
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.FAIL)).isTrue();
        }

        @Test
        void SUCCESS에서_EXPIRED로_전이_가능() {
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.EXPIRED)).isTrue();
        }

        @Test
        void REFUNDING에서_REFUNDED로_전이_가능() {
            assertThat(PaymentStatus.REFUNDING.canTransitionTo(PaymentStatus.REFUNDED)).isTrue();
        }

        @Test
        void REFUNDING에서_SUCCESS로_전이_가능() {
            assertThat(PaymentStatus.REFUNDING.canTransitionTo(PaymentStatus.SUCCESS)).isTrue();
        }

        @Test
        void REFUNDING에서_FAIL로_전이_가능() {
            assertThat(PaymentStatus.REFUNDING.canTransitionTo(PaymentStatus.FAIL)).isTrue();
        }

        @Test
        void REFUNDING에서_EXPIRED로_전이_가능() {
            assertThat(PaymentStatus.REFUNDING.canTransitionTo(PaymentStatus.EXPIRED)).isTrue();
        }
    }

    @Nested
    @DisplayName("차단된 전이")
    class BlockedTransitions {

        @Test
        void INIT에서_SUCCESS로_직행_불가() {
            assertThat(PaymentStatus.INIT.canTransitionTo(PaymentStatus.SUCCESS)).isFalse();
        }

        @Test
        void INIT에서_FAIL로_직행_불가() {
            assertThat(PaymentStatus.INIT.canTransitionTo(PaymentStatus.FAIL)).isFalse();
        }

        @Test
        void INIT에서_REFUNDING으로_직행_불가() {
            assertThat(PaymentStatus.INIT.canTransitionTo(PaymentStatus.REFUNDING)).isFalse();
        }

        @Test
        void PAYING에서_INIT으로_되돌아가기_불가() {
            assertThat(PaymentStatus.PAYING.canTransitionTo(PaymentStatus.INIT)).isFalse();
        }

        @Test
        void SUCCESS에서_INIT으로_되돌아가기_불가() {
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.INIT)).isFalse();
        }

        @Test
        void SUCCESS에서_PAYING으로_되돌아가기_불가() {
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.PAYING)).isFalse();
        }

        @ParameterizedTest(name = "FAIL에서 {0}으로 전이 불가 — terminal 상태")
        @EnumSource(PaymentStatus.class)
        void FAIL은_모든_상태로_전이_불가(PaymentStatus next) {
            assertThat(PaymentStatus.FAIL.canTransitionTo(next)).isFalse();
        }

        @ParameterizedTest(name = "REFUNDED에서 {0}으로 전이 불가 — terminal 상태")
        @EnumSource(PaymentStatus.class)
        void REFUNDED는_모든_상태로_전이_불가(PaymentStatus next) {
            assertThat(PaymentStatus.REFUNDED.canTransitionTo(next)).isFalse();
        }

        @ParameterizedTest(name = "EXPIRED에서 {0}으로 전이 불가 — terminal 상태")
        @EnumSource(PaymentStatus.class)
        void EXPIRED는_모든_상태로_전이_불가(PaymentStatus next) {
            assertThat(PaymentStatus.EXPIRED.canTransitionTo(next)).isFalse();
        }
    }

    @Nested
    @DisplayName("isTerminal")
    class Terminal {

        @Test
        void FAIL은_terminal() {
            assertThat(PaymentStatus.FAIL.isTerminal()).isTrue();
        }

        @Test
        void REFUNDED는_terminal() {
            assertThat(PaymentStatus.REFUNDED.isTerminal()).isTrue();
        }

        @Test
        void EXPIRED는_terminal() {
            assertThat(PaymentStatus.EXPIRED.isTerminal()).isTrue();
        }

        @Test
        void INIT은_non_terminal() {
            assertThat(PaymentStatus.INIT.isTerminal()).isFalse();
        }

        @Test
        void PAYING은_non_terminal() {
            assertThat(PaymentStatus.PAYING.isTerminal()).isFalse();
        }

        @Test
        void SUCCESS는_non_terminal() {
            assertThat(PaymentStatus.SUCCESS.isTerminal()).isFalse();
        }

        @Test
        void REFUNDING은_non_terminal() {
            assertThat(PaymentStatus.REFUNDING.isTerminal()).isFalse();
        }
    }
}