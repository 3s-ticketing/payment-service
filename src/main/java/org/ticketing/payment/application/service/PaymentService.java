package org.ticketing.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import org.ticketing.payment.domain.exception.PaymentAmountMismatchException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.exception.PaymentReservationMismatchException;
import org.ticketing.payment.domain.exception.TossPaymentConfirmException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.toss.TossPaymentClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;

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

    @Transactional
    public PaymentResult confirmPayment(UUID paymentId, ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getReservationId().equals(command.getReservationId())) {
            throw new PaymentReservationMismatchException(payment.getReservationId(), command.getReservationId());
        }
        if (!payment.getTotalPrice().equals(command.getTotalPrice())) {
            throw new PaymentAmountMismatchException(payment.getTotalPrice(), command.getTotalPrice());
        }

        payment.start();

        try {
            tossPaymentClient.confirm(
                    command.getPaymentKey(),
                    command.getReservationId().toString(),
                    command.getTotalPrice()
            );
            payment.succeed();
        } catch (TossPaymentConfirmException e) {
            payment.fail();
            throw e;
        }

        return PaymentResult.from(payment);
    }
}