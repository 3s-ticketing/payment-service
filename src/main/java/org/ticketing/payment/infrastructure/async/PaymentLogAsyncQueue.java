package org.ticketing.payment.infrastructure.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;
import org.ticketing.payment.domain.model.PaymentLog;

@Component
public class PaymentLogAsyncQueue {

    private final ConcurrentLinkedQueue<PaymentLog> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(PaymentLog log) {
        queue.add(log);
    }

    public List<PaymentLog> drainAll() {
        List<PaymentLog> drained = new ArrayList<>();
        PaymentLog item;
        while ((item = queue.poll()) != null) {
            drained.add(item);
        }
        return drained;
    }
}
