package org.ticketing.payment.presentation.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.presentation.dto.response.PaymentResponseDto;

import java.util.UUID;

@PreAuthorize("hasRole('INTERNAL')")
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentService paymentService;

    // userId에 따른 결제 정보 조회
    @GetMapping("/users/{userId}")
    public Page<PaymentResponseDto> getPaymentsByUserId(
            @PathVariable @NotNull UUID userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getPaymentsByUserId(userId, pageable).map(PaymentResponseDto::from);
    }

    // paymentId에 따른 결제 정보 조회
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPayment(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.getPayment(paymentId));
    }

    // reservationId에 따른 결제 정보 조회 (성공, 실패 등등 모든 시도 포함)
    @GetMapping("/reservations/{reservationId}")
    public Page<PaymentResponseDto> getPaymentsByReservationId(
            @PathVariable @NotNull UUID reservationId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getPaymentsByReservationId(reservationId, pageable).map(PaymentResponseDto::from);
    }

    // reservationId에 따른 성공 결제 정보 조회
    @GetMapping("/reservations/{reservationId}/success")
    public PaymentResponseDto getSuccessPaymentByReservationId(@PathVariable @NotNull UUID reservationId) {
        return PaymentResponseDto.from(paymentService.getSuccessPaymentByReservationId(reservationId));
    }
}