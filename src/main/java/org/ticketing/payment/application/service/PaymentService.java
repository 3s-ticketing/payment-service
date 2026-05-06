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
import org.ticketing.common.exception.ForbiddenException;
import org.ticketing.payment.domain.exception.DuplicatePaymentException;
import org.ticketing.payment.domain.exception.PaymentNotFoundException;
import org.ticketing.payment.infrastructure.kafka.event.ReservationCanceledEvent.CancelReason;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.repository.PaymentRepository;
import org.ticketing.payment.domain.provider.ReservationProvider;
import org.ticketing.payment.domain.provider.ReservationProvider.ReservationSnapshot;
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
    private final ReservationProvider reservationProvider;

    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {

        ReservationSnapshot reservation = reservationProvider.getReservation(command.getReservationId());
        if (!reservation.userId().equals(command.getUserId())) {
            throw new IllegalArgumentException("예약의 userId 불일치: 예약 userId " + reservation.userId() + ", 요청 userId " + command.getUserId());
        }
        // Todo: [Feign] Reservation 해당 예약 상태가 PENDING인지 & 좌석 상태가 HOLD인지 확인

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
        tossPaymentProvider.cancel(payment.getPaymentKey(), "고객 요청 취소");
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
        Payment payment = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.getPaymentId()));
        if (!payment.getUserId().equals(command.getUserId())) {
            throw new ForbiddenException("본인의 결제만 승인할 수 있습니다.");
        }

        paymentStatusService.startPayment(command.getPaymentId(), command.getTotalPrice());

        ConfirmResult tossResponse;
        try {
            tossResponse = tossPaymentProvider.confirm(
                    command.getPaymentKey(),
                    command.getPaymentId().toString(),
                    command.getTotalPrice()
            );
        } catch (Exception e) {
            paymentStatusService.failPayment(command.getPaymentId());
            throw new RuntimeException(e);
        }

        return paymentStatusService.succeedPayment(command.getPaymentId(), tossResponse.paymentKey());
    }
}