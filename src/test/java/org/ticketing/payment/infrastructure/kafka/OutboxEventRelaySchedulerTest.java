package org.ticketing.payment.infrastructure.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketing.payment.domain.event.PaymentEventPublisher;
import org.ticketing.payment.domain.outbox.OutboxStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventRelayScheduler")
class OutboxEventRelaySchedulerTest {

    @Mock PaymentOutboxRepository outboxRepository;
    @Mock PaymentEventPublisher paymentEventPublisher;
    @InjectMocks OutboxEventRelayScheduler scheduler;

    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("relay")
    class Relay {

        @Test
        @DisplayName("빈 아웃박스 → publish 호출 없음")
        void 빈_아웃박스_publish_없음() throws Exception {
            given(outboxRepository.fetchAndMarkProcessing(anyInt())).willReturn(List.of());

            scheduler.relay();

            verify(paymentEventPublisher, never()).publishPaymentCompleted(any(), any(), any());
            verify(paymentEventPublisher, never()).publishPaymentFailed(any(), any(), any());
            verify(paymentEventPublisher, never()).publishPaymentRefunded(any(), any(), any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("payment.completed 토픽 → publish 성공 시 markPublished 저장")
        void completed_토픽_성공_markPublished() throws Exception {
            PaymentOutbox outbox = spyOutbox("payment.completed");
            given(outboxRepository.fetchAndMarkProcessing(anyInt())).willReturn(List.of(outbox));
            given(paymentEventPublisher.publishPaymentCompleted(any(), any(), any()))
                    .willReturn(CompletableFuture.completedFuture(null));
            given(outboxRepository.save(any())).willReturn(outbox);

            scheduler.relay();

            // CompletableFuture whenComplete가 비동기로 실행되므로 잠시 대기
            Thread.sleep(100);

            verify(outbox).markPublished();
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("payment.failed 토픽 → publish 성공 시 markPublished 저장")
        void failed_토픽_성공_markPublished() throws Exception {
            PaymentOutbox outbox = spyOutbox("payment.failed");
            given(outboxRepository.fetchAndMarkProcessing(anyInt())).willReturn(List.of(outbox));
            given(paymentEventPublisher.publishPaymentFailed(any(), any(), any()))
                    .willReturn(CompletableFuture.completedFuture(null));
            given(outboxRepository.save(any())).willReturn(outbox);

            scheduler.relay();
            Thread.sleep(100);

            verify(outbox).markPublished();
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("payment.refunded 토픽 → publish 성공 시 markPublished 저장")
        void refunded_토픽_성공_markPublished() throws Exception {
            PaymentOutbox outbox = spyOutbox("payment.refunded");
            given(outboxRepository.fetchAndMarkProcessing(anyInt())).willReturn(List.of(outbox));
            given(paymentEventPublisher.publishPaymentRefunded(any(), any(), any()))
                    .willReturn(CompletableFuture.completedFuture(null));
            given(outboxRepository.save(any())).willReturn(outbox);

            scheduler.relay();
            Thread.sleep(100);

            verify(outbox).markPublished();
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("publish 실패 → markFailed 저장")
        void publish_실패_markFailed() throws Exception {
            PaymentOutbox outbox = spyOutbox("payment.completed");
            given(outboxRepository.fetchAndMarkProcessing(anyInt())).willReturn(List.of(outbox));
            given(paymentEventPublisher.publishPaymentCompleted(any(), any(), any()))
                    .willReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka 오류")));
            given(outboxRepository.save(any())).willReturn(outbox);

            scheduler.relay();
            Thread.sleep(100);

            verify(outbox).markFailed();
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("알 수 없는 토픽 → markFailed 저장")
        void 알_수_없는_토픽_markFailed() throws Exception {
            PaymentOutbox outbox = spyOutbox("payment.unknown");
            given(outboxRepository.fetchAndMarkProcessing(anyInt())).willReturn(List.of(outbox));
            given(outboxRepository.save(any())).willReturn(outbox);

            scheduler.relay();
            Thread.sleep(100);

            verify(outbox).markFailed();
            verify(outboxRepository).save(outbox);
        }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /**
     * PaymentOutbox를 spy로 감싸 markPublished / markFailed 호출 여부를 검증한다.
     * 실제 PaymentOutbox는 @Builder + static factory를 쓰므로 reflection으로 필드를 직접 주입한다.
     */
    private PaymentOutbox spyOutbox(String topic) throws Exception {
        PaymentOutbox raw = switch (topic) {
            case "payment.completed" -> PaymentOutbox.createCompleted(PAYMENT_ID, ORDER_ID);
            case "payment.failed"    -> PaymentOutbox.createFailed(PAYMENT_ID, ORDER_ID);
            case "payment.refunded"  -> PaymentOutbox.createRefund(PAYMENT_ID, ORDER_ID);
            default -> {
                // 알 수 없는 토픽: createCompleted 로 만든 뒤 topic 필드만 교체
                PaymentOutbox p = PaymentOutbox.createCompleted(PAYMENT_ID, ORDER_ID);
                var field = PaymentOutbox.class.getDeclaredField("topic");
                field.setAccessible(true);
                field.set(p, topic);
                yield p;
            }
        };

        // id 필드를 직접 주입 (UUID 식별자가 null이면 publish 인자가 null로 넘어감)
        var idField = PaymentOutbox.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(raw, MESSAGE_ID);

        return spy(raw);
    }
}
