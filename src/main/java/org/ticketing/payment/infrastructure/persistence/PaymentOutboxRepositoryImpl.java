package org.ticketing.payment.infrastructure.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.domain.outbox.OutboxStatus;
import org.ticketing.payment.domain.outbox.PaymentOutbox;
import org.ticketing.payment.domain.outbox.PaymentOutboxRepository;
import org.ticketing.payment.domain.outbox.QPaymentOutbox;

@Repository
@RequiredArgsConstructor
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepository {

    private final JpaPaymentOutboxRepository jpa;
    private final JPAQueryFactory queryFactory;

    @Override
    public PaymentOutbox save(PaymentOutbox outbox) {
        return jpa.save(outbox);
    }

    @Override
    @Transactional
    public List<PaymentOutbox> fetchAndMarkProcessing(int size) {
        QPaymentOutbox outbox = QPaymentOutbox.paymentOutbox;

        List<PaymentOutbox> pending = queryFactory
                .selectFrom(outbox)
                .where(outbox.status.eq(OutboxStatus.PENDING))
                .limit(size)
                .fetch();

        if (pending.isEmpty()) {
            return pending;
        }

        List<UUID> ids = pending.stream().map(PaymentOutbox::getId).toList();
        queryFactory
                .update(outbox)
                .set(outbox.status, OutboxStatus.PROCESSING)
                .where(outbox.id.in(ids).and(outbox.status.eq(OutboxStatus.PENDING)))
                .execute();

        return pending;
    }
}