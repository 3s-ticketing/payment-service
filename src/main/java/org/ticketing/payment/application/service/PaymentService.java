package org.ticketing.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ticketing.payment.application.dto.command.ConfirmPaymentCommand;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.result.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.ticketing.payment.domain.exception.PaymentErrorCode;
import org.ticketing.payment.domain.exception.PaymentException;
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

    public PaymentResult createPayment(CreatePaymentCommand command) {
        ReservationClient.ReservationDetail reservation =
                reservationClient.getReservationDetail(command.getReservationId());

        if (!reservation.isValid()) {
            throw new PaymentException(PaymentErrorCode.INVALID_RESERVATION_STATE,
                    "reservationId=" + command.getReservationId());
        }
        if (!reservation.userId().equals(command.getUserId())) {
            throw new PaymentException(PaymentErrorCode.USER_MISMATCH,
                    "예약의 userId=" + reservation.userId() + ", 요청 userId=" + command.getUserId());
        }

        try {
            Payment payment = Payment.create(
                    command.getUserId(),
                    command.getReservationId(),
                    reservation.totalPrice()
            );
            return PaymentResult.from(paymentRepository.save(payment));
        } catch (DataIntegrityViolationException e) {
            throw new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT,
                    "reservationId=" + command.getReservationId());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResult getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, paymentId.toString()));
        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResult getMyPayment(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, paymentId.toString()));
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
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, "reservationId=" + reservationId));
        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResult getMySuccessPaymentByReservationId(UUID userId, UUID reservationId) {
        Payment payment = paymentRepository.findSuccessPaymentByReservationIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, "reservationId=" + reservationId));
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
        } catch (RuntimeException e) {
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
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, command.getPaymentId().toString()));
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
        } catch (RuntimeException e) {
            paymentStatusService.failPayment(command.getPaymentId());
            throw e;
        }

        return paymentStatusService.succeedPayment(command.getPaymentId(), tossResponse.paymentKey());
    }
}