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
import org.ticketing.payment.presentation.dto.response.PaymentResponseDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponseDto createPayment(@RequestBody @Valid CreatePaymentRequestDto request) {
        return PaymentResponseDto.from(paymentService.createPayment(request.toCommand()));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPayment(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.getPayment(paymentId));
    }

    @GetMapping
    public Page<PaymentResponseDto> getPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getPayments(pageable).map(PaymentResponseDto::from);
    }

    @PatchMapping("/{paymentId}/cancel")
    public PaymentResponseDto updatePaymentStatus(@PathVariable @NotNull UUID paymentId) {
        return PaymentResponseDto.from(paymentService.cancelPayment(paymentId));
    }
}