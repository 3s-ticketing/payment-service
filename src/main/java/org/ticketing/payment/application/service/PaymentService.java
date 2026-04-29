package org.ticketing.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.ticketing.payment.domain.exception.DuplicatePaymentException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.infrastructure.toss.TossPaymentClient;
import org.ticketing.payment.infrastructure.toss.dto.TossConfirmResponse;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentStatusService paymentStatusService;

    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {
        if (paymentRepository.existsActivePayment(command.getReservationId())) {
            throw new DuplicatePaymentException(command.getReservationId());
        }
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

    @Transactional(readOnly = true)
    public Page<PaymentResult> getPaymentsByReservationId(UUID reservationId, Pageable pageable) {
        return paymentRepository.findByReservationId(reservationId, pageable).map(PaymentResult::from);
    }

    @Transactional(readOnly = true)
    public PaymentResult getSuccessPaymentByReservationId(UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResult> getPaymentsByUserId(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable).map(PaymentResult::from);
    }

    public PaymentResult cancelPayment(UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));

        tossPaymentClient.cancel(payment.getPaymentKey(), "고객 요청 취소");
        return paymentStatusService.cancelPayment(payment.getId());
    }

    public PaymentResult confirmPayment(ConfirmPaymentCommand command) {
        paymentStatusService.startPayment(command.getPaymentId(), command.getTotalPrice());

        TossConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirm(
                    command.getPaymentKey(),
                    command.getPaymentId().toString(),
                    command.getTotalPrice()
            );
        } catch (RuntimeException e) {
            paymentStatusService.failPayment(command.getPaymentId());
            throw e;
        } catch (Exception e) {
            paymentStatusService.failPayment(command.getPaymentId());
            throw new RuntimeException(e);
        }

        return paymentStatusService.succeedPayment(command.getPaymentId(), tossResponse.getPaymentKey());
    }
}