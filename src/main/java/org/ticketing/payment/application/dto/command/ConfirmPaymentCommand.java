package org.ticketing.payment.application.dto.command;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfirmPaymentCommand {

    private String paymentKey;
    private UUID paymentId;
    private Long totalPrice;
}