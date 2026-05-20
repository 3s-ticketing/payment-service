package org.ticketing.payment.infrastructure.async;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.domain.model.PaymentLog;
import org.ticketing.payment.domain.repository.PaymentLogRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentLogBatchFlusher {

    private final PaymentLogAsyncQueue queue;
    private final PaymentLogRepository paymentLogRepository;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void flush() {
        List<PaymentLog> logs = queue.drainAll();
        if (logs.isEmpty()) return;
        paymentLogRepository.saveAll(logs);
        log.debug("[PaymentLogBatchFlusher] flushed {} logs", logs.size());
    }
}
