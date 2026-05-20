package org.ticketing.payment.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.presentation.dto.request.CreatePaymentRequestDto;
import org.ticketing.payment.presentation.dto.request.PaymentSuccessRequestDto;
import org.ticketing.payment.presentation.dto.response.PaymentResponseDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 시도 생성 [Customer]
    // (사용자가 결제 요청 버튼 누를 때)
    @PostMapping
    public PaymentResponseDto createPayment(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody @Valid CreatePaymentRequestDto request) {
        return PaymentResponseDto.from(paymentService.createPayment(request.toCommand(userId)));
    }

    // paymentId에 따른 결제 정보 조회 [Customer]
    @GetMapping("/my/{paymentId}")
    public PaymentResponseDto getMyPayment(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.getMyPayment(userId, paymentId));
    }

    // reservationId에 따른 결제 정보 조회 [Customer]
    @GetMapping("/my/reservations/{reservationId}")
    public Page<PaymentResponseDto> getMyPaymentsByReservationId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable @NotNull UUID reservationId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getMyPaymentsByReservationId(userId, reservationId, pageable).map(PaymentResponseDto::from);
    }

    // reservationId에 따른 성공 결제 정보 조회 [Customer]
    @GetMapping("/my/reservations/{reservationId}/success")
    public PaymentResponseDto getMySuccessPaymentByReservationId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable @NotNull UUID reservationId) {
        return PaymentResponseDto.from(paymentService.getMySuccessPaymentByReservationId(userId, reservationId));
    }

    // paymentId에 따른 결제 정보 조회 [Admin]
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPayment(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.getPayment(paymentId));
    }

    // reservationId에 따른 결제 정보 조회 [Admin]
    @GetMapping("/reservations/{reservationId}")
    public Page<PaymentResponseDto> getPaymentsByReservationId(
            @PathVariable @NotNull UUID reservationId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getPaymentsByReservationId(reservationId, pageable).map(PaymentResponseDto::from);
    }

    // 전체 결제 정보 조회 [Admin]
    @GetMapping
    public Page<PaymentResponseDto> getPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getPayments(pageable).map(PaymentResponseDto::from);
    }

    // 결제 환불 [Customer]
    @PostMapping("/refund/reservations/{reservationId}")
    public PaymentResponseDto refundPayment(@PathVariable @NotNull UUID reservationId) {
        return PaymentResponseDto.from(paymentService.refundPayment(reservationId));
    }

    // 결제 승인 [PG]
    @PostMapping("/success")
    public PaymentResponseDto confirmPayment(
            @RequestBody @Valid PaymentSuccessRequestDto request) {
        return PaymentResponseDto.from(paymentService.confirmPayment(request.toCommand()));
    }
}