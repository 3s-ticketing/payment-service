package org.ticketing.payment.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.ticketing.common.response.CommonResponse;
import org.ticketing.payment.application.dto.command.CreatePaymentCommand;
import org.ticketing.payment.application.service.PaymentService;
import org.ticketing.payment.presentation.dto.request.CreatePaymentRequestDto;
import org.ticketing.payment.presentation.dto.response.PaymentResponseDto;

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
}