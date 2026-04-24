package org.ticketing.payment.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.ticketing.common.response.CommonResponse;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.dto.command.UpdatePaymentStatusCommand;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.presentation.dto.request.CreatePaymentRequestDto;
import org.ticketing.payment.presentation.dto.request.UpdatePaymentStatusRequestDto;
import org.ticketing.payment.presentation.dto.response.PaymentResponseDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<PaymentResponseDto> createPayment(@RequestBody CreatePaymentRequestDto request) {
        PaymentResponseDto response = PaymentResponseDto.from(
                paymentService.createPayment(CreatePaymentCommand.from(request))
        );
        return CommonResponse.success(response);
    }

    @GetMapping("/{paymentId}")
    public CommonResponse<PaymentResponseDto> getPayment(@PathVariable UUID paymentId) {
        return CommonResponse.success(PaymentResponseDto.from(paymentService.getPayment(paymentId)));
    }

    @GetMapping
    public CommonResponse<Page<PaymentResponseDto>> getPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<PaymentResponseDto> response = paymentService.getPayments(pageable).map(PaymentResponseDto::from);
        return CommonResponse.success(response);
    }

    @PatchMapping("/{paymentId}/status")
    public CommonResponse<PaymentResponseDto> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @RequestBody UpdatePaymentStatusRequestDto request) {
        return CommonResponse.success(PaymentResponseDto.from(
                paymentService.updatePaymentStatus(paymentId, UpdatePaymentStatusCommand.from(request))
        ));
    }
}