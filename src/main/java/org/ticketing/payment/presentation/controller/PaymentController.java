package org.ticketing.payment.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public PaymentResponseDto createPayment(@RequestBody @Valid CreatePaymentRequestDto request) {
        return PaymentResponseDto.from(paymentService.createPayment(request.toCommand()));
    }

    // paymentId에 따른 결제 정보 조회 [Customer]
    @GetMapping("/my/{paymentId}")
    public PaymentResponseDto getMyPayment(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.getPayment(paymentId));
    }

    // reservationId에 따른 결제 정보 조회 [Customer]
    @GetMapping("/my/reservations/{reservationId}")
    public Page<PaymentResponseDto> getMyPaymentsByReservationId(
            @PathVariable @NotNull UUID reservationId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getPaymentsByReservationId(reservationId, pageable).map(PaymentResponseDto::from);
    }

    // reservationId에 따른 성공 결제 정보 조회 [Customer]
    @GetMapping("/my/reservations/{reservationId}/success")
    public PaymentResponseDto getMySuccessPaymentByReservationId(@PathVariable @NotNull UUID reservationId) {
        return PaymentResponseDto.from(paymentService.getSuccessPaymentByReservationId(reservationId));
    }

    // paymentId에 따른 결제 정보 조회 [Admin]
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPayment(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.getPayment(paymentId));
    }

    // reservationId에 따른 결제 정보 조회 [Amdin]
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

    @PatchMapping("/{paymentId}/cancel")
    public PaymentResponseDto updatePaymentStatus(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.cancelPayment(paymentId));
    }

    // 결제 승인 [PG]
    @PostMapping("/success")
    public PaymentResponseDto confirmPayment(
            @RequestBody @Valid PaymentSuccessRequestDto request) {
        return PaymentResponseDto.from(paymentService.confirmPayment(request.toCommand()));
    }
}