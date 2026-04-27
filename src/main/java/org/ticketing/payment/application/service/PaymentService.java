package org.ticketing.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.toss.TossPaymentClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentStatusService paymentStatusService;

    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {
        Payment payment = Payment.create(
                command.getUserId(),
                command.getReservationId(),
                command.getTotalPrice()
        );
        return PaymentResult.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResult getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResult> getPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(PaymentResult::from);
    }

    @Transactional
    public PaymentResult cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.cancel();
        return PaymentResult.from(payment);
    }

    public PaymentResult confirmPayment(ConfirmPaymentCommand command) {
        paymentStatusService.startPayment(command.getReservationId(), command.getTotalPrice());

        try {
            tossPaymentClient.confirm(
                    command.getPaymentKey(),
                    command.getReservationId().toString(),
                    command.getTotalPrice()
            );
        } catch (RuntimeException e) {
            paymentStatusService.failPayment(command.getReservationId());
            throw e;
        } catch (Exception e) {
            paymentStatusService.failPayment(command.getReservationId());
            throw new RuntimeException(e);
        }

        return paymentStatusService.succeedPayment(command.getReservationId());
    }
}