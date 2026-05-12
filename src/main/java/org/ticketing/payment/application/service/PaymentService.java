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
import org.ticketing.payment.domain.exception.InvalidReservationStateException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.domain.exception.PaymentUserMismatchException;
import org.ticketing.payment.domain.exception.TossPaymentCancelException;
import org.ticketing.payment.domain.exception.TossPaymentConfirmException;
import org.ticketing.payment.domain.client.ReservationClient;
import org.ticketing.payment.infrastructure.kafka.event.ReservationCanceledEvent.CancelReason;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.domain.provider.TossPaymentProvider;
import org.ticketing.payment.domain.provider.TossPaymentProvider.ConfirmResult;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentProvider tossPaymentProvider;
    private final PaymentStatusService paymentStatusService;
    private final ReservationClient reservationClient;

    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {

        ReservationClient.ReservationDetail reservation =
                reservationClient.getReservationDetail(command.getReservationId());

        if (!reservation.isValid()) {
            throw new InvalidReservationStateException(command.getReservationId());
        }
        if (!reservation.userId().equals(command.getUserId())) {
            throw new PaymentUserMismatchException(reservation.userId(), command.getUserId());
        }

        if (paymentRepository.existsActivePayment(command.getReservationId())) {
            throw new DuplicatePaymentException(command.getReservationId());
        }

        Payment payment = Payment.create(
                command.getUserId(),
                command.getReservationId(),
                reservation.totalPrice()
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
    public PaymentResult getMyPayment(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
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
    public Page<PaymentResult> getMyPaymentsByReservationId(UUID userId, UUID reservationId, Pageable pageable) {
        return paymentRepository.findByReservationIdAndUserId(reservationId, userId, pageable).map(PaymentResult::from);
    }

    @Transactional(readOnly = true)
    public PaymentResult getSuccessPaymentByReservationId(UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResult getMySuccessPaymentByReservationId(UUID userId, UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new PaymentNotFoundException(reservationId));
        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResult> getPaymentsByUserId(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable).map(PaymentResult::from);
    }

    public PaymentResult refundPayment(UUID reservationId) {
        Payment payment = paymentStatusService.startRefund(reservationId);
        try {
            tossPaymentProvider.cancel(payment.getPaymentKey(), "고객 요청 취소");
        } catch (TossPaymentCancelException e) {
            paymentStatusService.revertRefund(payment.getId());
            throw e;
        }
        return paymentStatusService.refundPayment(payment.getId());
    }

    public void handleReservationCanceled(UUID reservationId, CancelReason cancelReason) {
        if (paymentRepository.findSuccessPaymentByReservationId(reservationId).isEmpty()) {
            // Success된 결제가 없어서, 환불 이벤트 씹음
            return;
        }
        refundPayment(reservationId);
    }

    public PaymentResult confirmPayment(ConfirmPaymentCommand command) {
        // 이미 SUCCESS인 결제는 재처리 없이 그대로 반환
        Payment existing = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.getPaymentId()));
        if (existing.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResult.from(existing);
        }

        paymentStatusService.startPayment(command.getPaymentId(), command.getTotalPrice());

        ConfirmResult tossResponse;
        try {
            tossResponse = tossPaymentProvider.confirm(
                    command.getPaymentKey(),
                    command.getPaymentId().toString(),
                    command.getTotalPrice()
            );
        } catch (TossPaymentConfirmException e) {
            paymentStatusService.failPayment(command.getPaymentId());
            throw e;
        }

        return paymentStatusService.succeedPayment(command.getPaymentId(), tossResponse.paymentKey());
    }
}